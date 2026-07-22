package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Main extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 480;
    private static final float WORLD_HEIGHT = 800;

    private enum GameState { MENU, PLAYING, GAME_OVER }
    private GameState state;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;

    // Камера чтобы не смещать коорды кнопок
    private OrthographicCamera uiCamera;
    private Viewport uiViewport;

    // Текстуры и шрифты
    private Texture playerTex;
    private Texture platformTex;
    private Texture btnLeftTex;
    private Texture btnRightTex;
    private Texture btnPlayTex;
    private Texture btnSoundTex;
    private BitmapFont font;
    private GlyphLayout glyphLayout;

    // Звуки и музыка
    private Music bgMusic;
    private Sound jumpSound;
    private Sound loseSound;
    private boolean isSoundOn = true;

    // Сохранения
    private Preferences prefs;
    private int score;
    private int highScore;
    private float maxAltitude;

    // Игрок и физика
    private Rectangle player;
    private Vector2 velocity;
    private static final float GRAVITY = -1200f;
    private static final float JUMP_VELOCITY = 800f;
    private static final float MOVE_SPEED = 300f;

    // Платформы
    private Array<Rectangle> platforms;
    private static final int PLATFORM_COUNT = 10;
    private float highestPlatformY;

    // Кнопки
    private Rectangle btnLeft;
    private Rectangle btnRight;
    private Rectangle btnPlay;
    private Rectangle btnSound;

    @Override
    public void create() {
        batch = new SpriteBatch();

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);

        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, uiCamera);
        uiCamera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);

        playerTex = new Texture("player.png");
        platformTex = new Texture("platform.png");
        btnLeftTex = new Texture("btn_left.png");
        btnRightTex = new Texture("btn_right.png");
        btnPlayTex = new Texture("btn_play.png");
        btnSoundTex = new Texture("btn_sound.png");

        font = new BitmapFont();
        font.getData().setScale(2f);
        glyphLayout = new GlyphLayout();

        bgMusic = Gdx.audio.newMusic(Gdx.files.internal("bgm.mp3"));
        bgMusic.setLooping(true);
        jumpSound = Gdx.audio.newSound(Gdx.files.internal("jump.wav"));
        loseSound = Gdx.audio.newSound(Gdx.files.internal("gameover.wav"));

        prefs = Gdx.app.getPreferences("JumpGamePrefs");
        highScore = prefs.getInteger("highScore", 0);

        platforms = new Array<>();
        velocity = new Vector2();

        btnLeft = new Rectangle(20, 20, 100, 100);
        btnRight = new Rectangle(WORLD_WIDTH - 120, 20, 100, 100);
        btnPlay = new Rectangle(WORLD_WIDTH / 2 - 75, WORLD_HEIGHT / 2 - 50, 150, 100);
        btnSound = new Rectangle(WORLD_WIDTH - 80, WORLD_HEIGHT - 80, 60, 60);

        state = GameState.MENU;
        if (isSoundOn) bgMusic.play();
    }

    private void resetGame() {
        player = new Rectangle(WORLD_WIDTH / 2 - 32, WORLD_HEIGHT / 2, 64, 64);
        velocity.set(0, 0);
        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);

        platforms.clear();
        highestPlatformY = 0;
        score = 0;
        maxAltitude = player.y;

        spawnPlatform(WORLD_HEIGHT / 2 - 50);
        for (int i = 1; i < PLATFORM_COUNT; i++) {
            spawnPlatform(highestPlatformY + MathUtils.random(80, 150));
        }
    }

    private void spawnPlatform(float y) {
        Rectangle platform = new Rectangle(MathUtils.random(0, WORLD_WIDTH - 100), y, 100, 20);
        platforms.add(platform);
        highestPlatformY = y;
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        update(dt);

        Gdx.gl.glClearColor(0.8f, 0.9f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Отрисовка игры (action камера)
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (state == GameState.PLAYING) {
            for (Rectangle p : platforms) {
                batch.draw(platformTex, p.x, p.y, p.width, p.height);
            }
            batch.draw(playerTex, player.x, player.y, player.width, player.height);
        }
        batch.end();

        // Отрисовка UI (staitc cam)
        uiCamera.update();
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        batch.draw(btnSoundTex, btnSound.x, btnSound.y, btnSound.width, btnSound.height);
        if (!isSoundOn) {
            // Звук выключен
            font.draw(batch, "X", btnSound.x + 15, btnSound.y + 45);
        }

        if (state == GameState.MENU) {
            drawCenteredText("JUMP GAME", WORLD_HEIGHT - 200);
            drawCenteredText("High Score: " + highScore, WORLD_HEIGHT - 300);
            batch.draw(btnPlayTex, btnPlay.x, btnPlay.y, btnPlay.width, btnPlay.height);
        }
        else if (state == GameState.PLAYING) {
            font.draw(batch, "Score: " + score, 20, WORLD_HEIGHT - 20);
            batch.draw(btnLeftTex, btnLeft.x, btnLeft.y, btnLeft.width, btnLeft.height);
            batch.draw(btnRightTex, btnRight.x, btnRight.y, btnRight.width, btnRight.height);
        }
        else if (state == GameState.GAME_OVER) {
            drawCenteredText("GAME OVER", WORLD_HEIGHT - 200);
            drawCenteredText("Score: " + score, WORLD_HEIGHT - 300);
            batch.draw(btnPlayTex, btnPlay.x, btnPlay.y, btnPlay.width, btnPlay.height);
        }
        batch.end();
    }

    private void update(float dt) {
        handleInput();

        if (state != GameState.PLAYING) return;

        // Физика
        velocity.y += GRAVITY * dt;
        player.x += velocity.x * dt;
        player.y += velocity.y * dt;

        // Screen wrap
        if (player.x > WORLD_WIDTH) player.x = -player.width;
        if (player.x + player.width < 0) player.x = WORLD_WIDTH;

        // Очки от max высоты
        if (player.y > maxAltitude) {
            maxAltitude = player.y;
            score = (int) (maxAltitude / 100);
        }

        // коллизия
        if (velocity.y < 0) {
            for (Rectangle p : platforms) {
                if (player.overlaps(p) && player.y > p.y + p.height / 2) {
                    velocity.y = JUMP_VELOCITY;
                    if (isSoundOn) jumpSound.play();
                    break;
                }
            }
        }

        // Движение камеры
        if (player.y > camera.position.y) {
            camera.position.y = player.y;
        }

        // Генерация платформ
        for (int i = platforms.size - 1; i >= 0; i--) {
            Rectangle p = platforms.get(i);
            if (p.y < camera.position.y - WORLD_HEIGHT / 2 - p.height) {
                platforms.removeIndex(i);
                spawnPlatform(highestPlatformY + MathUtils.random(80, 150));
            }
        }

        // Условие проигрыша
        if (player.y < camera.position.y - WORLD_HEIGHT / 2) {
            saveScore();
            if (isSoundOn) loseSound.play();
            state = GameState.GAME_OVER;
        }
    }

    private void handleInput() {
        velocity.x = 0;
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            uiViewport.unproject(touchPos);

            // Кнопка звука
            if (btnSound.contains(touchPos.x, touchPos.y)) {
                isSoundOn = !isSoundOn;
                if (isSoundOn) bgMusic.play();
                else bgMusic.pause();
                return;
            }

            // Кнопка Play (Меню и Game Over)
            if (state == GameState.MENU || state == GameState.GAME_OVER) {
                if (btnPlay.contains(touchPos.x, touchPos.y)) {
                    resetGame();
                    state = GameState.PLAYING;
                }
            }
        }

        // Управление в игре (холдить)
        if (state == GameState.PLAYING && Gdx.input.isTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            uiViewport.unproject(touchPos);

            if (btnLeft.contains(touchPos.x, touchPos.y)) velocity.x = -MOVE_SPEED;
            else if (btnRight.contains(touchPos.x, touchPos.y)) velocity.x = MOVE_SPEED;
        }
    }

    private void saveScore() {
        if (score > highScore) {
            highScore = score;
            prefs.putInteger("highScore", highScore);
            prefs.flush();
        }
    }

    private void drawCenteredText(String text, float y) {
        glyphLayout.setText(font, text);
        font.draw(batch, text, (WORLD_WIDTH - glyphLayout.width) / 2, y);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        uiViewport.update(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        playerTex.dispose();
        platformTex.dispose();
        btnLeftTex.dispose();
        btnRightTex.dispose();
        btnPlayTex.dispose();
        btnSoundTex.dispose();
        font.dispose();
        bgMusic.dispose();
        jumpSound.dispose();
        loseSound.dispose();
    }
}

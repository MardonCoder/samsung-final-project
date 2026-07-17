package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 480;
    private static final float WORLD_HEIGHT = 800;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;

    // Текстуры
    private Texture playerTex;
    private Texture platformTex;
    private Texture btnLeftTex;
    private Texture btnRightTex;

    // Игрок
    private Rectangle player;
    private Vector2 velocity;
    private static final float GRAVITY = -1200f;
    private static final float JUMP_VELOCITY = 800f;
    private static final float MOVE_SPEED = 300f;

    // Платформы
    private Array<Rectangle> platforms;
    private static final int PLATFORM_COUNT = 10;
    private float highestPlatformY;

    // Кнопки управления
    private Rectangle btnLeft;
    private Rectangle btnRight;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);

        playerTex = new Texture("player.png");
        platformTex = new Texture("platform.png");
        btnLeftTex = new Texture("btn_left.png");
        btnRightTex = new Texture("btn_right.png");

        platforms = new Array<>();

        btnLeft = new Rectangle(20, 20, 100, 100);
        btnRight = new Rectangle(WORLD_WIDTH - 120, 20, 100, 100);

        // КРИТИЧЕСКИ ВАЖНО: Инициализация вектора скорости
        velocity = new Vector2();

        resetGame();
    }

    private void resetGame() {
        // Сброс физики и координат игрока
        player = new Rectangle(WORLD_WIDTH / 2 - 32, WORLD_HEIGHT / 2, 64, 64);
        velocity.set(0, 0); // Обнуление вектора скорости без создания нового объекта

        // Сброс камеры
        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);

        // Сброс и перегенерация платформ
        platforms.clear();
        highestPlatformY = 0;

        // Обязательная платформа под ногами при спавне
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

        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        for (Rectangle p : platforms) {
            batch.draw(platformTex, p.x, p.y, p.width, p.height);
        }
        batch.draw(playerTex, player.x, player.y, player.width, player.height);

        // Отрисовка кнопок (относительно позиции камеры)
        batch.draw(btnLeftTex, camera.position.x - WORLD_WIDTH/2 + btnLeft.x, camera.position.y - WORLD_HEIGHT/2 + btnLeft.y, btnLeft.width, btnLeft.height);
        batch.draw(btnRightTex, camera.position.x - WORLD_WIDTH/2 + btnRight.x, camera.position.y - WORLD_HEIGHT/2 + btnRight.y, btnRight.width, btnRight.height);
        batch.end();
    }

    private void update(float dt) {
        // Логика управления
        velocity.x = 0;
        if (Gdx.input.isTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPos);

            // Проверка попадания по кнопкам (с учетом смещения камеры)
            float camX = camera.position.x - WORLD_WIDTH/2;
            float camY = camera.position.y - WORLD_HEIGHT/2;

            if (touchPos.x >= camX + btnLeft.x && touchPos.x <= camX + btnLeft.x + btnLeft.width &&
                touchPos.y >= camY + btnLeft.y && touchPos.y <= camY + btnLeft.y + btnLeft.height) {
                velocity.x = -MOVE_SPEED;
            } else if (touchPos.x >= camX + btnRight.x && touchPos.x <= camX + btnRight.x + btnRight.width &&
                touchPos.y >= camY + btnRight.y && touchPos.y <= camY + btnRight.y + btnRight.height) {
                velocity.x = MOVE_SPEED;
            }
        }

        // Физика
        velocity.y += GRAVITY * dt;
        player.x += velocity.x * dt;
        player.y += velocity.y * dt;

        // Переход за края экрана (Screen wrap)
        if (player.x > WORLD_WIDTH) player.x = -player.width;
        if (player.x + player.width < 0) player.x = WORLD_WIDTH;

        // Столкновения с платформами (только при падении)
        if (velocity.y < 0) {
            for (Rectangle p : platforms) {
                if (player.overlaps(p) && player.y > p.y + p.height / 2) {
                    velocity.y = JUMP_VELOCITY;
                    break; // Прыжок только от одной платформы
                }
            }
        }

        // Движение камеры (только вверх)
        if (player.y > camera.position.y) {
            camera.position.y = player.y;
        }

        // Генерация новых платформ и удаление старых
        for (int i = platforms.size - 1; i >= 0; i--) {
            Rectangle p = platforms.get(i);
            if (p.y < camera.position.y - WORLD_HEIGHT / 2 - p.height) {
                platforms.removeIndex(i);
                spawnPlatform(highestPlatformY + MathUtils.random(80, 150));
            }
        }

        // Условие проигрыша
        if (player.y < camera.position.y - WORLD_HEIGHT / 2) {
            // Перезапуск игры
            resetGame();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        playerTex.dispose();
        platformTex.dispose();
        btnLeftTex.dispose();
        btnRightTex.dispose();
    }
}

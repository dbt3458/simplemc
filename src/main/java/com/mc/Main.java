package com.mc;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    private long window;
    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    private float lastX;
    private float lastY;
    private boolean firstMouse = true;
    private Camera camera;
    private World world;
    private Shader shader;
    private long lastTime = System.nanoTime();
    private float deltaTime;
    private CrosshairUI crosshair;
    private ProcessInput processInput;
    public void run() {
        init();
        loop();
        free();
    }

    private void generateDefaultConfig() {
        try {
            // 🔥 关键：获取 jar 所在的真实目录
            String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            File configFile = new File(jarFile.getParentFile(), "settings.cfg");

            if (!configFile.exists()) {
                FileWriter writer = new FileWriter(configFile);
                writer.write("render_distance=10\n");
                writer.write("chunk_cleanup_interval=15\n");
                writer.close();
                System.out.println("配置已生成: " + configFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static int loadRenderDistance() {
        try {
            Scanner scanner = new Scanner(new File("settings.cfg"));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("render_distance=")) {
                    String value = line.split("=")[1];
                    return Integer.parseInt(value.trim());
                }
            }
        } catch (Exception e) {
            return 6;
        }
        return 6;
    }

    private int renderDistance = loadRenderDistance();

    private void init() {
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW 初始化失败");
        }
        generateDefaultConfig();
        window = glfwCreateWindow(WIDTH, HEIGHT, "Mini MC", 0, 0);
        if (window == 0) {
            throw new RuntimeException("窗口创建失败");
        }

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - WIDTH) / 2, (vidmode.height() - HEIGHT) / 2);
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glClearColor(0.5f, 0.8f, 1.0f, 1);
        glfwSwapInterval(0);
        shader = new Shader();
        world = new World();
        camera = new Camera();
        Texture.loadTextures();
        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (firstMouse) {
                lastX = (float) x;
                lastY = (float) y;
                firstMouse = false;
            }

            float dx = (float) x - lastX;
            float dy = lastY - (float) y;
            lastX = (float) x;
            lastY = (float) y;
            camera.rotate(dx * 0.1f, dy * 0.1f);

        });
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        crosshair = new CrosshairUI(WIDTH, HEIGHT);
        processInput = new ProcessInput(camera, world, window);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            long now = System.nanoTime();
            deltaTime = (now - lastTime) / 1_000_000_000f;
            lastTime = now;
            world.cleanupPendingChunks();
            processInput.process(deltaTime);

            // ====================== 【适配你的原版Camera - 相机防穿模】 ======================
            RayCastResult cameraRay = RayCastResult.rayCast(camera, world, 0.4f);
            if (cameraRay.hit) {
                float pullBack = 0.08f;
                // 直接使用你的 camera.front 成员变量，零报错
                camera.position.x -= camera.front.x * pullBack;
                camera.position.y -= camera.front.y * pullBack;
                camera.position.z -= camera.front.z * pullBack;
            }
            // ============================================================================

            Vector3f p = camera.getPosition();
            int playerChunkX = (int) Math.floor(p.x / 16.0f);
            int playerChunkZ = (int) Math.floor(p.z / 16.0f);

            shader.use();
            shader.setMat4("model", new Matrix4f());
            shader.setMat4("view", camera.getViewMatrix());
            shader.setMat4("projection", new Matrix4f().perspective((float) Math.toRadians(70), (float)WIDTH/HEIGHT, 0.3f, 1000));
            Collision.resolveCameraCollision(camera, world, 0.08f);
            world.update(playerChunkX, playerChunkZ, renderDistance);
            for (Chunk chunk : world.getAllChunks()) {
                chunk.render();
            }

            crosshair.render();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }



    private void free() {
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public static void main(String[] args) {
        new Main().run();
    }
}
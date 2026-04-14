package com.mc;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
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
    private int renderDistance;
    private int cleanupIntervalSeconds;

    public void run() {
        init();
        loop();
        free();
    }


    private void init() {
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW 初始化失败");
        }
        LoadConfig.generateDefaultConfig();
        renderDistance = LoadConfig.getRenderDistance();
        cleanupIntervalSeconds = LoadConfig.getChunkCleanupInterval();
        window = glfwCreateWindow(WIDTH, HEIGHT, "Mini MC", 0, 0);
        if (window == 0) {
            throw new RuntimeException("窗口创建失败");
        }
        System.out.println("当前渲染距离"+renderDistance+"区块");
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
        long worldSeed = System.currentTimeMillis(); // 或使用固定的种子值，以便于调试
        Chunk.initTerrainGenerator(worldSeed);
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
            world.cleanupPendingChunks();
            long now = System.nanoTime();
            deltaTime = (now - lastTime) / 1_000_000_000f;
            lastTime = now;
            deltaTime = Math.min(deltaTime, 0.03f);// 避免卡顿时穿透
            processInput.process(deltaTime);
            Vector3f p = camera.getPosition();
            int playerChunkX = (int) Math.floor(p.x / 16.0f);
            int playerChunkZ = (int) Math.floor(p.z / 16.0f);

            shader.use();
            shader.setMat4("model", new Matrix4f());
            shader.setMat4("view", camera.getViewMatrix());
            shader.setMat4("projection", new Matrix4f().perspective((float) Math.toRadians(70), (float)WIDTH/HEIGHT, 0.3f, 1000));
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
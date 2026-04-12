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
    private float velocityY = 0;
    private long lastTime = System.nanoTime();
    private float deltaTime;
    private CrosshairUI crosshair;
    // 鼠标点击防抖（必须加！不然破坏放置不生效）
    private boolean leftClicked = false;
    private boolean rightClicked = false;
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
    }
    // 新增：穿透修正方法（放在 Main 类中）
    private void resolvePenetration(Vector3f pos, float halfWidth, float height) {
        AABB player = getPlayerAABBAt(pos.x, pos.y, pos.z);

        int minX = (int) Math.floor(player.minX) - 1;
        int maxX = (int) Math.ceil(player.maxX) + 1;
        int minY = (int) Math.floor(player.minY) - 2;
        int maxY = (int) Math.ceil(player.maxY) + 2;
        int minZ = (int) Math.floor(player.minZ) - 1;
        int maxZ = (int) Math.ceil(player.maxZ) + 1;

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (world.hasBlock(bx, by, bz)) {
                        AABB block = new AABB(bx - 0.5f, by, bz - 0.5f, bx + 0.5f, by + 1, bz + 0.5f);

                        if (player.intersects(block)) {
                            // 🔥 极简逻辑：只算穿透深度，选最小的推
                            float depthX = Math.min(player.maxX - block.minX, block.maxX - player.minX);
                            float depthY = Math.min(player.maxY - block.minY, block.maxY - player.minY);
                            float depthZ = Math.min(player.maxZ - block.minZ, block.maxZ - player.minZ);

                            // 🔥 关键：如果是侧面接触（X/Z有重叠但不是完全覆盖），优先推X/Z
                            boolean xFullyOverlaps = player.minX < block.maxX && player.maxX > block.minX;
                            boolean zFullyOverlaps = player.minZ < block.maxZ && player.maxZ > block.minZ;

                            // 只有当X和Z都完全覆盖方块时，才考虑Y轴推挤（真正的脚踩/头顶）
                            if (depthY <= depthX && depthY <= depthZ && xFullyOverlaps && zFullyOverlaps) {
                                // Y轴推挤
                                if (pos.y + height/2 > by + 0.5f) {
                                    pos.y = block.maxY;
                                } else {
                                    pos.y = block.minY - height;
                                }
                            } else if (depthX <= depthZ) {
                                // X轴推挤
                                if (pos.x > bx) {
                                    pos.x = block.maxX + halfWidth;
                                } else {
                                    pos.x = block.minX - halfWidth;
                                }
                            } else {
                                // Z轴推挤
                                if (pos.z > bz) {
                                    pos.z = block.maxZ + halfWidth;
                                } else {
                                    pos.z = block.minZ - halfWidth;
                                }
                            }

                            // 更新player AABB继续检查
                            player = getPlayerAABBAt(pos.x, pos.y, pos.z);
                        }
                    }
                }
            }
        }

    }
    // 替换原有的 moveWithCollision 方法
    private void moveWithCollision() {
        Vector3f p = camera.position;
        float speed = 12f;
        float delta = deltaTime;
        final float HALF_WIDTH = 0.3f;
        final float HEIGHT = 1.8f;

        // 移动方向
        Vector3f forward = new Vector3f(camera.front.x, 0, camera.front.z).normalize();
        Vector3f right = camera.getRight();
        right.y = 0;
        right.normalize();

        float mx = 0, mz = 0;
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            mx += forward.x * speed * delta;
            mz += forward.z * speed * delta;
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            mx -= forward.x * speed * delta;
            mz -= forward.z * speed * delta;
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            mx -= right.x * speed * delta;
            mz -= right.z * speed * delta;
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            mx += right.x * speed * delta;
            mz += right.z * speed * delta;
        }

        // -------------------- Y轴移动 + 碰撞 --------------------
// -------------------- Y轴移动 + 碰撞 --------------------
        float my = velocityY * delta;
        Vector3f tempPos = new Vector3f(p);
        AABB playerBox = getPlayerAABBAt(tempPos.x, tempPos.y, tempPos.z);

        int minX = (int) Math.floor(playerBox.minX) - 1;
        int maxX = (int) Math.ceil(playerBox.maxX) + 1;
        int minY = (int) Math.floor(playerBox.minY) - 2;
        int maxY = (int) Math.ceil(playerBox.maxY) + 2;
        int minZ = (int) Math.floor(playerBox.minZ) - 1;
        int maxZ = (int) Math.ceil(playerBox.maxZ) + 1;

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (world.hasBlock(bx, by, bz)) {
                        AABB block = new AABB(bx - 0.5f, by, bz - 0.5f, bx + 0.5f, by + 1, bz + 0.5f);
                        AABB testBox = getPlayerAABBAt(tempPos.x, tempPos.y, tempPos.z);

                        // 🔥 新增：只有当玩家在方块X/Z范围内时，才处理Y轴碰撞
                        boolean xOverlaps = testBox.minX < block.maxX && testBox.maxX > block.minX;
                        boolean zOverlaps = testBox.minZ < block.maxZ && testBox.maxZ > block.minZ;

                        if (xOverlaps && zOverlaps) {
                            my = testBox.collideY(block, my);
                        }
                    }
                }
            }
        }
        p.y += my;
        resolvePenetration(p, HALF_WIDTH, HEIGHT);

        // -------------------- X轴移动 + 碰撞 --------------------
        tempPos.set(p);
        playerBox = getPlayerAABBAt(tempPos.x, tempPos.y, tempPos.z);
        minX = (int) Math.floor(playerBox.minX) - 1;
        maxX = (int) Math.ceil(playerBox.maxX) + 1;
        minY = (int) Math.floor(playerBox.minY) - 2;
        maxY = (int) Math.ceil(playerBox.maxY) + 2;
        minZ = (int) Math.floor(playerBox.minZ) - 1;
        maxZ = (int) Math.ceil(playerBox.maxZ) + 1;

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (world.hasBlock(bx, by, bz)) {
                        AABB block = new AABB(bx - 0.5f, by, bz - 0.5f, bx + 0.5f, by + 1, bz + 0.5f);
                        AABB testBox = getPlayerAABBAt(tempPos.x, tempPos.y, tempPos.z);
                        mx = testBox.collideX(block, mx);
                    }
                }
            }
        }
        p.x += mx;
        resolvePenetration(p, HALF_WIDTH, HEIGHT);

        // -------------------- Z轴移动 + 碰撞 --------------------
        tempPos.set(p);
        playerBox = getPlayerAABBAt(tempPos.x, tempPos.y, tempPos.z);
        minX = (int) Math.floor(playerBox.minX) - 1;
        maxX = (int) Math.ceil(playerBox.maxX) + 1;
        minY = (int) Math.floor(playerBox.minY) - 2;
        maxY = (int) Math.ceil(playerBox.maxY) + 2;
        minZ = (int) Math.floor(playerBox.minZ) - 1;
        maxZ = (int) Math.ceil(playerBox.maxZ) + 1;

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (world.hasBlock(bx, by, bz)) {
                        AABB block = new AABB(bx - 0.5f, by, bz - 0.5f, bx + 0.5f, by + 1, bz + 0.5f);
                        AABB testBox = getPlayerAABBAt(tempPos.x, tempPos.y, tempPos.z);
                        mz = testBox.collideZ(block, mz);
                    }
                }
            }
        }
        p.z += mz;
        resolvePenetration(p, HALF_WIDTH, HEIGHT);

        // 应用最终位置
        camera.position.set(p);

        // -------------------- 地面检测与重力 --------------------
        boolean onGround = false;
        AABB feetCheck = getPlayerAABBAt(p.x, p.y - 0.01f, p.z);
        int checkY = (int) Math.floor(p.y - 0.1f);
        for (int bx = (int) Math.floor(feetCheck.minX); bx <= (int) Math.ceil(feetCheck.maxX); bx++) {
            for (int bz = (int) Math.floor(feetCheck.minZ); bz <= (int) Math.ceil(feetCheck.maxZ); bz++) {
                if (world.hasBlock(bx, checkY, bz)) {
                    AABB block = new AABB(bx - 0.5f, checkY, bz - 0.5f, bx + 0.5f, checkY + 1, bz + 0.5f);
                    if (feetCheck.intersects(block)) {
                        onGround = true;
                        break;
                    }
                }
            }
        }

        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && onGround) {
            velocityY = 12.0f;
        }
        if (!onGround) {
            velocityY -= 54.0f * delta;
        }
        if (onGround && velocityY < 0) {
            velocityY = 0;
        }
    }

    // 辅助方法（保持不变）
    private AABB getPlayerAABBAt(float x, float y, float z) {
        float w = 0.6f;
        float h = 1.8f;
        return new AABB(x - w/2, y, z - w/2, x + w/2, y + h, z + w/2);
    }
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            long now = System.nanoTime();
            deltaTime = (now - lastTime) / 1_000_000_000f;
            lastTime = now;
            world.cleanupPendingChunks();
            moveWithCollision();

            Vector3f p = camera.getPosition();
            int playerChunkX = (int) Math.floor(p.x / 16.0f);
            int playerChunkZ = (int) Math.floor(p.z / 16.0f);


            shader.use();
            shader.setMat4("model", new Matrix4f());
            shader.setMat4("view", camera.getViewMatrix());
            shader.setMat4("projection", new Matrix4f().perspective((float) Math.toRadians(70), (float)WIDTH/HEIGHT, 0.3f, 1000));

            world.update(playerChunkX, playerChunkZ, renderDistance);


            RayCastResult ray = RayCastResult.rayCast(camera, world, 8.0f);
            if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS && !leftClicked) {
                leftClicked = true;
                if (ray.hit) {
                    int cx = Math.floorDiv(ray.blockX, 16);
                    int cz = Math.floorDiv(ray.blockZ, 16);
                    int lx = ray.blockX - cx * 16;
                    int lz = ray.blockZ - cz * 16;
                    Chunk chunk = world.chunks.get(cx + "," + cz);

                    if (chunk != null) {
                        chunk.setBlock(lx, ray.blockY, lz, false);   // ✅ 直接使用 lx, lz
                    }
                }
            }
            if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_RELEASE) {
                leftClicked = false;
            }
            if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS && !rightClicked) {
                rightClicked = true;
                if (ray.hit) {
                    System.out.println("命中方块: (" + ray.blockX + "," + ray.blockY + "," + ray.blockZ + ") 面=" + ray.face);
                    int px = ray.blockX, py = ray.blockY, pz = ray.blockZ;
                    switch (ray.face) {
                        case 0: py++; break;
                        case 1: py--; break;
                        case 2: pz++; break;
                        case 3: pz--; break;
                        case 4: px++; break;
                        case 5: px--; break;
                    }
                    System.out.println("偏移后坐标: (" + px + "," + py + "," + pz + ")");

                    // 确保目标区块存在
                    int cx = Math.floorDiv(px, 16);
                    int cz = Math.floorDiv(pz, 16);
                    String key = cx + "," + cz;
                    if (!world.chunks.containsKey(key)) {
                        world.chunks.put(key, new Chunk(cx, cz));
                    }
                    Chunk chunk = world.chunks.get(key);

                    // 条件1：目标位置无方块（关键修改）
                    boolean noBlock = !world.hasBlock(px, py, pz);
                    // 条件2：不与玩家 AABB 重叠
                    AABB playerBox = getPlayerAABBAt(camera.position.x, camera.position.y, camera.position.z);
                    AABB targetBox = new AABB(px - 0.5f, py, pz - 0.5f, px + 0.5f, py + 1, pz + 0.5f);
                    boolean notOverlap = !playerBox.intersects(targetBox);
                    // 条件3：高度范围
                    boolean heightOk = (py >= 0 && py < 32);

                    // 必须同时满足三个条件
                    boolean canPlace = noBlock && notOverlap && heightOk;

                    if (canPlace && chunk != null) {
                        int lx = px - cx * 16;
                        int lz = pz - cz * 16;
                        chunk.setBlock(lx, py, lz, true);
                        System.out.println("放置成功: (" + px + "," + py + "," + pz + ")");
                    } else {
                        // 输出具体失败原因
                        if (!noBlock) System.out.println("放置失败: 目标位置 (" + px + "," + py + "," + pz + ") 已有方块");
                        else if (!notOverlap) System.out.println("放置失败: 与玩家重叠");
                        else if (!heightOk) System.out.println("放置失败: 高度超出范围");
                        else System.out.println("放置失败: 区块不存在");
                    }
                }
            }
            if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_RELEASE) {
                rightClicked = false;
            }
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
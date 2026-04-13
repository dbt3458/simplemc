package com.mc;

import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;

public class ProcessInput {
    private final Camera camera;
    private final World world;
    private final long window;
    private float velocityY = 0.0f;
    private boolean leftClicked = false;
    private boolean rightClicked = false;
    private static final float GRAVITY = 54.0f;
    private static final float JUMP_POWER = 12.0f;
    private static final float MOVE_SPEED = 10.0f;

    public ProcessInput(Camera camera, World world, long window) {
        this.camera = camera;
        this.world = world;
        this.window = window;
    }

    public void process(float deltaTime) {

        boolean onGround = Collision.isOnGround(camera.position, world);
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && onGround) {
            velocityY = 12.0f;          // 跳跃初速度
        }
        if (!onGround) {
            velocityY -= 54.0f * deltaTime;
        } else if (velocityY < 0) {
            velocityY = 0;
        }
        float my = velocityY * deltaTime;

// 计算水平移动
        Vector3f moveDelta = computeMoveDelta(deltaTime);
        moveDelta.y = my;

        Vector3f newPos = Collision.moveAndCollide(camera.position, moveDelta, world);
        camera.position.set(newPos);
        camera.position.set(newPos);
        // 3. 方块破坏 / 放置
        RayCastResult ray = RayCastResult.rayCast(camera, world, 8.0f);
        handleBlockBreaking(ray);
        handleBlockPlacing(ray);
        Vector3f oldPos = new Vector3f(camera.position);

// 限制实际位移长度不超过期望长度（修复贴墙加速）
        Vector3f expectedMove = new Vector3f(moveDelta);
        Vector3f actualMove = new Vector3f(newPos).sub(oldPos);
        float expectedLen = expectedMove.length();
        float actualLen = actualMove.length();
        if (expectedLen > 1e-5f && actualLen > expectedLen * 1.01f) {
            actualMove.mul(expectedLen / actualLen);
            newPos = oldPos.add(actualMove);
        }
        camera.position.set(newPos);
    }

    private Vector3f computeMoveDelta(float deltaTime) {
        Vector3f forward = new Vector3f(camera.front.x, 0, camera.front.z).normalize();
        Vector3f right = camera.getRight();
        right.y = 0;
        right.normalize();

        float mx = 0, mz = 0;
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            mx += forward.x;
            mz += forward.z;
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            mx -= forward.x;
            mz -= forward.z;
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            mx -= right.x;
            mz -= right.z;
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            mx += right.x;
            mz += right.z;
        }

        // 归一化（防止斜向速度过快）
        float len = (float) Math.sqrt(mx * mx + mz * mz);
        if (len > 1e-5f) {
            mx /= len;
            mz /= len;
        }

        float speed = MOVE_SPEED * deltaTime;
        return new Vector3f(mx * speed, 0, mz * speed);
    }

    private void handleBlockBreaking(RayCastResult ray) {
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS && !leftClicked) {
            leftClicked = true;
            if (ray.hit) {
                int cx = Math.floorDiv(ray.blockX, 16);
                int cz = Math.floorDiv(ray.blockZ, 16);
                int lx = ray.blockX - cx * 16;
                int lz = ray.blockZ - cz * 16;
                Chunk chunk = world.chunks.get(cx + "," + cz);
                if (chunk != null) {
                    chunk.setBlock(lx, ray.blockY, lz, false);
                }
            }
        }
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_RELEASE) {
            leftClicked = false;
        }
    }

    private void handleBlockPlacing(RayCastResult ray) {
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS && !rightClicked) {
            rightClicked = true;
            if (ray.hit) {
                int px = ray.blockX, py = ray.blockY, pz = ray.blockZ;
                switch (ray.face) {
                    case 0: py++; break;
                    case 1: py--; break;
                    case 2: pz++; break;
                    case 3: pz--; break;
                    case 4: px++; break;
                    case 5: px--; break;
                }
                int cx = Math.floorDiv(px, 16);
                int cz = Math.floorDiv(pz, 16);
                String key = cx + "," + cz;
                if (!world.chunks.containsKey(key)) {
                    world.chunks.put(key, new Chunk(cx, cz));
                }
                Chunk chunk = world.chunks.get(key);
                boolean noBlock = !world.hasBlock(px, py, pz);
                AABB playerBox = Collision.getPlayerAABBAt(camera.position.x, camera.position.y, camera.position.z);
                AABB targetBox = new AABB(px - 0.5f, py, pz - 0.5f, px + 0.5f, py + 1, pz + 0.5f);
                boolean notOverlap = !playerBox.intersects(targetBox);
                boolean heightOk = (py >= 0 && py < 32);
                if (noBlock && notOverlap && heightOk && chunk != null) {
                    int lx = px - cx * 16;
                    int lz = pz - cz * 16;
                    chunk.setBlock(lx, py, lz, true);
                }
            }
        }
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_RELEASE) {
            rightClicked = false;
        }
    }
}
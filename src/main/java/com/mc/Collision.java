package com.mc;

import org.joml.Vector3f;

public class Collision {
    public static final float PLAYER_WIDTH = 0.6f;
    public static final float PLAYER_HEIGHT = 1.8f;
    public static final float HALF_WIDTH = PLAYER_WIDTH / 2f;
    private static final float EPSILON = 1e-5f;
    private static final float FIX_AMOUNT = 0.001f; // 微小修正步长

    public static AABB getPlayerAABBAt(float x, float y, float z) {
        return new AABB(x - HALF_WIDTH, y, z - HALF_WIDTH,
                x + HALF_WIDTH, y + PLAYER_HEIGHT, z + HALF_WIDTH);
    }

    public static Vector3f moveAndCollide(Vector3f startPos, Vector3f moveDelta, World world) {
        Vector3f pos = new Vector3f(startPos);
        Vector3f remaining = new Vector3f(moveDelta);

        // 先彻底清除当前穿透
        forceNoPenetration(pos, world);

        for (int iter = 0; iter < 2 && (Math.abs(remaining.x) > EPSILON || Math.abs(remaining.y) > EPSILON || Math.abs(remaining.z) > EPSILON); iter++) {
            // 水平移动
            if (Math.abs(remaining.x) > EPSILON || Math.abs(remaining.z) > EPSILON) {
                Vector3f horizMove = new Vector3f(remaining.x, 0, remaining.z);
                Vector3f newHoriz = moveHorizontally(pos, horizMove, world);
                pos.x = newHoriz.x;
                pos.z = newHoriz.z;
                remaining.x = 0;
                remaining.z = 0;
                forceNoPenetration(pos, world);
            }
            // 垂直移动
            if (Math.abs(remaining.y) > EPSILON) {
                float newY = moveVertically(pos, remaining.y, world);
                pos.y = newY;
                remaining.y = 0;
                forceNoPenetration(pos, world);
            }
        }
        return pos;
    }

    // 强制将玩家从任何方块重叠中推出（但只推极小距离）
    private static void forceNoPenetration(Vector3f pos, World world) {
        AABB player = getPlayerAABBAt(pos.x, pos.y, pos.z);
        boolean fixed;
        int maxIter = 100;          // 最多尝试100次
        int iter = 0;
        do {
            fixed = false;
            int minX = (int) Math.floor(player.minX);
            int maxX = (int) Math.ceil(player.maxX);
            int minY = (int) Math.floor(player.minY);
            int maxY = (int) Math.ceil(player.maxY);
            int minZ = (int) Math.floor(player.minZ);
            int maxZ = (int) Math.ceil(player.maxZ);
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (world.hasBlock(x, y, z)) {
                            AABB block = new AABB(x - 0.5f, y, z - 0.5f, x + 0.5f, y + 1, z + 0.5f);
                            if (player.intersects(block)) {
                                // 计算穿透深度
                                float dx1 = player.maxX - block.minX;
                                float dx2 = block.maxX - player.minX;
                                float dy1 = player.maxY - block.minY;
                                float dy2 = block.maxY - player.minY;
                                float dz1 = player.maxZ - block.minZ;
                                float dz2 = block.maxZ - player.minZ;
                                float dx = Math.min(dx1, dx2);
                                float dy = Math.min(dy1, dy2);
                                float dz = Math.min(dz1, dz2);
                                if (dy <= dx && dy <= dz) {
                                    if (dy1 < dy2) pos.y -= dy1 + 0.001f;
                                    else pos.y += dy2 + 0.001f;
                                } else if (dx <= dz) {
                                    if (dx1 < dx2) pos.x -= dx1 + 0.001f;
                                    else pos.x += dx2 + 0.001f;
                                } else {
                                    if (dz1 < dz2) pos.z -= dz1 + 0.001f;
                                    else pos.z += dz2 + 0.001f;
                                }
                                fixed = true;
                                player = getPlayerAABBAt(pos.x, pos.y, pos.z);
                                break;
                            }
                        }
                    }
                    if (fixed) break;
                }
                if (fixed) break;
            }
            iter++;
            if (iter > maxIter) {
                System.err.println("forceNoPenetration: 达到最大迭代次数 " + maxIter + "，强制退出以避免死循环");
                break;
            }
        } while (fixed);
    }

    private static Vector3f moveHorizontally(Vector3f pos, Vector3f move, World world) {
        float x = pos.x;
        float z = pos.z;
        float moveX = move.x;
        float moveZ = move.z;

        // X轴移动
        if (Math.abs(moveX) > EPSILON) {
            float newX = x + moveX;
            AABB playerBox = getPlayerAABBAt(newX, pos.y, z);
            if (!collidesWithWorld(playerBox, world)) {
                x = newX;
            } else {
                if (moveX > 0) {
                    float nearest = Float.POSITIVE_INFINITY;
                    int minY = (int) Math.floor(playerBox.minY);
                    int maxY = (int) Math.ceil(playerBox.maxY);
                    int minZ = (int) Math.floor(playerBox.minZ);
                    int maxZ = (int) Math.ceil(playerBox.maxZ);
                    for (int y = minY; y <= maxY; y++) {
                        for (int zz = minZ; zz <= maxZ; zz++) {
                            int xBlock = (int) Math.floor(playerBox.maxX);
                            if (world.hasBlock(xBlock, y, zz)) {
                                float blockLeft = xBlock - 0.5f;
                                float collisionX = blockLeft - HALF_WIDTH;
                                if (collisionX > x && collisionX < nearest) {
                                    nearest = collisionX;
                                }
                            }
                        }
                    }
                    if (nearest != Float.POSITIVE_INFINITY) x = nearest;
                } else {
                    float nearest = Float.NEGATIVE_INFINITY;
                    int minY = (int) Math.floor(playerBox.minY);
                    int maxY = (int) Math.ceil(playerBox.maxY);
                    int minZ = (int) Math.floor(playerBox.minZ);
                    int maxZ = (int) Math.ceil(playerBox.maxZ);
                    for (int y = minY; y <= maxY; y++) {
                        for (int zz = minZ; zz <= maxZ; zz++) {
                            int xBlock = (int) Math.ceil(playerBox.minX) - 1;
                            if (world.hasBlock(xBlock, y, zz)) {
                                float blockRight = xBlock + 0.5f;
                                float collisionX = blockRight + HALF_WIDTH;
                                if (collisionX < x && collisionX > nearest) {
                                    nearest = collisionX;
                                }
                            }
                        }
                    }
                    if (nearest != Float.NEGATIVE_INFINITY) x = nearest;
                }
            }
        }

        // Z轴移动
        if (Math.abs(moveZ) > EPSILON) {
            float newZ = z + moveZ;
            AABB playerBox = getPlayerAABBAt(x, pos.y, newZ);
            if (!collidesWithWorld(playerBox, world)) {
                z = newZ;
            } else {
                if (moveZ > 0) {
                    float nearest = Float.POSITIVE_INFINITY;
                    int minY = (int) Math.floor(playerBox.minY);
                    int maxY = (int) Math.ceil(playerBox.maxY);
                    int minX = (int) Math.floor(playerBox.minX);
                    int maxX = (int) Math.ceil(playerBox.maxX);
                    for (int y = minY; y <= maxY; y++) {
                        for (int xx = minX; xx <= maxX; xx++) {
                            int zBlock = (int) Math.floor(playerBox.maxZ);
                            if (world.hasBlock(xx, y, zBlock)) {
                                float blockFront = zBlock - 0.5f;
                                float collisionZ = blockFront - HALF_WIDTH;
                                if (collisionZ > z && collisionZ < nearest) {
                                    nearest = collisionZ;
                                }
                            }
                        }
                    }
                    if (nearest != Float.POSITIVE_INFINITY) z = nearest;
                } else {
                    float nearest = Float.NEGATIVE_INFINITY;
                    int minY = (int) Math.floor(playerBox.minY);
                    int maxY = (int) Math.ceil(playerBox.maxY);
                    int minX = (int) Math.floor(playerBox.minX);
                    int maxX = (int) Math.ceil(playerBox.maxX);
                    for (int y = minY; y <= maxY; y++) {
                        for (int xx = minX; xx <= maxX; xx++) {
                            int zBlock = (int) Math.ceil(playerBox.minZ) - 1;
                            if (world.hasBlock(xx, y, zBlock)) {
                                float blockBack = zBlock + 0.5f;
                                float collisionZ = blockBack + HALF_WIDTH;
                                if (collisionZ < z && collisionZ > nearest) {
                                    nearest = collisionZ;
                                }
                            }
                        }
                    }
                    if (nearest != Float.NEGATIVE_INFINITY) z = nearest;
                }
            }
        }
        return new Vector3f(x, pos.y, z);
    }

    // 垂直移动：扩大检测范围，确保不漏掉头顶方块
    private static float moveVertically(Vector3f pos, float moveY, World world) {
        if (moveY == 0) return pos.y;
        float newY = pos.y + moveY;
        // 扩大水平范围 0.1 格，彻底覆盖紧贴情况
        float margin = 0.1f;
        int minX = (int) Math.floor(pos.x - HALF_WIDTH - margin);
        int maxX = (int) Math.ceil(pos.x + HALF_WIDTH + margin);
        int minZ = (int) Math.floor(pos.z - HALF_WIDTH - margin);
        int maxZ = (int) Math.ceil(pos.z + HALF_WIDTH + margin);

        if (moveY > 0) {
            float nearest = newY;
            int startY = (int) Math.floor(pos.y + PLAYER_HEIGHT);
            int endY = (int) Math.ceil(newY + PLAYER_HEIGHT);
            for (int y = startY; y <= endY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (world.hasBlock(x, y, z)) {
                            float blockBottom = y;
                            float collisionY = blockBottom - PLAYER_HEIGHT;
                            if (collisionY > pos.y && collisionY < nearest) {
                                nearest = collisionY;
                            }
                        }
                    }
                }
            }
            return Math.max(pos.y, Math.min(newY, nearest));
        } else {
            float nearest = newY;
            int startY = (int) Math.floor(newY);
            int endY = (int) Math.ceil(pos.y);
            for (int y = startY; y <= endY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (world.hasBlock(x, y, z)) {
                            float blockTop = y + 1;
                            float collisionY = blockTop;
                            if (collisionY < pos.y && collisionY > nearest) {
                                nearest = collisionY;
                            }
                        }
                    }
                }
            }
            return Math.min(pos.y, Math.max(newY, nearest));
        }
    }

    private static boolean collidesWithWorld(AABB box, World world) {
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.ceil(box.maxX);
        int minY = (int) Math.floor(box.minY);
        int maxY = (int) Math.ceil(box.maxY);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.ceil(box.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.hasBlock(x, y, z)) {
                        AABB block = new AABB(x - 0.5f, y, z - 0.5f, x + 0.5f, y + 1, z + 0.5f);
                        if (box.intersects(block)) return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isOnGround(Vector3f position, World world) {
        AABB feetBox = getPlayerAABBAt(position.x, position.y - 0.1f, position.z);
        int minX = (int) Math.floor(feetBox.minX);
        int maxX = (int) Math.ceil(feetBox.maxX);
        int minZ = (int) Math.floor(feetBox.minZ);
        int maxZ = (int) Math.ceil(feetBox.maxZ);
        int checkY = (int) Math.floor(position.y - 0.05f);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (world.hasBlock(x, checkY, z)) {
                    AABB block = new AABB(x - 0.5f, checkY, z - 0.5f, x + 0.5f, checkY + 1, z + 0.5f);
                    if (feetBox.intersects(block)) return true;
                }
            }
        }
        return false;
    }
}
package com.mc;

import org.joml.Vector3f;

public class Collision {
    public static final float PLAYER_WIDTH = 0.6f;
    public static final float PLAYER_HEIGHT = 1.8f;
    public static final float HALF_WIDTH = PLAYER_WIDTH / 2f;
    private static final float EPSILON = 0.001f;
    private static final float MIN_PENETRATION = 0.005f;

    public static AABB getPlayerAABBAt(float x, float y, float z) {
        return new AABB(x - HALF_WIDTH, y, z - HALF_WIDTH,
                x + HALF_WIDTH, y + PLAYER_HEIGHT, z + HALF_WIDTH);
    }

    public static Vector3f moveAndCollide(Vector3f startPos, Vector3f moveDelta, World world) {
        Vector3f pos = new Vector3f(startPos);
        // 记录移动前的坐标
        /*System.out.printf("[移动前] (%.6f, %.6f, %.6f) 移动增量 (%.6f, %.6f, %.6f)%n",
                pos.x, pos.y, pos.z, moveDelta.x, moveDelta.y, moveDelta.z);*/

        // Y轴移动
        if (moveDelta.y != 0) {
            float oldY = pos.y;
            pos.y += moveY(pos, moveDelta.y, world);
            if (Math.abs(pos.y - oldY - moveDelta.y) > 0.001f) {
                /*System.out.printf("[Y轴碰撞] 期望移动 %.6f, 实际移动 %.6f, 新Y %.6f%n",
                        moveDelta.y, pos.y - oldY, pos.y);*/
            }
        }

        // X轴移动
        if (moveDelta.x != 0) {
            float oldX = pos.x;
            pos.x += moveAxisX(pos, moveDelta.x, world);
            if (Math.abs(pos.x - oldX - moveDelta.x) > 0.001f) {
                /*System.out.printf("[X轴碰撞] 期望移动 %.6f, 实际移动 %.6f, 新X %.6f%n",
                        moveDelta.x, pos.x - oldX, pos.x);*/
            }
        }

        // Z轴移动
        if (moveDelta.z != 0) {
            float oldZ = pos.z;
            pos.z += moveAxisZ(pos, moveDelta.z, world);
            if (Math.abs(pos.z - oldZ - moveDelta.z) > 0.001f) {
                /*System.out.printf("[Z轴碰撞] 期望移动 %.6f, 实际移动 %.6f, 新Z %.6f%n",
                        moveDelta.z, pos.z - oldZ, pos.z);*/
            }
        }

        // 穿透修正前坐标
        Vector3f beforeResolve = new Vector3f(pos);
        resolvePenetration(pos, world);
        if (!beforeResolve.equals(pos)) {
            /*System.out.printf("[穿透修正] 修正前 (%.6f, %.6f, %.6f) 修正后 (%.6f, %.6f, %.6f)%n",
                    beforeResolve.x, beforeResolve.y, beforeResolve.z,
                    pos.x, pos.y, pos.z);*/
        }

        //System.out.printf("[移动后] (%.6f, %.6f, %.6f)%n", pos.x, pos.y, pos.z);
        return pos;
    }

    private static float moveY(Vector3f pos, float moveY, World world) {
        if (moveY == 0) return 0;
        float startY = pos.y;
        float endY = startY + moveY;
        AABB playerBox = getPlayerAABBAt(pos.x, startY, pos.z);
        int minX = (int) Math.floor(playerBox.minX);
        int maxX = (int) Math.ceil(playerBox.maxX);
        int minZ = (int) Math.floor(playerBox.minZ);
        int maxZ = (int) Math.ceil(playerBox.maxZ);

        int step = (moveY > 0) ? 1 : -1;
        int startYBlock = (moveY > 0) ? (int) Math.floor(playerBox.maxY) : (int) Math.ceil(playerBox.minY);
        int endYBlock = (moveY > 0) ? (int) Math.ceil(endY + PLAYER_HEIGHT) : (int) Math.floor(endY);

        float nearest = (moveY > 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        boolean collided = false;
        float lastCollisionY = startY;

        for (int y = startYBlock; y != endYBlock + step; y += step) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.hasBlock(x, y, z)) {
                        AABB block = new AABB(x - 0.5f, y, z - 0.5f, x + 0.5f, y + 1, z + 0.5f);
                        if (moveY > 0) {
                            float collisionY = block.minY - PLAYER_HEIGHT - EPSILON;
                            if (collisionY > startY && collisionY < nearest) {
                                nearest = collisionY;
                                collided = true;
                                lastCollisionY = collisionY;
                            }
                        } else {
                            float collisionY = block.maxY + EPSILON;
                            if (collisionY < startY && collisionY > nearest) {
                                nearest = collisionY;
                                collided = true;
                                lastCollisionY = collisionY;
                            }
                        }
                    }
                }
            }
        }

        float moved;
        if (collided) {
            moved = nearest - startY;
        } else {
            moved = moveY;
        }

        // 异常检测：实际移动与期望移动符号相反或绝对值相差很大（超过 0.5 米）
        if (Math.abs(moved - moveY) > 0.5f || (moveY != 0 && Math.signum(moved) != Math.signum(moveY))) {
            System.out.printf("[异常Y移动] 期望=%.6f, 实际=%.6f, startY=%.6f, nearest=%.6f, collided=%s, 最后碰撞点=%.6f%n",
                    moveY, moved, startY, nearest, collided, lastCollisionY);
            System.out.printf("  玩家位置: (%.6f,%.6f,%.6f)%n", pos.x, startY, pos.z);
            // 可选：打印附近的方块范围
            System.out.printf("  Y范围: %d -> %d, step=%d%n", startYBlock, endYBlock, step);
        }
        return moved;
    }
    private static float moveAxisX(Vector3f pos, float moveX, World world) {
        if (moveX == 0) return 0;
        float startX = pos.x;
        float endX = startX + moveX;
        AABB playerBox = getPlayerAABBAt(startX, pos.y, pos.z);
        int minY = (int) Math.floor(playerBox.minY);
        int maxY = (int) Math.ceil(playerBox.maxY);
        int minZ = (int) Math.floor(playerBox.minZ);
        int maxZ = (int) Math.ceil(playerBox.maxZ);

        int step = (moveX > 0) ? 1 : -1;
        int startXBlock = (moveX > 0) ? (int) Math.floor(playerBox.maxX) : (int) Math.ceil(playerBox.minX);
        int endXBlock = (moveX > 0) ? (int) Math.ceil(endX + HALF_WIDTH) : (int) Math.floor(endX - HALF_WIDTH);

        float nearest = (moveX > 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        boolean collided = false;
        float lastCollisionX = startX;

        for (int x = startXBlock; x != endXBlock + step; x += step) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (world.hasBlock(x, y, z)) {
                        AABB block = new AABB(x - 0.5f, y, z - 0.5f, x + 0.5f, y + 1, z + 0.5f);
                        if (moveX > 0) {
                            float collisionX = block.minX - HALF_WIDTH - EPSILON;
                            if (collisionX > startX && collisionX < nearest) {
                                nearest = collisionX;
                                collided = true;
                                lastCollisionX = collisionX;
                            }
                        } else {
                            float collisionX = block.maxX + HALF_WIDTH + EPSILON;
                            if (collisionX < startX && collisionX > nearest) {
                                nearest = collisionX;
                                collided = true;
                                lastCollisionX = collisionX;
                            }
                        }
                    }
                }
            }
        }

        float moved;
        if (collided) {
            moved = nearest - startX;
        } else {
            moved = moveX;
        }

        // 异常检测：如果实际移动与期望移动符号相反或绝对值相差很大
        if (Math.abs(moved - moveX) > 0.5f) {
            System.out.printf("[异常X移动] 期望=%.6f, 实际=%.6f, startX=%.6f, nearest=%.6f, collided=%s, 最后碰撞点=%.6f%n",
                    moveX, moved, startX, nearest, collided, lastCollisionX);
            // 可选：打印涉及的方块信息
            System.out.printf("  位置: (%.6f,%.6f,%.6f)%n", pos.x, pos.y, pos.z);
        }
        return moved;
    }

    private static float moveAxisZ(Vector3f pos, float moveZ, World world) {
        if (moveZ == 0) return 0;
        float startZ = pos.z;
        float endZ = startZ + moveZ;
        AABB playerBox = getPlayerAABBAt(pos.x, pos.y, startZ);
        int minX = (int) Math.floor(playerBox.minX);
        int maxX = (int) Math.ceil(playerBox.maxX);
        int minY = (int) Math.floor(playerBox.minY);
        int maxY = (int) Math.ceil(playerBox.maxY);

        int step = (moveZ > 0) ? 1 : -1;
        int startZBlock = (moveZ > 0) ? (int) Math.floor(playerBox.maxZ) : (int) Math.ceil(playerBox.minZ);
        int endZBlock = (moveZ > 0) ? (int) Math.ceil(endZ + HALF_WIDTH) : (int) Math.floor(endZ - HALF_WIDTH);

        float nearest = (moveZ > 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        boolean collided = false;
        float lastCollisionZ = startZ;

        for (int z = startZBlock; z != endZBlock + step; z += step) {
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (world.hasBlock(x, y, z)) {
                        AABB block = new AABB(x - 0.5f, y, z - 0.5f, x + 0.5f, y + 1, z + 0.5f);
                        if (moveZ > 0) {
                            float collisionZ = block.minZ - HALF_WIDTH - EPSILON;
                            if (collisionZ > startZ && collisionZ < nearest) {
                                nearest = collisionZ;
                                collided = true;
                                lastCollisionZ = collisionZ;
                            }
                        } else {
                            float collisionZ = block.maxZ + HALF_WIDTH + EPSILON;
                            if (collisionZ < startZ && collisionZ > nearest) {
                                nearest = collisionZ;
                                collided = true;
                                lastCollisionZ = collisionZ;
                            }
                        }
                    }
                }
            }
        }

        float moved;
        if (collided) {
            moved = nearest - startZ;
        } else {
            moved = moveZ;
        }

        if (Math.abs(moved - moveZ) > 0.5f) {
            System.out.printf("[异常Z移动] 期望=%.6f, 实际=%.6f, startZ=%.6f, nearest=%.6f, collided=%s, 最后碰撞点=%.6f%n",
                    moveZ, moved, startZ, nearest, collided, lastCollisionZ);
            System.out.printf("  位置: (%.6f,%.6f,%.6f)%n", pos.x, pos.y, pos.z);
        }
        return moved;
    }

    private static void resolvePenetration(Vector3f pos, World world) {
        final int MAX_ITER = 2;
        for (int iter = 0; iter < MAX_ITER; iter++) {
            AABB player = getPlayerAABBAt(pos.x, pos.y, pos.z);
            boolean resolved = false;
            int minX = (int) Math.floor(player.minX) - 1;
            int maxX = (int) Math.ceil(player.maxX) + 1;
            int minY = (int) Math.floor(player.minY) - 1;
            int maxY = (int) Math.ceil(player.maxY) + 1;
            int minZ = (int) Math.floor(player.minZ) - 1;
            int maxZ = (int) Math.ceil(player.maxZ) + 1;

            for (int bx = minX; bx <= maxX; bx++) {
                for (int by = minY; by <= maxY; by++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        if (world.hasBlock(bx, by, bz)) {
                            AABB block = new AABB(bx - 0.5f, by, bz - 0.5f, bx + 0.5f, by + 1, bz + 0.5f);
                            if (player.intersects(block)) {
                                float dx1 = player.maxX - block.minX;
                                float dx2 = block.maxX - player.minX;
                                float dy1 = player.maxY - block.minY;
                                float dy2 = block.maxY - player.minY;
                                float dz1 = player.maxZ - block.minZ;
                                float dz2 = block.maxZ - player.minZ;
                                float dx = Math.min(dx1, dx2);
                                float dy = Math.min(dy1, dy2);
                                float dz = Math.min(dz1, dz2);
                                if (dx < MIN_PENETRATION && dy < MIN_PENETRATION && dz < MIN_PENETRATION)
                                    continue;
                                // 记录穿透修正细节
                                /*System.out.printf("[穿透修正] 与方块(%d,%d,%d)重叠: dx=%.6f, dy=%.6f, dz=%.6f%n",
                                        bx, by, bz, dx, dy, dz);*/
                                if (dy <= dx && dy <= dz) {
                                    if (dy1 < dy2) pos.y -= dy1;
                                    else pos.y += dy2;
                                } else if (dx <= dz) {
                                    if (dx1 < dx2) pos.x -= dx1;
                                    else pos.x += dx2;
                                } else {
                                    if (dz1 < dz2) pos.z -= dz1;
                                    else pos.z += dz2;
                                }
                                resolved = true;
                                player = getPlayerAABBAt(pos.x, pos.y, pos.z);
                            }
                        }
                    }
                }
            }
            if (!resolved) break;
        }
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
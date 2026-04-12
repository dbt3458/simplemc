package com.mc;

import org.joml.Vector3f;

public class Collision {
    // 玩家尺寸常量
    public static final float PLAYER_WIDTH = 0.6f;
    public static final float PLAYER_HEIGHT = 1.8f;
    public static final float HALF_WIDTH = PLAYER_WIDTH / 2f;
    private static final float SAFE_GAP = 0.05f;

    // 获取玩家在指定位置的AABB
    public static AABB getPlayerAABBAt(float x, float y, float z) {
        return new AABB(x - HALF_WIDTH, y, z - HALF_WIDTH,
                x + HALF_WIDTH, y + PLAYER_HEIGHT, z + HALF_WIDTH);
    }

    // 穿透修正（从原Main移入）
    public static void resolvePenetration(Vector3f pos, World world) {
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
                        AABB block = new AABB(bx - 0.5f, by, bz - 0.5f,
                                bx + 0.5f, by + 1, bz + 0.5f);
                        if (player.intersects(block)) {
                            float depthX = Math.min(player.maxX - block.minX, block.maxX - player.minX);
                            float depthY = Math.min(player.maxY - block.minY, block.maxY - player.minY);
                            float depthZ = Math.min(player.maxZ - block.minZ, block.maxZ - player.minZ);

                            boolean xFullyOverlaps = player.minX <= block.minX && player.maxX >= block.maxX;
                            boolean zFullyOverlaps = player.minZ <= block.minZ && player.maxZ >= block.maxZ;

                            if (depthY <= depthX && depthY <= depthZ && xFullyOverlaps && zFullyOverlaps) {
                                if (pos.y + PLAYER_HEIGHT/2 > by + 0.5f)
                                    pos.y = block.maxY + SAFE_GAP;
                                else
                                    pos.y = block.minY - PLAYER_HEIGHT - SAFE_GAP;
                            } else if (depthX <= depthZ) {
                                if (pos.x > bx)
                                    pos.x = block.maxX + HALF_WIDTH + SAFE_GAP;
                                else
                                    pos.x = block.minX - HALF_WIDTH - SAFE_GAP;
                            } else {
                                if (pos.z > bz)
                                    pos.z = block.maxZ + HALF_WIDTH + SAFE_GAP;
                                else
                                    pos.z = block.minZ - HALF_WIDTH - SAFE_GAP;
                            }
                            player = getPlayerAABBAt(pos.x, pos.y, pos.z);
                        }
                    }
                }
            }
        }
    }

    // 应用移动增量（分轴碰撞+穿透修正）
    public static Vector3f applyMovement(Vector3f startPos, Vector3f moveDelta, World world) {
        Vector3f pos = new Vector3f(startPos);
        // Y轴移动
        float my = moveDelta.y;
        AABB playerBox = getPlayerAABBAt(pos.x, pos.y, pos.z);
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
                        AABB block = new AABB(bx - 0.5f, by, bz - 0.5f,
                                bx + 0.5f, by + 1, bz + 0.5f);
                        AABB testBox = getPlayerAABBAt(pos.x, pos.y, pos.z);
                        boolean xOverlaps = testBox.minX < block.maxX && testBox.maxX > block.minX;
                        boolean zOverlaps = testBox.minZ < block.maxZ && testBox.maxZ > block.minZ;
                        if (xOverlaps && zOverlaps) {
                            my = testBox.collideY(block, my);
                        }
                    }
                }
            }
        }
        pos.y += my;
        resolvePenetration(pos, world);

        // X轴移动
        float mx = moveDelta.x;
        playerBox = getPlayerAABBAt(pos.x, pos.y, pos.z);
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
                        AABB block = new AABB(bx - 0.5f, by, bz - 0.5f,
                                bx + 0.5f, by + 1, bz + 0.5f);
                        AABB testBox = getPlayerAABBAt(pos.x, pos.y, pos.z);
                        mx = testBox.collideX(block, mx);
                    }
                }
            }
        }
        pos.x += mx;
        resolvePenetration(pos, world);

        // Z轴移动
        float mz = moveDelta.z;
        playerBox = getPlayerAABBAt(pos.x, pos.y, pos.z);
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
                        AABB block = new AABB(bx - 0.5f, by, bz - 0.5f,
                                bx + 0.5f, by + 1, bz + 0.5f);
                        AABB testBox = getPlayerAABBAt(pos.x, pos.y, pos.z);
                        mz = testBox.collideZ(block, mz);
                    }
                }
            }
        }
        pos.z += mz;
        resolvePenetration(pos, world);

        return pos;
    }

    // 检测是否站在地面上
    public static boolean isOnGround(Vector3f position, World world) {
        AABB feetCheck = getPlayerAABBAt(position.x, position.y - 0.01f, position.z);
        int checkY = (int) Math.floor(position.y - 0.1f);
        for (int bx = (int) Math.floor(feetCheck.minX); bx <= (int) Math.ceil(feetCheck.maxX); bx++) {
            for (int bz = (int) Math.floor(feetCheck.minZ); bz <= (int) Math.ceil(feetCheck.maxZ); bz++) {
                if (world.hasBlock(bx, checkY, bz)) {
                    AABB block = new AABB(bx - 0.5f, checkY, bz - 0.5f,
                            bx + 0.5f, checkY + 1, bz + 0.5f);
                    if (feetCheck.intersects(block)) return true;
                }
            }
        }
        return false;
    }

    // 相机防穿模（避免相机钻进方块）
    public static void resolveCameraCollision(Camera camera, World world, float pullBack) {
        RayCastResult cameraRay = RayCastResult.rayCast(camera, world, 0.4f);
        if (cameraRay.hit) {
            camera.position.x -= camera.front.x * pullBack;
            camera.position.y -= camera.front.y * pullBack;
            camera.position.z -= camera.front.z * pullBack;
        }
    }
}
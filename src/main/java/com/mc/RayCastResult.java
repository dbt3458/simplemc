package com.mc;

import org.joml.Vector3f;

public class RayCastResult {
    public boolean hit;
    public int blockX, blockY, blockZ;
    public Vector3f hitPos;
    public int face;

    public RayCastResult() {
        hit = false;
        hitPos = new Vector3f();
    }

    /**
     * 射线检测（适配方块中心坐标模型）
     * @param camera   相机（眼睛位置 = position + (0,1.5,0)）
     * @param world    世界
     * @param maxDist  最大检测距离
     * @return 命中结果
     */
    public static RayCastResult rayCast(Camera camera, World world, float maxDist) {
        RayCastResult res = new RayCastResult();
        Vector3f start = new Vector3f(camera.position).add(0, 1.5f, 0);
        Vector3f dir = new Vector3f(camera.front).normalize();

        int bx = (int) Math.floor(start.x + 0.5f);
        int by = (int) Math.floor(start.y + 0.5f);
        int bz = (int) Math.floor(start.z + 0.5f);

        int stepX = (dir.x > 0) ? 1 : -1;
        int stepY = (dir.y > 0) ? 1 : -1;
        int stepZ = (dir.z > 0) ? 1 : -1;

        float tDeltaX = (dir.x == 0) ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dir.x);
        float tDeltaY = (dir.y == 0) ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dir.y);
        float tDeltaZ = (dir.z == 0) ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dir.z);

        float tMaxX, tMaxY, tMaxZ;
        if (dir.x > 0) tMaxX = ((bx + 0.5f) - start.x) / dir.x;
        else if (dir.x < 0) tMaxX = (start.x - (bx - 0.5f)) / (-dir.x);
        else tMaxX = Float.POSITIVE_INFINITY;

        if (dir.y > 0) tMaxY = ((by + 0.5f) - start.y) / dir.y;
        else if (dir.y < 0) tMaxY = (start.y - (by - 0.5f)) / (-dir.y);
        else tMaxY = Float.POSITIVE_INFINITY;

        if (dir.z > 0) tMaxZ = ((bz + 0.5f) - start.z) / dir.z;
        else if (dir.z < 0) tMaxZ = (start.z - (bz - 0.5f)) / (-dir.z);
        else tMaxZ = Float.POSITIVE_INFINITY;

        float distance = 0;
        while (distance < maxDist) {
            if (world.hasBlock(bx, by, bz)) {
                res.hit = true;
                res.blockX = bx;
                res.blockY = by;
                res.blockZ = bz;

                // 精确计算击中的面
                float t = Float.POSITIVE_INFINITY;
                int hitFace = -1;
                for (int i = 0; i < 6; i++) {
                    float nx = 0, ny = 0, nz = 0, d = 0; // 初始化，避免编译错误
                    switch (i) {
                        case 0: nx = 0; ny = 1; nz = 0; d = by + 0.5f; break; // 上
                        case 1: nx = 0; ny = -1; nz = 0; d = by - 0.5f; break; // 下
                        case 2: nx = 0; ny = 0; nz = 1; d = bz + 0.5f; break; // 北
                        case 3: nx = 0; ny = 0; nz = -1; d = bz - 0.5f; break; // 南
                        case 4: nx = 1; ny = 0; nz = 0; d = bx + 0.5f; break; // 东
                        case 5: nx = -1; ny = 0; nz = 0; d = bx - 0.5f; break; // 西
                    }
                    float denom = dir.x * nx + dir.y * ny + dir.z * nz;
                    if (Math.abs(denom) < 1e-6) continue;
                    float tPlane = (d - (start.x * nx + start.y * ny + start.z * nz)) / denom;
                    if (tPlane < 0 || tPlane > t) continue;
                    // 计算交点
                    float ix = start.x + dir.x * tPlane;
                    float iy = start.y + dir.y * tPlane;
                    float iz = start.z + dir.z * tPlane;
                    float eps = 1e-4f;
                    boolean inside = false;
                    switch (i) {
                        case 0: case 1: // 上下
                            inside = (ix >= bx - 0.5f - eps && ix <= bx + 0.5f + eps &&
                                    iz >= bz - 0.5f - eps && iz <= bz + 0.5f + eps);
                            break;
                        case 2: case 3: // 北南
                            inside = (ix >= bx - 0.5f - eps && ix <= bx + 0.5f + eps &&
                                    iy >= by - 0.5f - eps && iy <= by + 0.5f + eps);
                            break;
                        case 4: case 5: // 东西
                            inside = (iy >= by - 0.5f - eps && iy <= by + 0.5f + eps &&
                                    iz >= bz - 0.5f - eps && iz <= bz + 0.5f + eps);
                            break;
                    }
                    if (inside) {
                        t = tPlane;
                        hitFace = i;
                    }
                }
                res.face = hitFace;
                return res;
            }

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                distance = tMaxX;
                bx += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                distance = tMaxY;
                by += stepY;
                tMaxY += tDeltaY;
            } else {
                distance = tMaxZ;
                bz += stepZ;
                tMaxZ += tDeltaZ;
            }
        }
        return res;
    }
}
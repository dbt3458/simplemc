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
        Vector3f start = new Vector3f(camera.position).add(0, 1.15f, 0);
        Vector3f dir = new Vector3f(camera.front).normalize();

        int bx = (int) Math.floor(start.x + 0.5f);
        int by = (int) Math.floor(start.y + 0.5f);
        int bz = (int) Math.floor(start.z + 0.5f);

        int stepX = dir.x > 0 ? 1 : -1;
        int stepY = dir.y > 0 ? 1 : -1;
        int stepZ = dir.z > 0 ? 1 : -1;

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
        int lastStepAxis = -1; // 0:X, 1:Y, 2:Z

        while (distance < maxDist) {
            if (world.hasBlock(bx, by, bz)) {
                res.hit = true;
                res.blockX = bx;
                res.blockY = by;
                res.blockZ = bz;

                // 根据最后一步的轴和方向确定击中的面
                if (lastStepAxis == 0) {
                    // 向 stepX 方向步进，射线从相反方向击中
                    res.face = (stepX > 0) ? 5 : 4; // stepX>0 向西步进 → 击中西面(5)
                } else if (lastStepAxis == 1) {
                    res.face = (stepY > 0) ? 1 : 0; // stepY>0 向下步进 → 击中下面(1)
                } else if (lastStepAxis == 2) {
                    res.face = (stepZ > 0) ? 3 : 2; // stepZ>0 向南步进 → 击中南面(3)
                } else {
                    // 起点就在方块内（极少情况），回退到基于偏移的判定
                    float hitX = start.x + dir.x * distance;
                    float hitY = start.y + dir.y * distance;
                    float hitZ = start.z + dir.z * distance;
                    float dx = hitX - bx, dy = hitY - by, dz = hitZ - bz;
                    float adx = Math.abs(dx), ady = Math.abs(dy), adz = Math.abs(dz);
                    if (adx > ady && adx > adz) res.face = dx > 0 ? 4 : 5;
                    else if (ady > adz) res.face = dy > 0 ? 0 : 1;
                    else res.face = dz > 0 ? 2 : 3;
                }
                return res;
            }

            // 步进并记录移动轴
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                distance = tMaxX;
                bx += stepX;
                tMaxX += tDeltaX;
                lastStepAxis = 0;
            } else if (tMaxY < tMaxZ) {
                distance = tMaxY;
                by += stepY;
                tMaxY += tDeltaY;
                lastStepAxis = 1;
            } else {
                distance = tMaxZ;
                bz += stepZ;
                tMaxZ += tDeltaZ;
                lastStepAxis = 2;
            }
        }
        return res;
    }
}
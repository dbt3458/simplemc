package com.mc;

public class AABB {
    public float minX, minY, minZ;
    public float maxX, maxY, maxZ;
    private static final float EPSILON = 0.001f;

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public boolean intersects(AABB o) {
        return maxX > o.minX - EPSILON && minX < o.maxX + EPSILON &&
                maxY > o.minY - EPSILON && minY < o.maxY + EPSILON &&
                maxZ > o.minZ - EPSILON && minZ < o.maxZ + EPSILON;
    }

    // X轴碰撞（仅基于AABB边界）
    public float collideX(AABB block, float moveX) {
        if (maxY <= block.minY || minY >= block.maxY) return moveX;
        if (maxZ <= block.minZ || minZ >= block.maxZ) return moveX;
        if (moveX > 0) {
            if (maxX <= block.minX + EPSILON) {
                return Math.min(moveX, block.minX - maxX - EPSILON);
            }
        } else if (moveX < 0) {
            if (minX >= block.maxX - EPSILON) {
                return Math.max(moveX, block.maxX - minX + EPSILON);
            }
        }
        return moveX;
    }

    // Y轴碰撞
    public float collideY(AABB block, float moveY) {
        if (maxX <= block.minX || minX >= block.maxX) return moveY;
        if (maxZ <= block.minZ || minZ >= block.maxZ) return moveY;
        if (moveY > 0) {
            if (maxY <= block.minY + EPSILON) {
                return Math.min(moveY, block.minY - maxY - EPSILON);
            }
        } else if (moveY < 0) {
            if (minY >= block.maxY - EPSILON) {
                return Math.max(moveY, block.maxY - minY + EPSILON);
            }
        }
        return moveY;
    }

    // Z轴碰撞
    public float collideZ(AABB block, float moveZ) {
        if (maxX <= block.minX || minX >= block.maxX) return moveZ;
        if (maxY <= block.minY || minY >= block.maxY) return moveZ;
        if (moveZ > 0) {
            if (maxZ <= block.minZ + EPSILON) {
                return Math.min(moveZ, block.minZ - maxZ - EPSILON);
            }
        } else if (moveZ < 0) {
            if (minZ >= block.maxZ - EPSILON) {
                return Math.max(moveZ, block.maxZ - minZ + EPSILON);
            }
        }
        return moveZ;
    }
}
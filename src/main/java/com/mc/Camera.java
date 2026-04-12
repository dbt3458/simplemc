package com.mc;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    public Vector3f position = new Vector3f(0, 40, 0);
    public Vector3f front = new Vector3f(0, 0, -1);
    public Vector3f up = new Vector3f(0, 1, 0);
    public float yaw = -90.0f;
    public float pitch = 0.0f;

    private static final float CAMERA_VIEW_HEIGHT = 1.15f;

    public Matrix4f getViewMatrix() {
        Vector3f cameraPos = new Vector3f(position);
        cameraPos.y += CAMERA_VIEW_HEIGHT;
        // 将相机向后偏移 0.1 米（沿视线反方向）
        cameraPos.sub(front.mul(0.1f, new Vector3f()));
        Vector3f target = new Vector3f(cameraPos).add(front);
        return new Matrix4f().lookAt(cameraPos, target, up);
    }

    public void rotate(float dx, float dy) {
        yaw += dx;
        pitch += dy;
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        front.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.normalize();
    }

    // 🔥 新增：直接获取标准 right 向量（永远正确）
    public Vector3f getRight() {
        return new Vector3f(front).cross(up).normalize();
    }
    public Vector3f getPosition() {
        return position;
    }
}
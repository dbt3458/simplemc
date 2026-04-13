package com.mc;
import java.util.ArrayList;
import static org.lwjgl.opengl.GL33.*;

public class ChunkMesh {
    private int vao = -1;
    private int vbo;
    private int vertexCount = 0;
    private final ArrayList<Float> vertices = new ArrayList<>();

    public void addTopFace(float x, float y, float z) {
        // 三角形1
        vertices.add(x-0.5f); vertices.add(y+0.5f); vertices.add(z-0.5f); vertices.add(0.0f); vertices.add(1.0f);
        vertices.add(x-0.5f); vertices.add(y+0.5f); vertices.add(z+0.5f); vertices.add(0.0f); vertices.add(0.0f);
        vertices.add(x+0.5f); vertices.add(y+0.5f); vertices.add(z-0.5f); vertices.add(1.0f); vertices.add(1.0f);
        // 三角形2
        vertices.add(x+0.5f); vertices.add(y+0.5f); vertices.add(z-0.5f); vertices.add(1.0f); vertices.add(1.0f);
        vertices.add(x-0.5f); vertices.add(y+0.5f); vertices.add(z+0.5f); vertices.add(0.0f); vertices.add(0.0f);
        vertices.add(x+0.5f); vertices.add(y+0.5f); vertices.add(z+0.5f); vertices.add(1.0f); vertices.add(0.0f);
    }

    // 🔹 底面（泥土专用）【已修复】
    public void addBottomFace(float x, float y, float z) {
        // 三角形1
        vertices.add(x-0.5f); vertices.add(y-0.5f); vertices.add(z-0.5f); vertices.add(0.0f); vertices.add(1.0f);
        vertices.add(x+0.5f); vertices.add(y-0.5f); vertices.add(z-0.5f); vertices.add(1.0f); vertices.add(1.0f);
        vertices.add(x-0.5f); vertices.add(y-0.5f); vertices.add(z+0.5f); vertices.add(0.0f); vertices.add(0.0f);
        // 三角形2
        vertices.add(x+0.5f); vertices.add(y-0.5f); vertices.add(z-0.5f); vertices.add(1.0f); vertices.add(1.0f);
        vertices.add(x+0.5f); vertices.add(y-0.5f); vertices.add(z+0.5f); vertices.add(1.0f); vertices.add(0.0f);
        vertices.add(x-0.5f); vertices.add(y-0.5f); vertices.add(z+0.5f); vertices.add(0.0f); vertices.add(0.0f);
    }

    // 🔹 北面前面 z+【已修复】
// 🔹 北面前面 z+【修复贴图倒置：仅翻转UV】
    public void addNorthFace(float x, float y, float z) {
        // 三角形1
        vertices.add(x-0.5f); vertices.add(y-0.5f); vertices.add(z+0.5f); vertices.add(0.0f); vertices.add(1.0f);
        vertices.add(x+0.5f); vertices.add(y-0.5f); vertices.add(z+0.5f); vertices.add(1.0f); vertices.add(1.0f);
        vertices.add(x-0.5f); vertices.add(y+0.5f); vertices.add(z+0.5f); vertices.add(0.0f); vertices.add(0.0f);
        // 三角形2
        vertices.add(x+0.5f); vertices.add(y-0.5f); vertices.add(z+0.5f); vertices.add(1.0f); vertices.add(1.0f);
        vertices.add(x+0.5f); vertices.add(y+0.5f); vertices.add(z+0.5f); vertices.add(1.0f); vertices.add(0.0f);
        vertices.add(x-0.5f); vertices.add(y+0.5f); vertices.add(z+0.5f); vertices.add(0.0f); vertices.add(0.0f);
    }

    // 🔹 南面后面 z-【修复贴图倒置：仅翻转UV】
    public void addSouthFace(float x, float y, float z) {
        // 三角形1
        vertices.add(x+0.5f); vertices.add(y-0.5f); vertices.add(z-0.5f); vertices.add(0.0f); vertices.add(1.0f);
        vertices.add(x-0.5f); vertices.add(y-0.5f); vertices.add(z-0.5f); vertices.add(1.0f); vertices.add(1.0f);
        vertices.add(x+0.5f); vertices.add(y+0.5f); vertices.add(z-0.5f); vertices.add(0.0f); vertices.add(0.0f);
        // 三角形2
        vertices.add(x-0.5f); vertices.add(y-0.5f); vertices.add(z-0.5f); vertices.add(1.0f); vertices.add(1.0f);
        vertices.add(x-0.5f); vertices.add(y+0.5f); vertices.add(z-0.5f); vertices.add(1.0f); vertices.add(0.0f);
        vertices.add(x+0.5f); vertices.add(y+0.5f); vertices.add(z-0.5f); vertices.add(0.0f); vertices.add(0.0f);
    }

    // 🔹 东面右面 x+【修复贴图倒置：仅翻转UV】
    public void addEastFace(float x, float y, float z) {
        // 三角形1
        vertices.add(x+0.5f); vertices.add(y-0.5f); vertices.add(z+0.5f); vertices.add(0.0f); vertices.add(1.0f);
        vertices.add(x+0.5f); vertices.add(y-0.5f); vertices.add(z-0.5f); vertices.add(1.0f); vertices.add(1.0f);
        vertices.add(x+0.5f); vertices.add(y+0.5f); vertices.add(z+0.5f); vertices.add(0.0f); vertices.add(0.0f);
        // 三角形2
        vertices.add(x+0.5f); vertices.add(y-0.5f); vertices.add(z-0.5f); vertices.add(1.0f); vertices.add(1.0f);
        vertices.add(x+0.5f); vertices.add(y+0.5f); vertices.add(z-0.5f); vertices.add(1.0f); vertices.add(0.0f);
        vertices.add(x+0.5f); vertices.add(y+0.5f); vertices.add(z+0.5f); vertices.add(0.0f); vertices.add(0.0f);
    }

    // 🔹 西面左面 x-【修复贴图倒置：仅翻转UV】
    public void addWestFace(float x, float y, float z) {
        // 三角形1
        vertices.add(x-0.5f); vertices.add(y-0.5f); vertices.add(z-0.5f); vertices.add(0.0f); vertices.add(1.0f);
        vertices.add(x-0.5f); vertices.add(y-0.5f); vertices.add(z+0.5f); vertices.add(1.0f); vertices.add(1.0f);
        vertices.add(x-0.5f); vertices.add(y+0.5f); vertices.add(z-0.5f); vertices.add(0.0f); vertices.add(0.0f);
        // 三角形2
        vertices.add(x-0.5f); vertices.add(y-0.5f); vertices.add(z+0.5f); vertices.add(1.0f); vertices.add(1.0f);
        vertices.add(x-0.5f); vertices.add(y+0.5f); vertices.add(z+0.5f); vertices.add(1.0f); vertices.add(0.0f);
        vertices.add(x-0.5f); vertices.add(y+0.5f); vertices.add(z-0.5f); vertices.add(0.0f); vertices.add(0.0f);
    }
    // 清空数据
    public void clear() {
        vertices.clear();
    }

    // 合并数据（安全）
    public void merge(ChunkMesh other) {
        vertices.addAll(other.vertices);
    }
    // 构建GPU网格（完全不变）
    public void build() {
        if (vertices.isEmpty()) return;
        float[] data = new float[vertices.size()];
        for (int i = 0; i < data.length; i++) data[i] = vertices.get(i);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        vertexCount = data.length / 5;
        vertices.clear();
    }
    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }
    // 渲染（完全不变）
    public void render(Texture texture) {
        if (vao == -1) return;
        texture.bind();
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
    }
}
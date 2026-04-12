package com.mc;

import static org.lwjgl.opengl.GL33.*;

public class CrosshairUI {
    // 准星参数（可直接调整）
    public float thickness = 3f;    // 粗细（像素）
    public float length = 30.0f;       // 长度（像素）
    public float centerGap = 1.0f;    // 中心空隙（像素）
    public float[] color = {0.78f, 0.78f, 0.78f, 0.8f};

    private int vao;
    private int vbo;
    private int shader;
    private final int windowWidth;
    private final int windowHeight;

    // 构造函数，传入窗口宽高
    public CrosshairUI(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        init();
    }

    // 初始化准星
    private void init() {
        // 加载准星着色器
        shader = loadShader("crosshair.vs", "crosshair.fs");

        // 生成顶点数据
        rebuildMesh();

        // 创建VAO/VBO
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, getVertices(), GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    // 生成准星顶点数据
    private float[] getVertices() {
        // 分别用宽高转换为归一化坐标，解决拉伸问题
        float halfThickX = thickness / (float) windowWidth;
        float halfThickY = thickness / (float) windowHeight;
        float halfLenX = length / (float) windowWidth;
        float halfLenY = length / (float) windowHeight;
        float halfGapX = centerGap / (float) windowWidth;
        float halfGapY = centerGap / (float) windowHeight;

        return new float[] {
                // 上矩形
                -halfThickX,  halfGapY,
                halfThickX,  halfGapY,
                halfThickX,  halfGapY + halfLenY,
                -halfThickX,  halfGapY,
                halfThickX,  halfGapY + halfLenY,
                -halfThickX,  halfGapY + halfLenY,

                // 下矩形
                -halfThickX, -halfGapY - halfLenY,
                halfThickX, -halfGapY - halfLenY,
                halfThickX, -halfGapY,
                -halfThickX, -halfGapY - halfLenY,
                halfThickX, -halfGapY,
                -halfThickX, -halfGapY,

                // 左矩形
                -halfGapX - halfLenX, -halfThickY,
                -halfGapX,           -halfThickY,
                -halfGapX,            halfThickY,
                -halfGapX - halfLenX, -halfThickY,
                -halfGapX,            halfThickY,
                -halfGapX - halfLenX,  halfThickY,

                // 右矩形
                halfGapX,           -halfThickY,
                halfGapX + halfLenX, -halfThickY,
                halfGapX + halfLenX,  halfThickY,
                halfGapX,           -halfThickY,
                halfGapX + halfLenX,  halfThickY,
                halfGapX,            halfThickY
        };
    }

    // 重新构建网格（窗口大小变化时调用）
    public void rebuildMesh() {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, getVertices(), GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    // 渲染准星
    public void render() {
        glDisable(GL_DEPTH_TEST);
        glUseProgram(shader);

        // 设置颜色
        glUniform3f(glGetUniformLocation(shader, "color"), color[0], color[1], color[2]);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 24);

        glBindVertexArray(0);
        glUseProgram(0);
        glEnable(GL_DEPTH_TEST);
    }

    // 加载着色器工具方法
    private int loadShader(String vertexPath, String fragmentPath) {
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);

        try {
            // 读取顶点着色器
            String vertexSource = new String(CrosshairUI.class.getResourceAsStream("/" + vertexPath).readAllBytes());
            glShaderSource(vertexShader, vertexSource);
            glCompileShader(vertexShader);

            // 读取片段着色器
            String fragmentSource = new String(CrosshairUI.class.getResourceAsStream("/" + fragmentPath).readAllBytes());
            glShaderSource(fragmentShader, fragmentSource);
            glCompileShader(fragmentShader);

            // 链接着色器程序
            int shaderProgram = glCreateProgram();
            glAttachShader(shaderProgram, vertexShader);
            glAttachShader(shaderProgram, fragmentShader);
            glLinkProgram(shaderProgram);

            // 删除着色器
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);

            return shaderProgram;
        } catch (Exception e) {
            throw new RuntimeException("加载准星着色器失败", e);
        }
    }

    // 清理资源
    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        glDeleteProgram(shader);
    }
}
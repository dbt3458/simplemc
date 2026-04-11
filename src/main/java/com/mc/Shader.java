package com.mc;

import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL33.*;

public class Shader {
    private int id;

    public Shader() {
        String vertex = "#version 330 core\n"
                + "layout (location = 0) in vec3 aPos;\n"
                + "layout (location = 1) in vec2 aTexCoord;\n"
                + "uniform mat4 model, view, projection;\n"
                + "out vec2 texCoord;\n"
                + "void main() {\n"
                + "    gl_Position = projection * view * model * vec4(aPos, 1.0);\n"
                + "    texCoord = aTexCoord;\n"
                + "}";

        String fragment = "#version 330 core\n"
                + "out vec4 FragColor;\n"
                + "in vec2 texCoord;\n"
                + "uniform sampler2D texture0;\n"
                + "void main() {\n"
                + "    FragColor = texture(texture0, texCoord);\n"
                + "}";

        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vertex);
        glCompileShader(vs);

        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fragment);
        glCompileShader(fs);

        id = glCreateProgram();
        glAttachShader(id, vs);
        glAttachShader(id, fs);
        glLinkProgram(id);

        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    public void use() {
        glUseProgram(id);
    }

    public void setMat4(String name, Matrix4f mat) {
        float[] f = new float[16];
        mat.get(f);
        glUniformMatrix4fv(glGetUniformLocation(id, name), false, f);
    }
}
package com.mc;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;

public class Texture {
    private final int id;
    public static Texture dirtTexture;
    public static Texture grassTopTexture;
    public static Texture grassSideTexture;
    public static Texture stoneTexture;
    public static void loadTextures()

    {
        dirtTexture = new Texture(Texture.class.getResourceAsStream("/dirt.png"));
        grassTopTexture = new Texture(Texture.class.getResourceAsStream("/grass_top.png"));
        grassSideTexture = new Texture(Texture.class.getResourceAsStream("/grass_side.png"));
        stoneTexture = new Texture(Texture.class.getResourceAsStream("/stone.png"));


    }


    // 🔥 新增：支持 Jar 包内读取的构造方法（核心）
    public Texture(InputStream inputStream) {
        int width, height;
        ByteBuffer buffer;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // 读取 InputStream 到 ByteBuffer
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer data = MemoryUtil.memAlloc(bytes.length);
            data.put(bytes);
            data.flip();

            // STB 从内存加载图片
            buffer = STBImage.stbi_load_from_memory(data, w, h, channels, 4);
            MemoryUtil.memFree(data);

            if (buffer == null) {
                throw new RuntimeException("图片加载失败: → " + STBImage.stbi_failure_reason());
            }

            width = w.get();
            height = h.get();
        }
        catch (Exception e) {
            throw new RuntimeException("读取材质流失败", e);
        }

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        STBImage.stbi_image_free(buffer);
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }
}
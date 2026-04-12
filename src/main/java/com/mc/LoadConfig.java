package com.mc;
import java.io.*;
import java.util.Properties;

public class LoadConfig {
    private static final Properties props = new Properties();

    // 静态块，类加载时自动执行一次配置加载
    static {
        load();
    }
    public static void generateDefaultConfig() {
        try {
            // 🔥 关键：获取 jar 所在的真实目录
            String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            File configFile = new File(jarFile.getParentFile(), "settings.cfg");

            if (!configFile.exists()) {
                FileWriter writer = new FileWriter(configFile);
                writer.write("render_distance=10\n");
                writer.write("chunk_cleanup_interval=15\n");
                writer.close();
                System.out.println("配置已生成: " + configFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void load() {
        // 1. 优先从 jar 包所在目录读取外部配置文件
        File externalFile = new File(getJarDirectory(), "settings.cfg");
        if (externalFile.exists()) {
            try (InputStream is = new FileInputStream(externalFile)) {
                props.load(is);
                System.out.println("[LoadConfig] 使用外部配置文件: " + externalFile.getAbsolutePath());
                return;
            } catch (IOException e) {
                System.err.println("[LoadConfig] 读取外部配置文件失败: " + e.getMessage());
                // 继续尝试内部
            }
        }

        // 2. 回退到 classpath 内部（即 jar 包内置的配置文件）
        try (InputStream is = LoadConfig.class.getResourceAsStream("/settings.cfg")) {
            if (is != null) {
                props.load(is);
                System.out.println("[LoadConfig] 使用内部配置文件");
                return;
            } else {
                System.out.println("[LoadConfig] 未找到内部配置文件，使用默认值");
            }
        } catch (IOException e) {
            System.err.println("[LoadConfig] 读取内部配置文件失败: " + e.getMessage());
        }
    }

    private static String getJarDirectory() {
        try {
            String path = LoadConfig.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            File file = new File(path);
            // 如果是 jar 文件，返回其所在目录；否则返回路径本身（开发环境可能为目录）
            return file.isFile() ? file.getParent() : path;
        } catch (Exception e) {
            return "."; // 默认当前目录
        }
    }

    // 对外提供获取渲染距离的方法
    public static int getRenderDistance() {
        String val = props.getProperty("render_distance", "6");
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            System.err.println("解析 render_distance 失败，使用默认值 6");
            return 6;
        }
    }

    // 对外提供获取区块清理间隔（秒）的方法
    public static int getChunkCleanupInterval() {
        String val = props.getProperty("chunk_cleanup_interval", "15");
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            System.err.println("解析 chunk_cleanup_interval 失败，使用默认值 15");
            return 15;
        }
    }
}
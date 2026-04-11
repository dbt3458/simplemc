package com.mc;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.util.Scanner;

public class World {
    private final Map<String, Chunk> chunks = new HashMap<>();
    private long lastCleanupTime = System.currentTimeMillis();
    private List<Chunk> chunksToCleanup = new ArrayList<>();
    public int cleanupIntervalSeconds = 10;

    public World() {
        loadCleanupInterval();
    }

    public void addChunkToCleanup(Chunk chunk) {
        chunksToCleanup.add(chunk);
    }

    public void update(int playerCX, int playerCZ, int dist) {
        // 1. 加载区块
        for (int dx = -dist; dx <= dist; dx++) {
            for (int dz = -dist; dz <= dist; dz++) {
                int cx = playerCX + dx;
                int cz = playerCZ + dz;
                String key = cx + "," + cz;
                chunks.computeIfAbsent(key, k -> new Chunk(cx, cz));
            }
        }

        // 2. 每帧构建几个网格，保证丝滑
        int built = 0;
        for (Chunk chunk : chunks.values()) {
            if (built >= 8) break;
            if (!chunk.isMeshBuilt()) {
                chunk.buildMesh();
                built++;
            }
        }

        // 3. 卸载远的区块（更快、不卡）
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, Chunk> entry : chunks.entrySet()) {
            Chunk chunk = entry.getValue();
            int dx = Math.abs(chunk.chunkX - playerCX);
            int dz = Math.abs(chunk.chunkZ - playerCZ);

            if (dx > dist || dz > dist) {
                toRemove.add(entry.getKey());
            }
        }

        for (String key : toRemove) {
            Chunk chunk = chunks.remove(key);
            addChunkToCleanup(chunk);
        }
    }

    public void loadCleanupInterval() {
        try {
            Scanner scanner = new Scanner(new File("settings.cfg"));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.startsWith("chunk_cleanup_interval=")) {
                    String val = line.split("=")[1].trim();
                    cleanupIntervalSeconds = Integer.parseInt(val);
                }
            }
            scanner.close();
        } catch (Exception e) {
            cleanupIntervalSeconds = 10;
        }
    }

    public void cleanupPendingChunks() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < cleanupIntervalSeconds * 1000L) {
            return;
        }

        for (Chunk c : chunksToCleanup) {
            c.cleanup();
        }
        chunksToCleanup.clear();
        lastCleanupTime = now;
    }

    public boolean hasBlock(float worldX, float worldY, float worldZ) {
        int cx = (int) Math.floor(worldX / 16.0f);
        int cz = (int) Math.floor(worldZ / 16.0f);
        Chunk chunk = chunks.get(cx + "," + cz);
        if (chunk == null)
            return false;

        // 🔥 用 Math.floor 确保坐标绝对准确
        int globalX = (int) Math.floor(worldX);
        int globalY = (int) Math.floor(worldY);
        int globalZ = (int) Math.floor(worldZ);

        int lx = globalX - cx * 16;
        int lz = globalZ - cz * 16;
        return lx >= 0 && lx < 16 && lz >= 0 && lz < 16 && chunk.hasBlock(lx, globalY, lz);
    }

    public Iterable<Chunk> getAllChunks() {
        return chunks.values();
    }
}
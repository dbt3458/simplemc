package com.mc;

public class Chunk {
    public static final int SIZE = 16;
    public int chunkX, chunkZ;
    private final ChunkMesh meshTop = new ChunkMesh();
    private final ChunkMesh meshSide = new ChunkMesh();
    private final ChunkMesh meshBottom = new ChunkMesh();
    private final boolean[][][] blockData = new boolean[SIZE][32][SIZE];

    // 标记是否已经构建过网格
    private boolean meshBuilt = false;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        generateBlocks();
    }

    private float smoothNoise(int x, int z) {
        float n = (float)(Math.sin(x * 0.1) + Math.cos(z * 0.1) + Math.sin(x * 0.05 + z * 0.05));
        return n * 1.5f;
    }

    private int height(int x, int z) {
        return 20 + (int)smoothNoise(x, z);
    }

    private void generateBlocks() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int wx = chunkX * SIZE + x;
                int wz = chunkZ * SIZE + z;
                int h = height(wx, wz);
                for (int y = 10; y <= h; y++) {
                    blockData[x][y][z] = true;
                }
            }
        }
    }

    // 单次构建，只在主线程调用
    public void buildMesh() {
        if (meshBuilt) return;

        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int h = height(chunkX * SIZE + x, chunkZ * SIZE + z);
                for (int y = 10; y <= h; y++) {
                    if (!blockData[x][y][z]) continue;
                    float wx = chunkX * SIZE + x;
                    float wy = y;
                    float wz = chunkZ * SIZE + z;

                    if (y == h) {
                        meshTop.addTopFace(wx, wy, wz);
                    }
                    meshBottom.addBottomFace(wx, wy, wz);
                    meshSide.addNorthFace(wx, wy, wz);
                    meshSide.addSouthFace(wx, wy, wz);
                    meshSide.addEastFace(wx, wy, wz);
                    meshSide.addWestFace(wx, wy, wz);
                }
            }
        }

        meshTop.build();
        meshSide.build();
        meshBottom.build();

        meshBuilt = true;
    }

    public boolean isMeshBuilt() {
        return meshBuilt;
    }

    public void cleanup() {
        meshTop.cleanup();
        meshSide.cleanup();
        meshBottom.cleanup();
        System.out.println("已经清理");
    }

    public void render() {
        if (!meshBuilt) return;

        meshTop.render(Main.grassTopTexture);
        meshSide.render(Main.grassSideTexture);
        meshBottom.render(Main.dirtTexture);
    }

    public boolean hasBlock(int x, int y, int z) {
        return y >= 10 && y < 32 && blockData[x][y][z];
    }
}
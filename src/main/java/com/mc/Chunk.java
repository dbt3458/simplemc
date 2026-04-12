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
    public void setBlock(int x, int y, int z, boolean exists) {
        if (x >= 0 && x < SIZE && y >= 10 && y < 32 && z >= 0 && z < SIZE) {
            System.out.println("setBlock: 局部("+x+","+y+","+z+") 存在="+exists+" 原值="+blockData[x][y][z]);
            blockData[x][y][z] = exists;
            rebuildMesh();
            System.out.println("rebuildMesh 调用完成");
        } else {
            System.out.println("setBlock 越界: "+x+","+y+","+z);
        }
    }
    public void rebuildMesh() {
        // 清理原有网格
        meshTop.cleanup();
        meshSide.cleanup();
        meshBottom.cleanup();

        // 重置网格数据
        meshTop.clear();
        meshSide.clear();
        meshBottom.clear();
        meshBuilt = false;

        // 重新构建
        buildMesh();
    }
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
    public void buildMesh() {
        if (meshBuilt) return;
        int faceCount = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = 10; y < 32; y++) {
                    if (!blockData[x][y][z]) continue;

                    float wx = chunkX * SIZE + x;
                    float wy = y;
                    float wz = chunkZ * SIZE + z;

                    // 顶面：上方无方块
                    if (y + 1 >= 32 || !blockData[x][y+1][z]) {
                        // 根据方块类型选择纹理（目前所有方块都是草/土混合，可简单用 meshTop）
                        meshTop.addTopFace(wx, wy, wz);
                    }
                    // 底面：下方无方块
                    if (y - 1 < 10 || !blockData[x][y-1][z]) {
                        meshBottom.addBottomFace(wx, wy, wz);
                    }
                    // 北面 (z+1)
                    if (z + 1 >= SIZE || !blockData[x][y][z+1]) {
                        meshSide.addNorthFace(wx, wy, wz);
                    }
                    // 南面 (z-1)
                    if (z - 1 < 0 || !blockData[x][y][z-1]) {
                        meshSide.addSouthFace(wx, wy, wz);
                    }
                    // 东面 (x+1)
                    if (x + 1 >= SIZE || !blockData[x+1][y][z]) {
                        meshSide.addEastFace(wx, wy, wz);
                    }
                    // 西面 (x-1)
                    if (x - 1 < 0 || !blockData[x-1][y][z]) {
                        meshSide.addWestFace(wx, wy, wz);
                    }
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
        meshTop.render(Texture.grassTopTexture);
        meshSide.render(Texture.grassSideTexture);
        meshBottom.render(Texture.dirtTexture);
    }

    public boolean hasBlock(int x, int y, int z) {
        return y >= 10 && y < 32 && blockData[x][y][z];
    }
}
package com.mc;

public class Chunk {
    public static final int SIZE = 16;
    public int chunkX, chunkZ;
    private final ChunkMesh meshTop = new ChunkMesh();
    private final ChunkMesh meshSide = new ChunkMesh();
    private final ChunkMesh meshBottom = new ChunkMesh();
    private final boolean[][][] blockData = new boolean[SIZE][256][SIZE];
    private boolean meshBuilt = false;
    private static final int SEA_LEVEL = 64;          // 海平面高度
    private static final int MAX_HEIGHT = 90;         // 世界最高高度限制
    private static final int WORLD_BOTTOM = 0;        // 世界最低高度
    private static final int CHUNK_HEIGHT = 256;
    private static SimplexNoise noiseGen;
    public static void initTerrainGenerator(long worldSeed) {
        noiseGen = new SimplexNoise(worldSeed);
    }
    private int getTerrainHeight(int blockX, int blockZ) {
        // 三层噪声叠加

        // 1. 大陆形状：低频、高振幅，决定宏观地貌
        double continentNoise = noiseGen.noise(blockX * 0.0025, blockZ * 0.0025, 0) * 1.2;

        // 2. 山脉起伏：中频、中振幅，增加高低变化
        double mountainNoise = noiseGen.noise(blockX * 0.012, blockZ * 0.012, 0) * 0.6;

        // 3. 地面细节：高频、低振幅，模拟小起伏
        double detailNoise = noiseGen.noise(blockX * 0.06, blockZ * 0.06, 0) * 0.15;

        // 噪声值范围：-1.2 ~ +1.2（大陆） + -0.6 ~ +0.6（山脉） + -0.15 ~ +0.15（细节）
        // 最终范围大约在 -1.95 到 +1.95 之间
        double finalNoise = continentNoise + mountainNoise + detailNoise;

        // 将噪声值映射到目标高度范围
        // 目标范围：海平面附近（~64）到最高点（MAX_HEIGHT），同时允许部分区域低于海平面
        double normalized = (finalNoise + 2.0) / 4.0;  // 将噪声范围从 [-2,2] 映射到 [0,1]
        // 将最终高度范围设定为 40 到 MAX_HEIGHT 之间，使大部分区域在40-90之间波动
        int height = (int)(40 + normalized * (MAX_HEIGHT - 40));

        // 限制高度不超过世界最高限制，且不低于世界最低限制
        return Math.min(MAX_HEIGHT, Math.max(WORLD_BOTTOM, height));
    }


    private void generateBlocks() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int wx = chunkX * SIZE + x;
                int wz = chunkZ * SIZE + z;
                int h = getTerrainHeight(wx, wz);
                for (int y = WORLD_BOTTOM; y <= h; y++) {
                    blockData[x][y][z] = true;
                }
            }
        }
    }

    // ========== 原有方法（已适配 Y 范围 0~256） ==========
    public void setBlock(int x, int y, int z, boolean exists) {
        if (x >= 0 && x < SIZE && y >= 0 && y < 256 && z >= 0 && z < SIZE) {
            System.out.println("setBlock: 局部("+x+","+y+","+z+") 存在="+exists+" 原值="+blockData[x][y][z]);
            blockData[x][y][z] = exists;
            rebuildMesh();
        }
    }

    public void rebuildMesh() {
        meshTop.cleanup();
        meshSide.cleanup();
        meshBottom.cleanup();
        meshTop.clear();
        meshSide.clear();
        meshBottom.clear();
        meshBuilt = false;
        buildMesh();
    }

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        generateBlocks();
    }

    public void buildMesh() {
        if (meshBuilt) return;
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = 0; y < 256; y++) {
                    if (!blockData[x][y][z]) continue;
                    float wx = chunkX * SIZE + x;
                    float wy = y;
                    float wz = chunkZ * SIZE + z;

                    if (y == 255 || !blockData[x][y+1][z])
                        meshTop.addTopFace(wx, wy, wz);
                    if (y == 0 || !blockData[x][y-1][z])
                        meshBottom.addBottomFace(wx, wy, wz);
                    if (z == SIZE-1 || !blockData[x][y][z+1])
                        meshSide.addNorthFace(wx, wy, wz);
                    if (z == 0 || !blockData[x][y][z-1])
                        meshSide.addSouthFace(wx, wy, wz);
                    if (x == SIZE-1 || !blockData[x+1][y][z])
                        meshSide.addEastFace(wx, wy, wz);
                    if (x == 0 || !blockData[x-1][y][z])
                        meshSide.addWestFace(wx, wy, wz);
                }
            }
        }
        meshTop.build();
        meshSide.build();
        meshBottom.build();
        meshBuilt = true;
    }

    public boolean isMeshBuilt() { return meshBuilt; }

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
        return y >= 0 && y < 256 && blockData[x][y][z];
    }
}
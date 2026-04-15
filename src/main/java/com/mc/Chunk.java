package com.mc;

public class Chunk {
    public static final int SIZE = 16;

    // 方块类型常量
    public static final byte BLOCK_AIR = 0;
    public static final byte BLOCK_GRASS = 1;
    public static final byte BLOCK_DIRT = 2;
    public static final byte BLOCK_STONE = 4;

    public int chunkX, chunkZ;
    private final byte[][][] blockData = new byte[SIZE][256][SIZE];
    private boolean meshBuilt = false;

    // 草方块的三个面网格
    private final ChunkMesh grassTop = new ChunkMesh();
    private final ChunkMesh grassSide = new ChunkMesh();
    private final ChunkMesh grassBottom = new ChunkMesh();
    // 泥土方块的三个面网格
    private final ChunkMesh dirtTop = new ChunkMesh();
    private final ChunkMesh dirtSide = new ChunkMesh();
    private final ChunkMesh dirtBottom = new ChunkMesh();

    private final ChunkMesh stoneTop = new ChunkMesh();
    private final ChunkMesh stoneSide = new ChunkMesh();
    private final ChunkMesh stoneBottom = new ChunkMesh();

    // ========== 地形生成（Simplex噪声） ==========
    private static SimplexNoise noiseGen;

    public static void initTerrainGenerator(long worldSeed) {
        noiseGen = new SimplexNoise(worldSeed);
    }

    private static final int MAX_HEIGHT = 90;
    private static final int WORLD_BOTTOM = 0;
    private static final int DIRT_LAYERS = 3;   // 草方块下面泥土层数

    private int getTerrainHeight(int blockX, int blockZ) {
        double continentNoise = noiseGen.noise(blockX * 0.0025, blockZ * 0.0025, 0) * 1.2;
        double mountainNoise = noiseGen.noise(blockX * 0.012, blockZ * 0.012, 0) * 0.6;
        double detailNoise = noiseGen.noise(blockX * 0.06, blockZ * 0.06, 0) * 0.15;
        double finalNoise = continentNoise + mountainNoise + detailNoise;
        double normalized = (finalNoise + 2.0) / 4.0;
        int height = (int)(40 + normalized * (MAX_HEIGHT - 40));
        return Math.min(MAX_HEIGHT, Math.max(WORLD_BOTTOM, height));
    }

    private void generateBlocks() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int wx = chunkX * SIZE + x;
                int wz = chunkZ * SIZE + z;
                int h = getTerrainHeight(wx, wz);
                for (int y = 0; y <= h; y++) {
                    if (y == h) {
                        blockData[x][y][z] = BLOCK_GRASS;
                    } else if (y >= h - DIRT_LAYERS) {
                        blockData[x][y][z] = BLOCK_DIRT;
                    } else {
                        // 更深处用泥土（可改为石头）
                        blockData[x][y][z] = BLOCK_STONE;
                    }
                }
            }
        }
    }

    // ========== 方块操作 ==========
    public void setBlock(int x, int y, int z, byte type) {
        if (x >= 0 && x < SIZE && y >= 0 && y < 256 && z >= 0 && z < SIZE) {
            if (blockData[x][y][z] != type) {
                blockData[x][y][z] = type;
                rebuildMesh();
            }
        }
    }

    public byte getBlock(int x, int y, int z) {
        if (x >= 0 && x < SIZE && y >= 0 && y < 256 && z >= 0 && z < SIZE) {
            return blockData[x][y][z];
        }
        return BLOCK_AIR;
    }

    public boolean hasBlock(int x, int y, int z) {
        return getBlock(x, y, z) != BLOCK_AIR;
    }

    // ========== 网格构建 ==========
    public void rebuildMesh() {
        // 清理所有网格
        grassTop.cleanup(); grassSide.cleanup(); grassBottom.cleanup();
        dirtTop.cleanup(); dirtSide.cleanup(); dirtBottom.cleanup();
        stoneTop.clear(); stoneSide.clear(); stoneBottom.clear();
        meshBuilt = false;
        buildMesh();
    }

    public void buildMesh() {
        if (meshBuilt) return;

        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = 0; y < 256; y++) {
                    byte type = blockData[x][y][z];
                    if (type == BLOCK_AIR) continue;

                    float wx = chunkX * SIZE + x;
                    float wy = y;
                    float wz = chunkZ * SIZE + z;

                    // 获取六个方向的邻居
                    byte up = (y == 255) ? BLOCK_AIR : blockData[x][y+1][z];
                    byte down = (y == 0) ? BLOCK_AIR : blockData[x][y-1][z];
                    byte north = (z == SIZE-1) ? BLOCK_AIR : blockData[x][y][z+1];
                    byte south = (z == 0) ? BLOCK_AIR : blockData[x][y][z-1];
                    byte east = (x == SIZE-1) ? BLOCK_AIR : blockData[x+1][y][z];
                    byte west = (x == 0) ? BLOCK_AIR : blockData[x-1][y][z];

                    if (type == BLOCK_GRASS) {
                        if (up == BLOCK_AIR) grassTop.addTopFace(wx, wy, wz);
                        if (down == BLOCK_AIR) grassBottom.addBottomFace(wx, wy, wz);
                        if (north == BLOCK_AIR) grassSide.addNorthFace(wx, wy, wz);
                        if (south == BLOCK_AIR) grassSide.addSouthFace(wx, wy, wz);
                        if (east == BLOCK_AIR) grassSide.addEastFace(wx, wy, wz);
                        if (west == BLOCK_AIR) grassSide.addWestFace(wx, wy, wz);
                    } else if (type == BLOCK_DIRT) {
                        if (up == BLOCK_AIR) dirtTop.addTopFace(wx, wy, wz);
                        if (down == BLOCK_AIR) dirtBottom.addBottomFace(wx, wy, wz);
                        if (north == BLOCK_AIR) dirtSide.addNorthFace(wx, wy, wz);
                        if (south == BLOCK_AIR) dirtSide.addSouthFace(wx, wy, wz);
                        if (east == BLOCK_AIR) dirtSide.addEastFace(wx, wy, wz);
                        if (west == BLOCK_AIR) dirtSide.addWestFace(wx, wy, wz);
                    }
                    else if (type == BLOCK_STONE) {
                        if (up == BLOCK_AIR) stoneTop.addTopFace(wx, wy, wz);
                        if (down == BLOCK_AIR) stoneBottom.addBottomFace(wx, wy, wz);
                        if (north == BLOCK_AIR) stoneSide.addNorthFace(wx, wy, wz);
                        if (south == BLOCK_AIR) stoneSide.addSouthFace(wx, wy, wz);
                        if (east == BLOCK_AIR) stoneSide.addEastFace(wx, wy, wz);
                        if (west == BLOCK_AIR) stoneSide.addWestFace(wx, wy, wz);
                    }
                }
            }
        }

        grassTop.build(); grassSide.build(); grassBottom.build();
        dirtTop.build(); dirtSide.build(); dirtBottom.build();
        stoneTop.build(); stoneSide.build(); stoneBottom.build();
        meshBuilt = true;
    }

    public boolean isMeshBuilt() { return meshBuilt; }

    public void cleanup() {
        grassTop.cleanup(); grassSide.cleanup(); grassBottom.cleanup();
        dirtTop.cleanup(); dirtSide.cleanup(); dirtBottom.cleanup();
        stoneTop.cleanup(); stoneSide.cleanup(); stoneBottom .cleanup();
        System.out.println("Chunk 已清理: " + chunkX + "," + chunkZ);
    }

    public void render() {
        if (!meshBuilt) return;
        // 草方块渲染
        grassTop.render(Texture.grassTopTexture);
        grassSide.render(Texture.grassSideTexture);
        grassBottom.render(Texture.dirtTexture);   // 草底部用泥土纹理
        // 泥土方块渲染
        dirtTop.render(Texture.dirtTexture);
        dirtSide.render(Texture.dirtTexture);
        dirtBottom.render(Texture.dirtTexture);
        stoneTop.render(Texture.stoneTexture);
        stoneSide.render(Texture.stoneTexture);
        stoneBottom.render(Texture.stoneTexture);

    }

    // ========== 构造器 ==========
    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        generateBlocks();
    }
}
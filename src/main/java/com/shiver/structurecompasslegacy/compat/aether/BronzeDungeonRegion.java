package com.shiver.structurecompasslegacy.compat.aether;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModMethod;
import com.shiver.structurecompasslegacy.simulation.ChunkGenerationBudget;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

/** 预测主房间与最小加载范围内可接出的侧室，保留邻近地牢影响的导航余量。 */
final class BronzeDungeonRegion {

    // 原生邻近地牢及房间乱序仍可能改变实际位置，返回区域而非精确入口。
    static final int SEARCH_MARGIN = 64;
    // 薄顶候选可能被地表水湖挖空；在房间上方多检查八格连续岩层。
    private static final int ROOF_MARGIN = 8;

    private final long seed;
    private final long mapSeedX;
    private final long mapSeedZ;
    private final long populateSeedX;
    private final long populateSeedZ;
    // 青铜、白银、黄金各自的 primary / secondary，创建任务时读取配置快照。
    private final int[] chances;
    private final ModConstructor<IChunkGenerator> factory;
    private final ModMethod foundation;
    private final ChunkGenerationBudget budget;
    private final Map<Long, BitSet> chunks = new LinkedHashMap<>(128, 0.75F, true);
    private IChunkGenerator terrain;

    BronzeDungeonRegion(long seed, int[] chances,
            ModConstructor<IChunkGenerator> factory, ModMethod foundation) {
        this.seed = seed;
        Random random = new Random(seed);
        mapSeedX = random.nextLong();
        mapSeedZ = random.nextLong();
        populateSeedX = mapSeedX / 2L * 2L + 1L;
        populateSeedZ = mapSeedZ / 2L * 2L + 1L;
        this.chances = chances;
        this.factory = factory;
        this.foundation = foundation;
        budget = new ChunkGenerationBudget(CompassConfig.cachedChunks, CompassConfig.generatedChunks);
    }

    BlockPos find(int chunkX, int chunkZ) {
        Candidate next = candidate(chunkX, chunkZ);
        if (next == null || !roomsFit(next)) {
            return null;
        }
        // 基础地形上至少能接出一个侧室；不要求周围还有另外两座候选。
        return new BlockPos(next.position.getX(), 64, next.position.getZ());
    }

    private Candidate candidate(int chunkX, int chunkZ) {
        // 天境使用方块坐标播种，与原版 populate 的区块坐标不同。
        Random random = new Random((chunkX * 16) * populateSeedX + (chunkZ * 16) * populateSeedZ ^ seed);
        int y = random.nextInt(48) + 24;
        random.nextInt(6); // BronzeDungeonSizeEvent 的默认房间数；不执行全局事件。
        if (!chance(random, chances[0], chances[1]) || hasPrecedingStructure(chunkX, chunkZ)) {
            return null;
        }

        // 原生两个 hollow box 的体积与六个面均消耗随机数，边角的重复消耗也保留。
        int draws = 16 * 12 * 16 + 2 * (16 * 16 + 16 * 12 + 16 * 12) + 4 * 4 * 4 + 6 * 4 * 4;
        for (int i = 0; i < draws; i++) {
            random.nextInt(20);
        }
        int side = random.nextInt(4);
        if (side != 0 && side != 2) {
            return null;
        }

        return new Candidate(new BlockPos(chunkX * 16 + 8, y + 2, chunkZ * 16 + 8), side);
    }

    private boolean roomsFit(Candidate candidate) {
        int x = candidate.position.getX() - 8;
        int y = candidate.position.getY() - 2;
        int z = candidate.position.getZ() - 8;
        if (!solid(x, y - 3, z, 16, 18 + ROOF_MARGIN, 16)) {
            return false;
        }
        // 只接纳当前、东、南、东南四个必需加载区块内就能容纳的三间房。
        return candidate.side == 0
                ? solid(x + 20, y, z + 2, 12, 12 + ROOF_MARGIN, 12)
                        && solid(x + 20, y, z + 18, 12, 8 + ROOF_MARGIN, 12)
                : solid(x + 2, y, z + 20, 12, 12 + ROOF_MARGIN, 12)
                        && solid(x + 18, y, z + 20, 12, 8 + ROOF_MARGIN, 12);
    }

    private boolean hasPrecedingStructure(int chunkX, int chunkZ) {
        // 三类原生前置结构的水平包围盒为起始块 +2 至 +102。
        // 排除与 populate 的 [chunk*16+8, chunk*16+23] 窗口相交的全部起点。
        for (int x = chunkX - 5; x <= chunkX + 1; x++) {
            for (int z = chunkZ - 5; z <= chunkZ + 1; z++) {
                if (mapRandom(x, z).nextInt(50) == 0
                        || x % 6 == 0 && z % 6 == 0 && chance(mapRandom(x, z), chances[2], chances[3])
                        || x % 10 == 0 && z % 10 == 0 && chance(mapRandom(x, z), chances[4], chances[5])) {
                    return true;
                }
            }
        }
        return false;
    }

    private Random mapRandom(int x, int z) {
        Random random = new Random((long) x * mapSeedX ^ (long) z * mapSeedZ ^ seed);
        random.nextInt(); // MapGenStructure.recursiveGenerate 的首次随机消耗。
        return random;
    }

    private static boolean chance(Random random, int primary, int secondary) {
        // RandomTracker 的重复值分支只在两次结果均非零时出现，候选在此已被排除。
        return random.nextInt(primary) == 0 || random.nextInt(secondary) == 0;
    }

    private boolean solid(int x, int y, int z, int width, int height, int depth) {
        for (int columnX = x; columnX < x + width; columnX++) {
            for (int columnZ = z; columnZ < z + depth; columnZ++) {
                BitSet blocks = chunk(columnX >> 4, columnZ >> 4);
                int first = ((columnX & 15) * 16 + (columnZ & 15)) * 128 + y;
                if (blocks.nextClearBit(first) < first + height) {
                    return false;
                }
            }
        }
        return true;
    }

    private BitSet chunk(int x, int z) {
        long key = ChunkPos.asLong(x, z);
        BitSet blocks = chunks.get(key);
        if (blocks != null) {
            return blocks;
        }
        if (chunks.size() >= CompassConfig.cachedChunks) {
            chunks.remove(chunks.keySet().iterator().next());
        }
        budget.begin(key, chunks.size());
        try {
            if (terrain == null) {
                // 已审计的构造器只保存 World 引用、创建私人噪声和生成器；基础噪声不访问 World。
                terrain = factory.create(null, seed);
            }
            ChunkPrimer primer = new ChunkPrimer();
            foundation.call(terrain, x, z, primer);
            blocks = new BitSet(16 * 16 * 128);
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localY = 0; localY < 128; localY++) {
                        if (primer.getBlockState(localX, localY, localZ).getBlock() != Blocks.AIR) {
                            blocks.set((localX * 16 + localZ) * 128 + localY);
                        }
                    }
                }
            }
            chunks.put(key, blocks);
            return blocks;
        } finally {
            budget.finish(key);
        }
    }

    int generatedChunks() {
        return budget.generated();
    }

    int peakCachedChunks() {
        return budget.peak();
    }

    void close() {
        terrain = null;
        chunks.clear();
    }

    private static final class Candidate {

        private final BlockPos position;
        private final int side;

        private Candidate(BlockPos position, int side) {
            this.position = position;
            this.side = side;
        }
    }
}

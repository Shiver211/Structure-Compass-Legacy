package com.shiver.structurecompasslegacy.compat.erebus;

import java.util.List;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.search.ChunkDistanceOrder;
import com.shiver.structurecompasslegacy.search.SearchLimitException;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;

import net.minecraft.block.BlockSand;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

/** 金字塔只检查群系；巨树额外验证基础地形和当前装饰窗口。 */
final class ErebusCandidateTask implements SearchTask {

    private final SearchContext context;
    private final ChunkDistanceOrder chunks;
    private final int biomeId;
    private final ErebusLocations locations;
    private final ErebusDecoration decoration;
    private GenLayer biomes;
    private List<BlockPos> known;
    private SimulationSession simulation;
    private BlockPos candidate;
    private int stage;
    private int generated;
    private int peakCached;
    private long longestCall;

    ErebusCandidateTask(SearchContext context, GenLayer biomes, int biomeId,
            ErebusLocations locations, ErebusDecoration decoration) {
        this.context = context;
        this.biomes = biomes;
        this.biomeId = biomeId;
        this.locations = locations;
        this.decoration = decoration;
        // 原生候选在触发区块的 (+16,+16)，偏移搜索原点以保持候选距离顺序。
        chunks = new ChunkDistanceOrder(context.origin.getX() - 8, context.origin.getZ() - 8, context.radius);
    }

    @Override
    public SearchStep advance(SearchBudget budget) {
        // 每次主线程切片刷新；缺少索引时不为每个候选重复访问磁盘。
        known = locations.read(context.source);
        while ((candidate != null || chunks.hasNext()) && !budget.exhausted()) {
            long before = System.nanoTime();
            SearchTarget target;
            try {
                target = step();
            } catch (SearchLimitException limit) {
                return SearchStep.limit(limit.getMessage());
            } finally {
                longestCall = Math.max(longestCall, System.nanoTime() - before);
            }
            if (target != null) {
                return SearchStep.found(target);
            }
            if (System.nanoTime() - before > CompassConfig.slowCallMillis * 1_000_000L) {
                return SearchStep.limit("slow_call");
            }
        }
        return candidate != null || chunks.hasNext() ? SearchStep.running() : SearchStep.exhausted();
    }

    private SearchTarget step() {
        if (candidate == null) {
            int chunkX = chunks.x(), chunkZ = chunks.z();
            chunks.next();
            int x = chunkX * 16 + 16, z = chunkZ * 16 + 16;
            BlockPos position = new BlockPos(x, 18, z);
            if (!context.contains(position)) {
                return null;
            }
            for (BlockPos recorded : known) {
                if (recorded.getX() == x && recorded.getZ() == z) {
                    return new SearchTarget(recorded);
                }
            }
            if (locations.blocked(known, x, z) || !matchesBiome(x, z)
                    || context.source.isChunkGeneratedAt(chunkX, chunkZ)) {
                return null;
            }
            if (decoration == null) {
                // 原生金字塔不检查高度、地形或随机概率，只额外检查四向 64 格的群系。
                return matchesBiome(x + 64, z) && matchesBiome(x - 64, z)
                        && matchesBiome(x, z + 64) && matchesBiome(x, z - 64)
                        ? new SearchTarget(position) : null;
            }
            candidate = position;
            stage = 0;
            return null;
        }

        // 真实索引/磁盘查询必须在模拟范围外；跨 tick 后重新确认候选尚未被自然生成排斥。
        if (locations.blocked(known, candidate.getX(), candidate.getZ())
                || context.source.isChunkGeneratedAt((candidate.getX() >> 4) - 1, (candidate.getZ() >> 4) - 1)) {
            finishCandidate();
            return null;
        }
        try (SimulationScope ignored = SimulationScope.enter()) {
            if (simulation == null) {
                simulation = new SimulationSession(context.source);
                return null;
            }
            if (generatedChunks() >= CompassConfig.generatedChunks) {
                throw new SearchLimitException("generated_chunks");
            }
            int x = candidate.getX(), z = candidate.getZ();
            if (stage == 0) {
                // 先排除原生空间检查必定失败的位置，减少昂贵的装饰复演。
                if (treePosition(simulation.world(), x, z) == null) {
                    finishCandidate();
                    return null;
                }
            } else if (stage <= 9) {
                // 红宝石生成会查询区块是否加载；预生成固定窗口后再调用装饰器。
                simulation.world().getChunk((x >> 4) - 1 + (stage - 1) / 3,
                        (z >> 4) - 1 + (stage - 1) % 3);
            } else if (stage == 10) {
                decoration.populate(simulation.world(), (x >> 4) - 1, (z >> 4) - 1);
            } else {
                BlockPos position = treePosition(simulation.world(), x, z);
                finishCandidate();
                return position == null ? null : new SearchTarget(position);
            }
            if (generatedChunks() > CompassConfig.generatedChunks) {
                throw new SearchLimitException("generated_chunks");
            }
            stage++;
            return null;
        }
    }

    private boolean matchesBiome(int x, int z) {
        IntCache.resetIntCache();
        return biomes.getInts(x, z, 1, 1)[0] == biomeId;
    }

    static BlockPos treePosition(World world, int x, int z) {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (int y = 16; y <= 80; y++) {
            IBlockState state = world.getBlockState(position.setPos(x, y, z));
            if (state != Blocks.GRASS.getDefaultState() && state != Blocks.DIRT.getDefaultState()
                    && state != Blocks.SAND.getDefaultState()
                    && state != Blocks.SAND.getDefaultState().withProperty(BlockSand.VARIANT, BlockSand.EnumType.RED_SAND)) {
                continue;
            }
            // 与 WorldGenGiantEucalyptus 一致：遇到第一个 MIXED 地表就停止，不能失败后改试更高地面。
            for (int xx = x - 14; xx <= x + 14; xx++) {
                for (int zz = z - 14; zz <= z + 14; zz++) {
                    for (int yy = y + 1; yy < y + 28; yy++) {
                        position.setPos(xx, yy, zz);
                        if (!world.getBlockState(position).getBlock().isReplaceable(world, position)) {
                            return null;
                        }
                    }
                }
            }
            return new BlockPos(x, y, z);
        }
        return null;
    }

    private void finishCandidate() {
        if (simulation != null) {
            generated += simulation.generatedChunks();
            peakCached = Math.max(peakCached, simulation.peakCachedChunks());
            simulation.close();
            simulation = null;
        }
        candidate = null;
    }

    @Override
    public long checkedCandidates() { return chunks.visited(); }

    @Override
    public long totalCandidates() { return chunks.total(); }

    @Override
    public int generatedChunks() { return generated + (simulation == null ? 0 : simulation.generatedChunks()); }

    @Override
    public int peakCachedChunks() { return Math.max(peakCached, simulation == null ? 0 : simulation.peakCachedChunks()); }

    @Override
    public long longestCallNanos() { return longestCall; }

    @Override
    public void close() {
        finishCandidate();
        biomes = null;
        known = null;
    }
}

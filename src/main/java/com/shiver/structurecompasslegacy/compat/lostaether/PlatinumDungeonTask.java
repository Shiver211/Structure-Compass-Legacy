package com.shiver.structurecompasslegacy.compat.lostaether;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.search.ChunkDistanceOrder;
import com.shiver.structurecompasslegacy.search.SearchLimitException;
import com.shiver.structurecompasslegacy.search.SeedMath;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeDecorator;

/** 按中心距离验证概率候选；以局部原生复演预测未加载区域。 */
final class PlatinumDungeonTask implements SearchTask {

    private final SearchContext context;
    private final ChunkDistanceOrder chunks;
    private final PlatinumPopulation population;
    private SimulationSession simulation;
    private BiomeDecorator decorator;
    private BlockPos candidate;
    private int stage;
    private int generated;
    private int peakCached;
    private long longestCall;

    PlatinumDungeonTask(SearchContext context, PlatinumPopulation population) {
        this.context = context;
        this.population = population;
        chunks = new ChunkDistanceOrder(context.origin.getX(), context.origin.getZ(), context.radius);
    }

    @Override
    public SearchStep advance(SearchBudget budget) {
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
            int x = chunks.x();
            int z = chunks.z();
            chunks.next();
            BlockPos center = new BlockPos(x * 16 + 8, 120, z * 16 + 8);
            // Forge 的触发区块在建筑中心东南方两个区块，使用独立的 Forge 随机源。
            if (context.contains(center) && SeedMath.forge(context.seed, x + 2, z + 2).nextInt(2000) == 0) {
                candidate = center;
                stage = 0;
            }
            return null;
        }
        try (SimulationScope ignored = SimulationScope.enter()) {
            if (simulation == null) {
                simulation = new SimulationSession(context.source);
                decorator = population.newDecorator();
                return null;
            }
            int x = candidate.getX() >> 4;
            int z = candidate.getZ() >> 4;
            if (stage < 25) {
                // 固定预生成 5×5 地形，供青铜房间的加载判断和云/树跨区块读写使用。
                if (generatedChunks() >= CompassConfig.generatedChunks) {
                    throw new SearchLimitException("generated_chunks");
                }
                simulation.world().getChunk(x - 2 + stage / 5, z - 2 + stage % 5);
            } else if (stage < 29) {
                int window = stage - 25;
                // 四个原生装饰窗口能覆盖中心；约定按 X、Z 递增复演。
                population.populate(simulation.world(), decorator, x - 1 + window / 2, z - 1 + window % 2);
                if (generatedChunks() > CompassConfig.generatedChunks) {
                    throw new SearchLimitException("generated_chunks");
                }
            } else {
                BlockPos anchor = new BlockPos(candidate.getX(), 120, candidate.getZ());
                boolean clear = simulation.world().getBlockState(anchor).getBlock() == Blocks.AIR;
                SearchTarget target = clear ? new SearchTarget(candidate) : null;
                finishCandidate();
                candidate = null;
                return target;
            }
            stage++;
            return null;
        }
    }

    private void finishCandidate() {
        if (simulation != null) {
            generated += simulation.generatedChunks();
            peakCached = Math.max(peakCached, simulation.peakCachedChunks());
            simulation.close();
            simulation = null;
            decorator = null;
        }
    }

    @Override
    public long checkedCandidates() {
        return chunks.visited();
    }

    @Override
    public long totalCandidates() {
        return chunks.total();
    }

    @Override
    public int generatedChunks() {
        return generated + (simulation == null ? 0 : simulation.generatedChunks());
    }

    @Override
    public int peakCachedChunks() {
        return Math.max(peakCached, simulation == null ? 0 : simulation.peakCachedChunks());
    }

    @Override
    public long longestCallNanos() {
        return longestCall;
    }
    
    @Override
    public void close() {
        finishCandidate();
    }
}

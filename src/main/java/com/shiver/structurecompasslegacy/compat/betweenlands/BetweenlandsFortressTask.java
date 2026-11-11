package com.shiver.structurecompasslegacy.compat.betweenlands;

import com.shiver.structurecompasslegacy.CompassConfig;
import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.SearchTask;
import com.shiver.structurecompasslegacy.search.ChunkSpiral;
import com.shiver.structurecompasslegacy.search.SearchLimitException;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;

import net.minecraft.world.gen.layer.GenLayer;

final class BetweenlandsFortressTask implements SearchTask {

    private static final int TERRAIN_CHUNKS = 4;

    private final SearchContext context;
    private final ChunkSpiral chunks;
    private final int marsh0;
    private final int marsh1;
    private GenLayer biomes;
    private SimulationSession simulation;
    private BetweenlandsTerrain terrain;
    private BetweenlandsPopulation population;
    private int chunkX;
    private int chunkZ;
    private int stage = -1;
    private long longest;

    BetweenlandsFortressTask(SearchContext context, GenLayer biomes, int marsh0, int marsh1) {
        this.context = context;
        this.biomes = biomes;
        this.marsh0 = marsh0;
        this.marsh1 = marsh1;
        chunks = new ChunkSpiral(context.origin.getX() >> 4, context.origin.getZ() >> 4, (context.radius + 63) / 16);
    }

    @Override
    public SearchStep advance(SearchBudget budget) {
        while ((stage >= 0 || chunks.hasNext()) && !budget.exhausted()) {
            long before = System.nanoTime();
            SearchTarget target;
            try {
                target = step();
            } catch (SearchLimitException limit) {
                return SearchStep.limit(limit.getMessage());
            } finally {
                longest = Math.max(longest, System.nanoTime() - before);
            }
            if (target != null) {
                // 真实区块状态只在模拟作用域外查询；跨 tick 后再次确认触发区块。
                if (!context.source.isChunkGeneratedAt(chunkX, chunkZ)) {
                    return SearchStep.found(target);
                }
            }
            if (System.nanoTime() - before > CompassConfig.slowCallMillis * 1_000_000L) {
                return SearchStep.limit("slow_call");
            }
        }
        return stage >= 0 || chunks.hasNext() ? SearchStep.running() : SearchStep.exhausted();
    }

    private SearchTarget step() {
        if (stage < 0) {
            chunkX = chunks.x();
            chunkZ = chunks.z();
            chunks.next();
            int biome = biomes.getInts(chunkX * 16 + 16, chunkZ * 16 + 16, 1, 1)[0];
            if ((biome != marsh0 && biome != marsh1) || context.source.isChunkGeneratedAt(chunkX, chunkZ)) {
                return null;
            }
            stage = 0;
            return null;
        }
        try (SimulationScope ignored = SimulationScope.enter()) {
            if (simulation == null) {
                simulation = new SimulationSession(context.source);
                terrain = (BetweenlandsTerrain) simulation.world().simulationChunks().chunkGenerator;
                population = new BetweenlandsPopulation();
                return null;
            }
            if (stage == 0) {
                simulation.beginCandidate();
                terrain.beginCandidate();
            }
            if (stage < TERRAIN_CHUNKS) {
                // 原生装饰窗口先具备 2×2 基础地形；之后按原生查询补临时邻区块。
                simulation.world().getChunk(chunkX + stage / 2, chunkZ + stage % 2);
            } else if (stage == TERRAIN_CHUNKS) {
                terrain.run(() -> population.begin(terrain, chunkX, chunkZ));
            } else {
                boolean[] done = new boolean[1];
                terrain.run(() -> done[0] = population.step());
                if (done[0]) {
                    SearchTarget target = terrain.fortress != null && context.contains(terrain.fortress)
                            ? new SearchTarget(terrain.fortress) : null;
                    stage = -1;
                    simulation.beginCandidate();
                    terrain.beginCandidate();
                    return target;
                }
                return null;
            }
            stage++;
            return null;
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
    public long longestCallNanos() {
        return longest;
    }

    @Override
    public int generatedChunks() {
        return simulation == null ? 0 : simulation.generatedChunks();
    }

    @Override
    public int peakCachedChunks() {
        return simulation == null ? 0 : simulation.peakCachedChunks();
    }

    @Override
    public void close() {
        if (simulation != null) {
            simulation.close();
        }
        simulation = null;
        terrain = null;
        population = null;
        biomes = null;
    }
}

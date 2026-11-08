package com.shiver.structurecompasslegacy.compat.betweenlands;

import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;

final class BetweenlandsLandmarkTask extends ChunkSearchTask {

    enum Landmark {
        FLOATING_ISLAND,
        GIANT_ROOT
    }

    private final Landmark landmark;
    private final BetweenlandsPlacement placement;
    private final Biome raisedIsles;
    private final Biome coarseIslands;
    private GenLayer biomes;

    BetweenlandsLandmarkTask(SearchContext context, Landmark landmark, GenLayer biomes,
            Biome raisedIsles, Biome coarseIslands) {
        super(context);
        this.landmark = landmark;
        this.biomes = biomes;
        this.raisedIsles = raisedIsles;
        this.coarseIslands = coarseIslands;
        placement = new BetweenlandsPlacement(context.seed);
    }

    @Override
    public SearchStep advance(SearchBudget budget) throws Exception {
        return biomes == null ? SearchStep.error("betweenlands_biome_layers") : super.advance(budget);
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        BlockPos position;
        if (landmark == Landmark.FLOATING_ISLAND) {
            position = placement.floatingIsland(chunkX, chunkZ, pos ->
                    Biome.getBiome(biomes.getInts(pos.getX(), pos.getZ(), 1, 1)[0]) == raisedIsles);
        } else {
            // 四个起点共用一次群系查询；数组仅在本候选内使用，不缓存区块或坐标。
            int[] area = biomes.getInts(chunkX * 16, chunkZ * 16, 9, 25);
            position = placement.giantRoot(chunkX, chunkZ, pos -> {
                int index = (pos.getZ() - chunkZ * 16) * 9 + pos.getX() - chunkX * 16;
                Biome biome = Biome.getBiome(area[index]);
                return biome == raisedIsles || biome == coarseIslands;
            });
        }
        return position == null ? null : new SearchTarget(position);
    }

    @Override
    public void close() {
        biomes = null;
    }
}

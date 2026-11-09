package com.shiver.structurecompasslegacy.compat.betweenlands;

import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchStep;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.layer.GenLayer;

final class BetweenlandsDungeonTask extends ChunkSearchTask {

    private final int clearingId;
    private GenLayer biomes;

    BetweenlandsDungeonTask(SearchContext context, GenLayer biomes, int clearingId) {
        super(context);
        this.biomes = biomes;
        this.clearingId = clearingId;
    }

    @Override
    public SearchStep advance(SearchBudget budget) throws Exception {
        return biomes == null ? SearchStep.limit("betweenlands_biome_layers") : super.advance(budget);
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        BlockPos position = BetweenlandsDungeonRegion.find(biomes, clearingId, chunkX, chunkZ);
        return position == null ? null : new SearchTarget(position);
    }

    @Override
    public void close() {
        biomes = null;
    }
}

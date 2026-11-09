package com.shiver.structurecompasslegacy.compat.betweenlands;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.layer.GenLayer;

final class BetweenlandsDungeonTask extends ChunkSearchTask {

    private BetweenlandsDungeonRegion regions;

    BetweenlandsDungeonTask(SearchContext context, GenLayer biomes, int clearingId) {
        super(context);
        regions = new BetweenlandsDungeonRegion(biomes, clearingId);
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        BlockPos position = regions.find(chunkX, chunkZ);
        return position == null ? null : new SearchTarget(position);
    }

    @Override
    public void close() {
        regions = null;
    }
}

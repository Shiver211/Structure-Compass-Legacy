package com.shiver.structurecompasslegacy.compat.aether;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModMethod;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.StructureBoundingBox;

final class BronzeDungeonTask extends ChunkSearchTask {

    private final BronzeDungeonRegion regions;

    BronzeDungeonTask(SearchContext context, int[] chances,
            ModConstructor<IChunkGenerator> terrain, ModMethod foundation) {
        super(context);
        regions = new BronzeDungeonRegion(context.seed, chances, terrain, foundation);
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        BlockPos point = regions.find(chunkX, chunkZ);
        if (point == null) {
            return null;
        }
        int margin = BronzeDungeonRegion.SEARCH_MARGIN;
        return new SearchTarget(point, new StructureBoundingBox(
                point.getX() - margin, 24, point.getZ() - margin,
                point.getX() + margin, 96, point.getZ() + margin));
    }

    @Override
    public int generatedChunks() {
        return regions.generatedChunks();
    }

    @Override
    public int peakCachedChunks() {
        return regions.peakCachedChunks();
    }

    @Override
    public void close() {
        regions.close();
    }
}

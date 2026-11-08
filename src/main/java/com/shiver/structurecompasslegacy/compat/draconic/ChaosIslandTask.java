package com.shiver.structurecompasslegacy.compat.draconic;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;

import net.minecraft.util.math.BlockPos;

final class ChaosIslandTask extends ChunkSearchTask {

    private final int separation;
    private final int height;

    ChaosIslandTask(SearchContext context, int separation, int height) {
        super(context);
        this.separation = separation;
        this.height = height;
        if (separation < 16) {
            throw new IllegalArgumentException("Chaos island separation must be at least one chunk");
        }
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        int x = multipleInChunk(chunkX, separation);
        int z = multipleInChunk(chunkZ, separation);
        if ((x >> 4) != chunkX || (z >> 4) != chunkZ || (x == 0 && z == 0)) {
            return null;
        }

        return new SearchTarget(new BlockPos(x, height, z));
    }

    static int multipleInChunk(int chunk, int separation) {
        return Math.floorDiv(chunk * 16 + 15, separation) * separation;
    }
}

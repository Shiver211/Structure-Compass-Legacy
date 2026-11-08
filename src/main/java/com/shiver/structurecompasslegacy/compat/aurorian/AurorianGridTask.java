package com.shiver.structurecompasslegacy.compat.aurorian;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;

import net.minecraft.util.math.BlockPos;

final class AurorianGridTask extends ChunkSearchTask {

    private final int spacing;
    private final int offset;
    private final int height;

    AurorianGridTask(SearchContext context, int spacing, int offset, int height) {
        super(context);
        if (spacing <= 0) {
            throw new IllegalArgumentException("Aurorian structure spacing must be positive");
        }
        this.spacing = spacing;
        this.offset = offset;
        this.height = height;
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        if ((chunkX + offset) % spacing != 0 || (chunkZ + offset) % spacing != 0) {
            return null;
        }
        return new SearchTarget(new BlockPos(chunkX * 16 + 8, height, chunkZ * 16 + 8));
    }
}

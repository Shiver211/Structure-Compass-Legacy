package com.shiver.structurecompasslegacy.compat.divinerpg;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;

final class DivineStructureTask extends ChunkSearchTask {

    private final DivineStructure structure;
    private final DivinePlacement placement;

    DivineStructureTask(SearchContext context, DivineStructure structure) {
        super(context);
        this.structure = structure;
        placement = new DivinePlacement(context.seed);
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        if (!placement.canSpawn(structure, chunkX, chunkZ)) {
            return null;
        }

        SearchTarget target = placement.layout(structure, chunkX, chunkZ);
        return placement.overlapsNeighbour(structure, chunkX, chunkZ, target.bounds) ? null : target;
    }
}

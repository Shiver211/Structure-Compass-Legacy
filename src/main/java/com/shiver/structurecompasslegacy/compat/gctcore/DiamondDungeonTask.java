package com.shiver.structurecompasslegacy.compat.gctcore;

import java.util.Random;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;
import com.shiver.structurecompasslegacy.search.ChunkSpiral;

import net.minecraft.util.math.BlockPos;

final class DiamondDungeonTask extends ChunkSearchTask {

    private static final int GRID_CHUNKS = 10;
    private static final int GRID_BLOCKS = GRID_CHUNKS * 16;
    private final int primaryChance;
    private final int secondaryChance;

    DiamondDungeonTask(SearchContext context, int primaryChance, int secondaryChance) {
        super(context, new ChunkSpiral(
                nearestGrid(context.origin.getX()), nearestGrid(context.origin.getZ()),
                (context.radius + GRID_BLOCKS - 1) / GRID_BLOCKS));
        this.primaryChance = Math.max(1, primaryChance);
        this.secondaryChance = Math.max(1, secondaryChance);
    }

    private static int nearestGrid(int coordinate) {
        return Math.floorDiv(coordinate - 8 + GRID_BLOCKS / 2, GRID_BLOCKS);
    }

    @Override
    protected SearchTarget inspect(int gridX, int gridZ) {
        int chunkX = gridX * GRID_CHUNKS;
        int chunkZ = gridZ * GRID_CHUNKS;
        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;
        if (Math.abs(x) < 3000 || Math.abs(z) < 3000) {
            return null;
        }

        // Matches GCT-Core's DiamondDungeonMapGen.canSpawnStructureAtCoords.
        long seed = context.seed ^ 0xD1B54A32D192ED03L ^ (chunkX * 341873128712L)
                ^ (chunkZ * 132897987541L) ^ (context.dimension * 0x9E3779B97F4A7C15L);
        Random random = new Random(seed);
        return random.nextInt(primaryChance) == 0 || random.nextInt(secondaryChance) == 0
                ? new SearchTarget(new BlockPos(x, 100, z)) : null;
    }
}

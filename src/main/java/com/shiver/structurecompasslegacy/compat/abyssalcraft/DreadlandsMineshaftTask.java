package com.shiver.structurecompasslegacy.compat.abyssalcraft;

import java.util.Random;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;
import com.shiver.structurecompasslegacy.search.SeedMath;

import net.minecraft.util.math.BlockPos;

final class DreadlandsMineshaftTask extends ChunkSearchTask {

    DreadlandsMineshaftTask(SearchContext context) {
        super(context);
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        if (!canSpawn(context.seed, chunkX, chunkZ)) {
            return null;
        }
        return new SearchTarget(new BlockPos(chunkX * 16 + 8, 64, chunkZ * 16 + 8));
    }

    static boolean canSpawn(long seed, int chunkX, int chunkZ) {
        Random random = SeedMath.mapGen(seed, chunkX, chunkZ);
        return random.nextDouble() < 0.004
                && random.nextInt(80) < Math.max(Math.abs(chunkX), Math.abs(chunkZ));
    }
}

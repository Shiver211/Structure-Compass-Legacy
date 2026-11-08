package com.shiver.structurecompasslegacy.compat.draconic;

import java.util.Random;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;
import com.shiver.structurecompasslegacy.search.SeedMath;

import net.minecraft.util.math.BlockPos;

final class EnderCometTask extends ChunkSearchTask {

    private final int rarity;

    EnderCometTask(SearchContext context, int rarity) {
        super(context);
        this.rarity = Math.max(1, rarity);
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        BlockPos position = predict(context.seed, chunkX, chunkZ, rarity);
        return position == null ? null : new SearchTarget(position);
    }

    static BlockPos predict(long seed, int chunkX, int chunkZ, int rarity) {
        Random random = SeedMath.forge(seed, chunkX, chunkZ);
        random.nextInt(16);
        int y = 20 + random.nextInt(170);
        random.nextInt(16);
        int x = chunkX * 16;
        int z = chunkZ * 16;

        // 当前分支在转换为 double 之前进行 int 乘法，保留远坐标下的实际行为。
        double distance = Math.sqrt(x * x + z * z);
        if (!(distance > 200.0D)) {
            return null;
        }
        if (random.nextInt(rarity) != 0) {
            return null;
        }

        // 彗星核心无地形放置条件；不需要调用会改动 Chunk.populating 的原生成器。
        return new BlockPos(x + 8, y, z + 8);
    }
}

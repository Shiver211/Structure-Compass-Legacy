package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import net.minecraft.util.math.BlockPos;

/** 3.9.6 的基础地形地标；不执行装饰、不访问区域存储。 */
final class BetweenlandsPlacement {

    private final long seed;
    private final long seedX;
    private final long seedZ;
    private final Random random = new Random();

    BetweenlandsPlacement(long seed) {
        this.seed = seed;
        random.setSeed(seed);
        seedX = random.nextLong();
        seedZ = random.nextLong();
    }

    BlockPos floatingIsland(int chunkX, int chunkZ, Predicate<BlockPos> raisedIsles) {
        beginChunk(chunkX, chunkZ);
        if (random.nextInt(80) != 0) {
            return null;
        }

        BlockPos column = new BlockPos(chunkX * 16 + random.nextInt(16), 64,
                chunkZ * 16 + random.nextInt(16));
        return raisedIsles.test(column) ? new BlockPos(column.getX(), 145 + random.nextInt(40), column.getZ()) : null;
    }

    BlockPos giantRoot(int chunkX, int chunkZ, Predicate<BlockPos> rootBiome) {
        beginChunk(chunkX, chunkZ);
        List<BlockPos> starts = new ArrayList<>();
        for (int sample = 0; sample < 4; sample++) {
            // 原生循环没有重置局部 Z，实际采样为 (0,0)、(0,8)、(8,16)、(8,24)。
            BlockPos column = new BlockPos(chunkX * 16 + sample / 2 * 8, 64, chunkZ * 16 + sample * 8);
            if (rootBiome.test(column) && random.nextInt(28) == 0) {
                starts.add(new BlockPos(column.getX(), 114, column.getZ()));
            }
        }

        Collections.shuffle(starts, random);
        for (BlockPos start : starts) {
            if (hasEndpoint(start)) {
                // 返回原生列表中的第一条根。之后的宽度、拱高和枝叶随机不改变其存在与起点。
                return start;
            }
        }
        return null;
    }

    private boolean hasEndpoint(BlockPos start) {
        List<BlockPos> ends = new ArrayList<>();
        int chunkX = start.getX() >> 4;
        int chunkZ = start.getZ() >> 4;
        for (int x = chunkX - 2; x <= chunkX + 2; x++) {
            for (int z = chunkZ - 2; z <= chunkZ + 2; z++) {
                for (int sample = 0; sample < 16; sample++) {
                    if (random.nextInt(100) == 0) {
                        // 终点的 4×4 循环同样不重置局部 Z，必须保留这一坐标规则。
                        ends.add(new BlockPos(x * 16 + sample / 4 * 4, 114, z * 16 + sample * 4));
                    }
                }
            }
        }

        Collections.shuffle(ends, random);
        for (BlockPos end : ends) {
            if (end.distanceSq(start) >= 1600.0) {
                return true;
            }
        }
        return false;
    }

    private void beginChunk(int x, int z) {
        // 普通 MapGenBase 没有 MapGenStructure 的额外 nextInt()。
        random.setSeed((long) x * seedX ^ (long) z * seedZ ^ seed);
    }
}

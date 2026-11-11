package com.shiver.structurecompasslegacy.compat.divinerpg;

import com.shiver.structurecompasslegacy.api.SearchTarget;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

final class DivineAltarLoaded {

    private DivineAltarLoaded() {
    }

    static boolean ready(World world, SearchTarget layout, int chunkX, int chunkZ) {
        if (!populated(world, chunkX, chunkZ)) {
            return false;
        }
        for (int x = layout.bounds.minX >> 4; x <= layout.bounds.maxX >> 4; x++) {
            for (int z = layout.bounds.minZ >> 4; z <= layout.bounds.maxZ >> 4; z++) {
                if (!populated(world, x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean populated(World world, int x, int z) {
        Chunk chunk = world.getChunkProvider().getLoadedChunk(x, z);
        return chunk != null && chunk.isTerrainPopulated();
    }

    static SearchTarget find(World world, DivineAltar altar, SearchTarget layout, Block spawner) {
        BlockPos column = layout.position;
        Chunk chunk = world.getChunkProvider().getLoadedChunk(column.getX() >> 4, column.getZ() >> 4);
        // 水平位置由原生随机与旋转决定，高度及是否成功以现有方块为准。
        for (int y = 255; y >= 0; y--) {
            if (chunk.getBlockState(column.getX(), y, column.getZ()).getBlock() == spawner) {
                int height = y - altar.spawner.getY();
                layout.bounds.offset(0, height, 0);
                return new SearchTarget(new BlockPos(column.getX(), y, column.getZ()), layout.bounds);
            }
        }
        return null;
    }
}

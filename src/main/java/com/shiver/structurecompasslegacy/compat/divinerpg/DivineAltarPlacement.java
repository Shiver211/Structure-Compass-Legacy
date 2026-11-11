package com.shiver.structurecompasslegacy.compat.divinerpg;

import java.util.Random;

import com.shiver.structurecompasslegacy.api.SearchTarget;

import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

final class DivineAltarPlacement {

    private final long seed;
    private final long seedX;
    private final long seedZ;

    DivineAltarPlacement(long seed) {
        this.seed = seed;
        Random random = new Random(seed);
        seedX = random.nextLong() / 2L * 2L + 1L;
        seedZ = random.nextLong() / 2L * 2L + 1L;
    }

    SearchTarget layout(DivineAltar altar, int chunkX, int chunkZ) {
        Random random = new Random((chunkX * seedX + chunkZ * seedZ) ^ seed);
        if (random.nextInt(60) != 0) {
            return null;
        }

        int x = chunkX * 16 + 1 + random.nextInt(16);
        int z = chunkZ * 16 + 1 + random.nextInt(16);
        // 草方块检查不消耗随机数，成功后的下一次随机调用就是模板旋转。
        Rotation rotation = Rotation.values()[random.nextInt(4)];
        BlockPos origin = new BlockPos(x, 0, z);
        switch (rotation) {
            case CLOCKWISE_90:
                origin = origin.add(altar.size.getZ() - 1, 0, 0);
                break;
            case CLOCKWISE_180:
                origin = origin.add(altar.size.getX() - 1, 0, altar.size.getZ() - 1);
                break;
            case COUNTERCLOCKWISE_90:
                origin = origin.add(0, 0, altar.size.getX() - 1);
                break;
            default:
                break;
        }

        // DRPGStructureHandler 先平移旋转原点，旋转后仍占据正向的 16×16 范围。
        StructureBoundingBox bounds = new StructureBoundingBox(
                x, 0, z, x + altar.size.getX() - 1, altar.size.getY() - 1, z + altar.size.getZ() - 1);
        return new SearchTarget(origin.add(altar.spawner.rotate(rotation)), bounds);
    }
}

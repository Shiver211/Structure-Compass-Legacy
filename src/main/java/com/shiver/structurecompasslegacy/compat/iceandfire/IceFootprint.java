package com.shiver.structurecompasslegacy.compat.iceandfire;

import java.util.function.Predicate;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

final class IceFootprint {
    private IceFootprint() {
    }

    static boolean clearTomb(BlockPos ground, EnumFacing face, BlockPos size, Predicate<BlockPos> dread) {
        int x = size.getX() / 2, z = size.getZ() / 2;
        BlockPos corner = ground.offset(face, z).offset(face.rotateYCCW(), x);
        return !dread.test(corner.offset(face, z)) && !dread.test(corner.offset(face, -z))
            && !dread.test(corner.offset(face.rotateY(), x)) && !dread.test(corner.offset(face.rotateYCCW(), x));
    }
}

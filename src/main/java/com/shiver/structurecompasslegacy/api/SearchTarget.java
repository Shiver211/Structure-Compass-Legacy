package com.shiver.structurecompasslegacy.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public final class SearchTarget {

    public final BlockPos position;
    public final StructureBoundingBox bounds;

    public SearchTarget(BlockPos position) {
        this(position, null);
    }

    public SearchTarget(BlockPos position, StructureBoundingBox bounds) {
        this.position = position.toImmutable();
        this.bounds = bounds;
    }
}

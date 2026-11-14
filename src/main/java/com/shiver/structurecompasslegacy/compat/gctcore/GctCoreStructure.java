package com.shiver.structurecompasslegacy.compat.gctcore;

import com.shiver.structurecompasslegacy.api.SearchTarget;

import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;

enum GctCoreStructure {

    CRIMSON_TEMPLE("crimson_temple", 41, 32, 19, 33),
    SHADOWBERRY_CAVE_MINOR("shadowberry_cave_minor", 42, 11, 11, 11),
    SHADOWBERRY_CAVE_BIGGER("shadowberry_cave_bigger", 42, 21, 21, 21);

    final String id;
    final int dimension;
    final boolean cave;
    final int targetMargin;
    private final BlockPos lastBlock;
    private final BlockPos center;

    GctCoreStructure(String id, int dimension, int sizeX, int sizeY, int sizeZ) {
        this.id = id;
        this.dimension = dimension;
        cave = dimension == 42;
        lastBlock = new BlockPos(sizeX - 1, sizeY - 1, sizeZ - 1);
        center = new BlockPos((sizeX - 1) / 2, (sizeY - 1) / 2, (sizeZ - 1) / 2);
        targetMargin = (int) Math.ceil(Math.hypot(center.getX() + 8, center.getZ() + 8));
    }

    SearchTarget target(BlockPos origin, Rotation rotation) {
        PlacementSettings settings = new PlacementSettings().setRotation(rotation);
        BlockPos opposite = Template.transformedBlockPos(settings, lastBlock).add(origin);
        StructureBoundingBox bounds = new StructureBoundingBox(
                Math.min(origin.getX(), opposite.getX()), origin.getY(),
                Math.min(origin.getZ(), opposite.getZ()),
                Math.max(origin.getX(), opposite.getX()), opposite.getY(),
                Math.max(origin.getZ(), opposite.getZ()));
        return new SearchTarget(Template.transformedBlockPos(settings, center).add(origin), bounds);
    }
}

package com.shiver.structurecompasslegacy.compat.gctcore;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModMethod;
import com.shiver.structurecompasslegacy.search.SeedMath;

import net.minecraft.block.material.Material;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldSavedData;

final class GctCorePlacement {

    private static final String WORLD_PACKAGE = "com.smd.gctcore.common.world.";

    private final GctCoreStructure structure;
    private final Class<? extends WorldSavedData> dataClass;
    private final String dataName;
    private final ModMethod positions;
    private final ModMethod caveCandidate;
    private final ModMethod cavePosition;
    private final ModMethod caveTemplate;

    GctCorePlacement(GctCoreStructure structure) {
        this.structure = structure;
        dataClass = type(structure.cave ? "ShadowberryCaveData" : "CrimsonTempleData")
                .asSubclass(WorldSavedData.class);
        dataName = (String) ModFields.read(ModFields.bind(dataClass.getName(), "NAME"), null);
        positions = ModMethod.bind(dataClass, structure.cave ? "getCaves" : "getTemples");
        if (structure.cave) {
            Class<?> generator = type("ShadowberryCaveGenerator");
            caveCandidate = ModMethod.bind(generator, "isCandidate", Random.class);
            cavePosition = ModMethod.bind(generator, "findPlacementPos",
                    Random.class, int.class, int.class, Predicate.class);
            caveTemplate = ModMethod.bind(generator, "selectTemplateName", Random.class);
        } else {
            caveCandidate = null;
            cavePosition = null;
            caveTemplate = null;
        }
    }

    private static Class<?> type(String name) {
        try {
            return Class.forName(WORLD_PACKAGE + name);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Missing GCT-Core class: " + name, exception);
        }
    }

    Random candidate(long seed, int chunkX, int chunkZ) {
        Random random = SeedMath.forge(seed, chunkX, chunkZ);
        boolean selected = structure.cave
                ? (Boolean) caveCandidate.call(null, random)
                : random.nextInt(2000) == 0; // CrimsonTempleGenerator.CHANCE
        return selected ? random : null;
    }

    @SuppressWarnings("unchecked")
    BlockPos recordedOrigin(WorldServer world, int chunkX, int chunkZ) {
        WorldSavedData data = world.getPerWorldStorage().getOrLoadData(dataClass, dataName);
        if (data != null) {
            for (BlockPos position : (List<BlockPos>) positions.call(data)) {
                if (position.getX() >> 4 == chunkX && position.getZ() >> 4 == chunkZ) {
                    return position;
                }
            }
        }
        return null;
    }

    SearchTarget locate(Random random, int chunkX, int chunkZ, World terrain, BlockPos recorded) {
        BlockPos origin;
        if (structure.cave) {
            Predicate<BlockPos> canPlace = recorded != null ? recorded::equals : position -> {
                Material material = terrain.getBlockState(position).getMaterial();
                return !material.isLiquid() && material != Material.AIR;
            };
            origin = (BlockPos) cavePosition.call(null, random, chunkX, chunkZ, canPlace);
            if (origin == null || !structure.id.equals(caveTemplate.call(null, random))) {
                return null;
            }
        } else {
            int x = chunkX * 16 + random.nextInt(16);
            int z = chunkZ * 16 + random.nextInt(16);
            origin = recorded != null ? recorded : terrain.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z));
            if (recorded == null && terrain.getBlockState(origin).getMaterial().isLiquid()) {
                return null;
            }
        }
        Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
        return structure.target(origin, rotation);
    }
}

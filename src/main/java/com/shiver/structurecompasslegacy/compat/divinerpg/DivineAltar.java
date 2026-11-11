package com.shiver.structurecompasslegacy.compat.divinerpg;

import net.minecraft.util.math.BlockPos;

enum DivineAltar {

    // twilight/eden/sunstorm_dungeon
    EDEN("eden", "Eden", "sunstormSpawner", 7, new BlockPos(8, 2, 8)),
    // twilight/wildwood/termasect_nest
    WILDWOOD("wildwood", "Wildwood", "termasectSpawner", 16, new BlockPos(7, 3, 8)),
    // twilight/apalachia/eternal_archer_dungeon
    APALACHIA("apalachia", "Apalachia", "eternalArcherSpawner", 5, new BlockPos(7, 2, 8)),
    // twilight/skythern/experienced_cori_dungeon
    SKYTHERN("skythern", "Skythern", "experiencedCoriSpawner", 16, new BlockPos(7, 7, 8));

    final String id;
    final String providerClass;
    final String generatorClass;
    final String grassField;
    final String spawnerField;
    final BlockPos size;
    final BlockPos spawner;

    DivineAltar(String dimension, String className, String spawnerField, int height, BlockPos spawner) {
        id = dimension + "_altar";
        String packageName = "divinerpg.dimensions." + dimension + ".";
        providerClass = packageName + "WorldProvider" + className;
        generatorClass = packageName + className + "ChunkGenerator";
        grassField = dimension + "Grass";
        this.spawnerField = spawnerField;
        size = new BlockPos(16, height, 16);
        this.spawner = spawner;
    }
}

package com.shiver.structurecompasslegacy.compat.divinerpg;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

/** 当前安装分支的模板目录、自然概率、层数、起始高度和完整模板尺寸。 */
enum DivineStructure {

    ARCHER_DUNGEON("archer_dungeon", "iceika/dungeons/archer",
            30, 0, 15, new BlockPos(25, 5, 14)),
    ROLLUM_DUNGEON("rollum_dungeon", "iceika/dungeons/rollum",
            110, 0, 15, new BlockPos(32, 5, 31)),

    CRYPT_1("crypt_1", "vethea/layer1/crypt1",
            300, 1, 10, new BlockPos(23, 12, 20)),
    CRYPT_2("crypt_2", "vethea/layer1/crypt2",
            300, 1, 12, new BlockPos(25, 8, 11)),
    HIVE("hive", "vethea/layer2/hive",
            800, 2, 64, new BlockPos(41, 24, 43)),
    TEMPLE_1("temple_1", "vethea/layer2/temple1",
            800, 2, 64, new BlockPos(43, 23, 45)),
    TEMPLE_2("temple_2", "vethea/layer2/temple2",
            800, 2, 64, new BlockPos(39, 21, 41)),
    QUADROTIC_POST("quadrotic_post", "vethea/layer3/quadroticpost",
            500, 3, 112, new BlockPos(24, 9, 24)),
    KAROS_MADHOUSE("karos_madhouse", "vethea/layer3/karosmadhouse",
            500, 3, 112, new BlockPos(31, 6, 20)),
    EVERGARDEN("evergarden", "vethea/layer4/evergarden",
            800, 4, 160, new BlockPos(31, 8, 35)),
    RAGLOK_CHAMBER("raglok_chamber", "vethea/layer4/raglokchamber",
            800, 4, 160, new BlockPos(32, 25, 27)),
    WRECK_HALL("wreck_hall", "vethea/layer4/wreckhall",
            800, 4, 160, new BlockPos(22, 12, 38));

    final String id;
    final ResourceLocation folder;
    final int rarity;
    final int layer;
    final int altitude;
    final BlockPos size;

    DivineStructure(String id, String folder, int rarity, int layer, int altitude, BlockPos size) {
        this.id = id;
        this.folder = new ResourceLocation("divinerpg", folder);
        this.rarity = rarity;
        this.layer = layer;
        this.altitude = altitude;
        this.size = size;
    }
}

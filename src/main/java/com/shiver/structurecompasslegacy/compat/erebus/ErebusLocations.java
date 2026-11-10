package com.shiver.structurecompasslegacy.compat.erebus;

import java.util.Collections;
import java.util.List;

import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModMethod;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

/** 只读原生位置索引；不调用会创建记录的 forWorld/checkWorldLocation。 */
final class ErebusLocations {

    private final Class<? extends WorldSavedData> type;
    private final ModMethod positions;
    private final String key;
    private final int exclusion;

    ErebusLocations(String name, String key, int exclusion) {
        type = ModConstructor.type("erebus.world.feature.structure.worlddata.WorldData" + name)
                .asSubclass(WorldSavedData.class);
        positions = ModMethod.bind(type, "getData");
        this.key = key;
        this.exclusion = exclusion;
    }

    @SuppressWarnings("unchecked")
    List<BlockPos> read(World world) {
        WorldSavedData data = world.getPerWorldStorage().getOrLoadData(type, key);
        return data == null ? Collections.emptyList() : (List<BlockPos>) positions.call(data);
    }

    boolean blocked(List<BlockPos> positions, int x, int z) {
        for (BlockPos position : positions) {
            // 原版检查 X、Z 各轴的闭区间，并不是欧氏距离。
            if (Math.abs((long) position.getX() - x) <= exclusion
                    && Math.abs((long) position.getZ() - z) <= exclusion) {
                return true;
            }
        }
        return false;
    }
}

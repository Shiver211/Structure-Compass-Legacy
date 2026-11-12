package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModMethod;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** 只读原版已加载的位置记录，不加载存档区域、区块或建立额外索引。 */
final class BetweenlandsSpiritTreeLocations {

    private final Class<?> spiritTreeType = ModConstructor.type(
            "thebetweenlands.common.world.storage.location.LocationSpiritTree");
    private final ModMethod getWorldStorage = ModMethod.bind(ModConstructor.type(
            "thebetweenlands.common.world.storage.BetweenlandsWorldStorage"), "forWorld", World.class);
    private final ModMethod getLocalStorageHandler = ModMethod.bind(ModConstructor.type(
            "thebetweenlands.api.storage.IWorldStorage"), "getLocalStorageHandler");
    private final ModMethod getLoadedStorages = ModMethod.bind(ModConstructor.type(
            "thebetweenlands.api.storage.ILocalStorageHandler"), "getLoadedStorages");
    private final ModMethod getEnclosingBounds = ModMethod.bind(spiritTreeType, "getEnclosingBounds");

    List<BlockPos> read(World world) {
        Object worldStorage = getWorldStorage.call(null, world);
        Object localStorageHandler = getLocalStorageHandler.call(worldStorage);
        Collection<?> loadedLocations = (Collection<?>) getLoadedStorages.call(localStorageHandler);

        List<BlockPos> positions = new ArrayList<>();
        for (Object location : loadedLocations) {
            if (!spiritTreeType.isInstance(location)) {
                continue;
            }

            AxisAlignedBB bounds = (AxisAlignedBB) getEnclosingBounds.call(location);
            if (bounds == null) {
                continue;
            }

            // 原版包围盒：new AxisAlignedBB(position).grow(32, 16, 32).offset(0, 6, 0)。
            positions.add(new BlockPos(bounds.minX + 32, bounds.minY + 10, bounds.minZ + 32));
        }

        return positions;
    }
}

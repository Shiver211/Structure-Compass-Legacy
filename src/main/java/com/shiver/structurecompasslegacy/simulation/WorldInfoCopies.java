package com.shiver.structurecompasslegacy.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldInfo;

/** 特殊世界信息由对应模块提供只读快照，结果始终是独立的原版 WorldInfo。 */
public final class WorldInfoCopies {

    private static final Map<Class<?>, Function<WorldInfo, NBTTagCompound>> COPIERS = new HashMap<>();

    private WorldInfoCopies() {
    }

    public static void register(Class<? extends WorldInfo> type, Function<WorldInfo, NBTTagCompound> copier) {
        COPIERS.put(type, copier);
    }

    public static WorldInfo copy(WorldInfo source) {
        Function<WorldInfo, NBTTagCompound> copier = COPIERS.get(source.getClass());
        NBTTagCompound data = copier == null ? source.cloneNBTCompound(null) : copier.apply(source);
        return new WorldInfo(data.copy());
    }
}

package com.shiver.structurecompasslegacy.simulation;

import net.minecraft.world.World;

/** 模拟入口的写入屏障；模组直接操作静态状态或文件仍需要独立适配。 */
public final class SimulationAccess {

    private SimulationAccess() {
    }

    public static void requireWorld(World world) {
        if (SimulationScope.isActive() && !(world instanceof SimulationMarker)) {
            throw new UnsupportedOperationException("Simulation cannot access mutable real-world state");
        }
    }

    public static void requireMemoryStorage(boolean memoryOnly) {
        if (SimulationScope.isActive() && !memoryOnly) {
            throw new UnsupportedOperationException("Simulation cannot access persistent storage");
        }
    }
}

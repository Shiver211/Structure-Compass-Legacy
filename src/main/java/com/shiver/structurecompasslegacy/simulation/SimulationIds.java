package com.shiver.structurecompasslegacy.simulation;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** 临时对象使用自己的递增 ID，不消耗 Minecraft 的全局分配器。 */
public final class SimulationIds {

    private static final AtomicInteger ENTITY = new AtomicInteger(Integer.MIN_VALUE);
    private static final AtomicLong BLOCK_TICK = new AtomicLong(Long.MIN_VALUE);

    private SimulationIds() {
    }

    public static int entity() {
        return ENTITY.getAndIncrement();
    }

    public static long blockTick() {
        return BLOCK_TICK.getAndIncrement();
    }
}

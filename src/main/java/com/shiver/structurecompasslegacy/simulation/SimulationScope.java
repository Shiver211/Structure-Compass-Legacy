package com.shiver.structurecompasslegacy.simulation;

/** 只在主线程的一次模拟调用内生效，离开调用后立即恢复正常事件派发。 */
public final class SimulationScope implements AutoCloseable {

    private static final ThreadLocal<SimulationScope> CURRENT = new ThreadLocal<>();

    private final SimulationScope previous;

    private SimulationScope() {
        previous = CURRENT.get();
        CURRENT.set(this);
    }

    public static SimulationScope enter() {
        return new SimulationScope();
    }

    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    @Override
    public void close() {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}

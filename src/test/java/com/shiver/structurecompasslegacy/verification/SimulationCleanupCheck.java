package com.shiver.structurecompasslegacy.verification;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import com.shiver.structurecompasslegacy.simulation.SimulationBlockTicks;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

import static com.shiver.structurecompasslegacy.verification.WorldStateSnapshot.require;

/** 资源清理断言只在测试中读取内部状态，正式世界无需提供诊断接口。 */
final class SimulationCleanupCheck {

    private SimulationCleanupCheck() {
    }

    static void verify(SimulationWorld world) {
        require(world.simulationChunks().getLoadedChunkCount() == 0, "session chunks retained");
        require(world.loadedEntityList.isEmpty() && world.weatherEffects.isEmpty(), "session entities retained");
        require(world.loadedTileEntityList.isEmpty() && world.tickableTileEntities.isEmpty(), "session tiles retained");
        require(((Map<?, ?>) read(world, "simulationEntities")).isEmpty(), "session entity index retained");
        require(((Map<?, ?>) read(world, "candidateState")).isEmpty(), "candidate state retained");
        require(((Collection<?>) read(world, "scheduledTasks")).isEmpty(), "session tasks retained");
        require(((SimulationBlockTicks) read(world, "blockTicks")).isEmpty(), "session block ticks retained");
        require(read(world, "executingTick") == null, "session executing tick retained");
        require(Boolean.FALSE.equals(read(world, "capturingPlacement")), "placement capture still active");
        require(read(world, "placementBounds") == null, "placement bounds retained");
    }

    private static Object read(SimulationWorld world, String name) {
        try {
            Field field = SimulationWorld.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(world);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot inspect simulation cleanup: " + name, exception);
        }
    }
}

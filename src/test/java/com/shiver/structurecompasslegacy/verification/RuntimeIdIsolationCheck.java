package com.shiver.structurecompasslegacy.verification;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import static com.shiver.structurecompasslegacy.verification.WorldStateSnapshot.require;

/** 客户端与内置服务器共享 Entity 静态字段；用另一线程复现正常实体构造。 */
final class RuntimeIdIsolationCheck {

    private static final Field ENTITY_IDS = ReflectionHelper.findField(
            Entity.class, new String[] {"nextEntityID", "field_70152_a"});
    private static final Field TICK_IDS = ReflectionHelper.findField(
            NextTickListEntry.class, new String[] {"nextTickEntryID", "field_77177_f"});

    private RuntimeIdIsolationCheck() {
    }

    static void verify(WorldServer source) throws Exception {
        verifyOtherThread(source);
        verifySimulationCounters(source);
        verifySameThread(source, () -> new EntityItem(source), "real entity ID");
        verifySameThread(source, () -> new NextTickListEntry(BlockPos.ORIGIN, Blocks.STONE), "real block-tick ID");
    }

    private static void verifyOtherThread(WorldServer source) throws Exception {
        int entityId = ENTITY_IDS.getInt(null);
        long tickId = TICK_IDS.getLong(null);
        try (SimulationScope ignored = SimulationScope.enter()) {
            CompletableFuture.runAsync(() -> {
                new EntityItem(source);
                new NextTickListEntry(BlockPos.ORIGIN, Blocks.STONE);
            }).get(10, TimeUnit.SECONDS);
        }
        require(ENTITY_IDS.getInt(null) == entityId + 1, "other thread did not allocate a real entity ID");
        require(TICK_IDS.getLong(null) == tickId + 1, "other thread did not allocate a real block-tick ID");
    }

    private static void verifySimulationCounters(WorldServer source) throws Exception {
        // 此处没有并发分配：直接确认真实注入后的模拟构造不改全局原始值。
        try (SimulationSession session = new SimulationSession(source);
                WorldStateSnapshot snapshot = new WorldStateSnapshot(source);
                SimulationScope ignored = SimulationScope.enter()) {
            Object entityId = ENTITY_IDS.get(null);
            Object tickId = TICK_IDS.get(null);
            EntityItem first = new EntityItem(session.world());
            EntityItem second = new EntityItem(session.world());
            new NextTickListEntry(BlockPos.ORIGIN, Blocks.STONE);
            require(first.getEntityId() != second.getEntityId(), "simulation entity IDs collided");
            require(entityId.equals(ENTITY_IDS.get(null)), "simulation consumed the real entity counter");
            require(tickId.equals(TICK_IDS.get(null)), "simulation consumed the real block-tick counter");
        }
    }

    private static void verifySameThread(WorldServer source, Runnable allocation, String message) {
        try (WorldStateSnapshot snapshot = new WorldStateSnapshot(source)) {
            // 故意在 scope 外走真实分配，确认审核仍能检测当前线程的错误路径。
            allocation.run();
        } catch (IllegalStateException expected) {
            require(expected.getMessage().contains(message), "wrong allocation failure: " + expected.getMessage());
            return;
        }
        throw new AssertionError("Real allocation was not detected: " + message);
    }
}

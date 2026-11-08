package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.eventhandler.ListenerList;

import static org.mockito.Mockito.mock;

/** 在实际 Mixin 下验证：屏蔽未知事件后，不能忽略真实世界的群系规则修改。 */
public final class BetweenlandsRuntimeCheck {

    public static void verify() throws Exception {
        if (System.getProperty("compatModsDir") == null) {
            return;
        }
        BetweenlandsNativeFixture nativeCode = new BetweenlandsNativeFixture();
        BetweenlandsBiomeLayers binding = nativeCode.binding();
        BiomeProvider source = mock(BiomeProvider.class);
        GenLayer original = nativeCode.biomeLayer(0L, WorldType.DEFAULT);
        BetweenlandsNativeFixture.field(BiomeProvider.class, "biomeIndexLayer").set(source, original);
        if (binding.create(0L, WorldType.DEFAULT, source) == null) {
            throw new AssertionError("Native Betweenlands biome rules were rejected");
        }

        AtomicInteger calls = new AtomicInteger();
        IEventListener listener = event -> {
            if (event instanceof WorldTypeEvent.BiomeSize) {
                calls.incrementAndGet();
                ((WorldTypeEvent.BiomeSize) event).setNewSize(5);
            }
        };
        // 此独立 JVM 不运行 Forge 的事件构造器转换；直接登记监听器，仍通过真实 EventBus.post 派发。
        int bus = BetweenlandsNativeFixture.field(EventBus.class, "busID").getInt(MinecraftForge.TERRAIN_GEN_BUS);
        new WorldTypeEvent.BiomeSize(WorldType.DEFAULT, 4).getListenerList().register(bus, EventPriority.NORMAL, listener);
        try {
            GenLayer changed = nativeCode.biomeLayer(0L, WorldType.DEFAULT);
            BetweenlandsNativeFixture.field(BiomeProvider.class, "biomeIndexLayer").set(source, changed);
            int before = calls.get();
            if (before == 0) {
                throw new AssertionError("The source biome-size event was not delivered");
            }
            if (binding.create(0L, WorldType.DEFAULT, source) != null) {
                throw new AssertionError("Changed biome rules were accepted");
            }
            if (calls.get() != before) {
                throw new AssertionError("Simulation dispatched a biome-size event");
            }
        } finally {
            ListenerList.unregisterAll(bus, listener);
        }
        System.out.println("BETWEENLANDS_RUNTIME_PASSED: private_layers, suppressed_events, changed_rules_rejected");
    }
}

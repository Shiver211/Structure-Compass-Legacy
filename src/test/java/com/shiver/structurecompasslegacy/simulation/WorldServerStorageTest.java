package com.shiver.structurecompasslegacy.simulation;

import java.lang.reflect.Method;

import com.shiver.structurecompasslegacy.mixin.WorldServerStorageMixin;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class WorldServerStorageTest {

    @Test
    void simulationUsesMemoryStorageWithoutAskingTheSaveHandlerForFiles() throws Exception {
        ISaveHandler files = mock(ISaveHandler.class);
        ISaveHandler selected = selectHandler(new SimulatedWorld(), files);
        assertNull(selected);
        MapStorage storage = new MapStorage(selected);
        assertEquals(0, storage.getUniqueDataId("probe"));
        assertEquals(1, storage.getUniqueDataId("probe"));
        storage.saveAllData();
        verifyNoInteractions(files);

        MapStorage nextSession = new MapStorage(selectHandler(new SimulatedWorld(), files));
        assertEquals(0, nextSession.getUniqueDataId("probe"));
    }

    @Test
    void realWorldKeepsItsHandlerEvenInsideASimulationScope() throws Exception {
        WorldServerStorageMixin realWorld = new WorldServerStorageMixin() {
        };
        ISaveHandler files = mock(ISaveHandler.class);
        try (SimulationScope ignored = SimulationScope.enter()) {
            assertSame(files, selectHandler(realWorld, files));
        }
    }

    @Test
    void worldServerConstructorHasOneMapStorageConstructionToRedirect() throws Exception {
        ClassNode world = new ClassNode();
        new ClassReader(WorldServer.class.getName()).accept(world, 0);
        int matches = 0;
        for (MethodNode method : world.methods) {
            if (!method.name.equals("<init>")) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals("net/minecraft/world/storage/MapStorage") && call.name.equals("<init>")
                            && call.desc.equals("(Lnet/minecraft/world/storage/ISaveHandler;)V")) {
                        matches++;
                    }
                }
            }
        }
        assertEquals(1, matches);
    }

    private ISaveHandler selectHandler(WorldServerStorageMixin world, ISaveHandler handler) throws Exception {
        Method method = WorldServerStorageMixin.class.getDeclaredMethod("compassUseMemoryMapStorage", ISaveHandler.class);
        method.setAccessible(true);
        return (ISaveHandler) method.invoke(world, handler);
    }

    private static final class SimulatedWorld extends WorldServerStorageMixin implements SimulationMarker {
    }
}

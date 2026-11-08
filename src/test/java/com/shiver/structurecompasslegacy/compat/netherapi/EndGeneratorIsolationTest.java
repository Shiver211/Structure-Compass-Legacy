package com.shiver.structurecompasslegacy.compat.netherapi;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.shiver.structurecompasslegacy.compat.InstalledModClasses;
import com.shiver.structurecompasslegacy.compat.netherapi.mixin.EndGeneratorMixin;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndGeneratorIsolationTest {

    @Test
    void simulationSkipsFactoriesAndLeavesRealGenerationUnchanged() throws Exception {
        Method redirect = EndGeneratorMixin.class.getDeclaredMethod("compassSkipUnrelatedStructures", List.class);
        redirect.setAccessible(true);
        EndGeneratorMixin mixin = new EndGeneratorMixin() {
        };
        AtomicInteger factoryCalls = new AtomicInteger();
        List<Runnable> entries = Arrays.asList(factoryCalls::incrementAndGet, factoryCalls::incrementAndGet);

        try (SimulationScope ignored = SimulationScope.enter()) {
            runFactories(redirect, mixin, entries);
        }
        assertEquals(0, factoryCalls.get());
        assertEquals(2, entries.size());

        runFactories(redirect, mixin, entries);
        assertEquals(2, factoryCalls.get());
    }

    @Test
    void installedConstructorMatchesTheSingleRedirectAndFactorySignature() throws Exception {
        ClassNode node = InstalledModClasses.read("Nether-API-v1.4.5-mc1.12.2.jar",
                "git/jbredwards/nether_api/mod/common/world/gen/ChunkGeneratorTheEnd");
        assertTrue(node.interfaces.contains("git/jbredwards/nether_api/api/world/INetherAPIChunkGenerator"));
        String descriptor = "(Lnet/minecraft/world/World;Z"
                + "Lgit/jbredwards/nether_api/mod/common/world/biome/BiomeProviderTheEnd;"
                + "Lnet/minecraft/util/math/BlockPos;)V";
        int matches = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("<init>") || !method.desc.equals(descriptor)) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals("java/util/List") && call.name.equals("stream")
                            && call.desc.equals("()Ljava/util/stream/Stream;")) {
                        matches++;
                    }
                }
            }
        }
        assertEquals(1, matches, "Nether API changed: review the constructor before enabling the adapter");
    }

    private void runFactories(Method redirect, EndGeneratorMixin mixin, List<Runnable> entries) throws Exception {
        try (Stream<?> stream = (Stream<?>) redirect.invoke(mixin, entries)) {
            stream.forEach(factory -> ((Runnable) factory).run());
        }
    }
}

package com.shiver.structurecompasslegacy.verification;

import com.shiver.structurecompasslegacy.compat.InstalledModClasses;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurrentProbeContractTest {

    @Test
    void installedTerrainProbeUsesTheAuditedApi() throws Exception {
        ClassNode factor = InstalledModClasses.read("RecurrentComplex-1.4.8.7.jar",
                "ivorius/reccomplex/world/gen/feature/structure/generic/placement/FactorMatch");
        assertTrue(hasMethod(factor, "<init>", "(FLjava/lang/String;Ljava/lang/String;F)V", Opcodes.ACC_PUBLIC));
        assertTrue(hasMethod(factor, "weight", "(Livorius/ivtoolkit/world/WorldCache;Ljava/util/Set;F)F",
                Opcodes.ACC_PROTECTED));
        ClassNode cache = InstalledModClasses.read("IvToolkit-1.3.3-1.12.jar", "ivorius/ivtoolkit/world/WorldCache");
        assertTrue(hasMethod(cache, "<init>",
                "(Lnet/minecraft/world/World;Lnet/minecraft/world/gen/structure/StructureBoundingBox;)V",
                Opcodes.ACC_PUBLIC));
    }

    private boolean hasMethod(ClassNode owner, String name, String descriptor, int access) {
        for (MethodNode method : owner.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor) && (method.access & access) != 0) {
                return true;
            }
        }
        return false;
    }
}

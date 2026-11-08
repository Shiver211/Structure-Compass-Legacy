package com.shiver.structurecompasslegacy.compat.abyssalcraft;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.shiver.structurecompasslegacy.compat.InstalledModClasses;
import com.shiver.structurecompasslegacy.search.SeedMath;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreadlandsMineshaftTest {

    @Test
    void predictionMatchesInstalledGeneratorAcrossSeedsAndNegativeChunks() throws Exception {
        Class<?> referenceClass = isolatedPlacementRule();
        Object reference = referenceClass.getConstructor().newInstance();
        Field random = referenceClass.getField("field_75038_b");
        Method canSpawn = referenceClass.getDeclaredMethod("func_75047_a", int.class, int.class);
        canSpawn.setAccessible(true);

        long[] seeds = {0L, 1L, -17429003L, Long.MIN_VALUE, Long.MAX_VALUE};
        int matches = 0;
        for (long seed : seeds) {
            for (int x = -160; x <= 160; x++) {
                for (int z = -160; z <= 160; z++) {
                    random.set(reference, SeedMath.mapGen(seed, x, z));
                    boolean expected = (Boolean) canSpawn.invoke(reference, x, z);
                    assertEquals(expected, DreadlandsMineshaftTask.canSpawn(seed, x, z),
                            "seed=" + seed + ", chunk=" + x + "," + z);
                    if (expected) {
                        matches++;
                    }
                }
            }
        }
        assertTrue(matches > 1000, "Comparison must include successful candidates");
    }

    private Class<?> isolatedPlacementRule() throws Exception {
        ClassNode node = InstalledModClasses.read("AbyssalCraft-1.12.2-1.11.3.jar",
                "com/shinoow/abyssalcraft/common/structures/dreadlands/mineshaft/MapGenDreadlandsMine");
        String originalParent = node.superName;
        node.superName = "java/lang/Object";
        node.methods.removeIf(method -> !method.name.equals("func_75047_a")
                && !(method.name.equals("<init>") && method.desc.equals("()V")));
        node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "field_75038_b", "Ljava/util/Random;", null, null));

        // 保留原构造器中的概率赋值与原判定字节码，只移除 Minecraft 父类依赖。
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals(originalParent) && call.name.equals("<init>")) {
                        call.owner = "java/lang/Object";
                    }
                }
            }
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        return new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
    }
}

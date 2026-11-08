package com.shiver.structurecompasslegacy.compat.endexpansion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.shiver.structurecompasslegacy.compat.InstalledModClasses;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public class LamentedIslandsTest {

    private static final String JAR = "EndExpansion-1.2-fix.jar";
    private static final String GENERATOR =
            "com/jboymercs/endexpansion/world/api/lamentedIslands/WorldGenLamentedIslands";

    @BeforeAll
    static void initializeMinecraft() {
        Bootstrap.register();
    }

    @Test
    void candidatesMatchInstalledRuleIncludingNegativeCoordinateAdjustment() throws Exception {
        Class<?> referenceClass = placementRule();
        Object reference = referenceClass.getConstructor().newInstance();
        Method canSpawn = referenceClass.getDeclaredMethod("canSpawnStructureAtPos", World.class, int.class, int.class);
        canSpawn.setAccessible(true);
        Field spacing = field(referenceClass, "spacing");
        field(referenceClass, "separation").setInt(reference, 16);
        AtomicLong seed = new AtomicLong();
        World world = mock(World.class, withSettings().stubOnly().defaultAnswer(call -> {
            if (call.getMethod().getName().equals("setRandomSeed")) {
                int x = call.getArgument(0);
                int z = call.getArgument(1);
                int salt = call.getArgument(2);
                return new Random(x * 341873128712L + z * 132897987541L + seed.get() + salt);
            }
            return null;
        }));

        int matches = 0;
        for (long value : new long[] {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE}) {
            seed.set(value);
            for (int distance : new int[] {17, 32, 100}) {
                spacing.setInt(reference, distance);
                for (int x = -110; x <= 110; x++) {
                    for (int z = -110; z <= 110; z++) {
                        boolean expected = (Boolean) canSpawn.invoke(reference, world, x, z);
                        assertEquals(expected, LamentedIslandsTask.gridCandidate(value, x, z, distance),
                                "seed=" + value + ", spacing=" + distance + ", chunk=" + x + "," + z);
                        if (expected) {
                            matches++;
                        }
                    }
                }
            }
        }
        assertTrue(matches > 100, "The comparison must include successful candidates");
    }

    @Test
    void outerGeneratorUsesEitherAxisAndTheChunkCenter() {
        assertFalse(LamentedIslandsTask.outsideCentralArea(37, -38));
        assertTrue(LamentedIslandsTask.outsideCentralArea(38, 0));
        assertTrue(LamentedIslandsTask.outsideCentralArea(0, -39));
        assertEquals(new BlockPos(-616, 90, 616), LamentedIslandsTask.anchor(-39, 38));
    }

    /** 群系已在单独规则外处理；保留目标 JAR 的完整网格字节码。 */
    public static boolean allowBiome(BlockPos position, World world) {
        return true;
    }

    private static Class<?> placementRule() throws Exception {
        ClassNode node = InstalledModClasses.read(JAR, GENERATOR);
        node.superName = "java/lang/Object";
        node.methods.removeIf(method -> !method.name.equals("canSpawnStructureAtPos"));
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.name.equals("func_72843_D")) {
                    call.name = "setRandomSeed";
                } else if (call.name.equals("isAbleToSpawnHere")) {
                    call.owner = LamentedIslandsTest.class.getName().replace('.', '/');
                    call.name = "allowBiome";
                }
            }
        }
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(constructor);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        return new ClassLoader(LamentedIslandsTest.class.getClassLoader()) {

            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}

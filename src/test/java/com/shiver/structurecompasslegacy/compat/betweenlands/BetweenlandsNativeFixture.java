package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.function.Function;

import com.shiver.structurecompasslegacy.compat.InstalledModClasses;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.layer.GenLayer;
import org.junit.jupiter.api.Assumptions;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/** 从指定安装 JAR 执行规则和方块几何；存储登记被移除，原生随机消耗保持不变。 */
final class BetweenlandsNativeFixture extends ClassLoader {

    static final String JAR = "TheBetweenlands-3.9.6-universal.jar";
    private static final String ROOT = "thebetweenlands/";
    private static final String FEATURE = ROOT + "common/world/gen/feature/";
    private static final String TYPES = BetweenlandsNativeTypes.class.getName().replace('.', '/');
    private final Properties names = new Properties();
    private final World world;
    private long seed;
    private Function<BlockPos, Biome> biomes;

    BetweenlandsNativeFixture() throws Exception {
        super(BetweenlandsNativeFixture.class.getClassLoader());
        String directory = System.getProperty("compatModsDir");
        Assumptions.assumeTrue(directory != null && Files.isRegularFile(Paths.get(directory, JAR)),
                "Supply the Betweenlands 3.9.6 JAR through -PcompatModsDir");
        try (InputStream input = getResourceAsStream("compat/betweenlands-native-names.properties")) {
            names.load(input);
        }
        BiomeProvider provider = mock(BiomeProvider.class, withSettings().stubOnly().defaultAnswer(call -> {
            if (call.getMethod().getName().equals("getBiome")) {
                return biomes.apply(call.getArgument(0));
            }
            throw new AssertionError("Unexpected biome access: " + call.getMethod());
        }));
        world = mock(World.class, withSettings().stubOnly().defaultAnswer(call -> {
            switch (call.getMethod().getName()) {
                case "getSeed": return seed;
                case "getBiomeProvider": return provider;
                default: throw new AssertionError("Unexpected world access: " + call.getMethod());
            }
        }));
    }

    void world(long seed, Function<BlockPos, Biome> biomes) {
        this.seed = seed;
        this.biomes = biomes;
    }

    GenLayer biomeLayer(long seed, WorldType type) throws Exception {
        Class<?> base = loadClass("thebetweenlands.common.world.gen.layer.GenLayerBetweenlands");
        return ((GenLayer[]) base.getMethod("initializeAllBiomeGenerators", long.class, WorldType.class)
                .invoke(null, seed, type))[1];
    }

    BetweenlandsBiomeLayers binding() throws Exception {
        return new BetweenlandsBiomeLayers(loadClass("thebetweenlands.common.world.gen.layer.GenLayerBetweenlands"));
    }

    Generator generator(boolean islands) throws Exception {
        Class<?> type = loadClass((FEATURE + (islands ? "MapGenFloatingIslands" : "MapGenGiantRoots")).replace('/', '.'));
        Object instance = islands ? type.getConstructor(long.class).newInstance(seed)
                : type.getConstructor(long.class, Set.class).newInstance(seed, new HashSet<>(Arrays.asList(
                        BetweenlandsNativeTypes.Biomes.RAISED_ISLES, BetweenlandsNativeTypes.Biomes.COARSE_ISLANDS)));
        return new Generator(instance, islands);
    }

    final class Generator extends MapGenBase {

        private final Object nativeGenerator;
        private final boolean islands;
        private final Method recursive;
        private final Field results;
        private final Field roots;

        Generator(Object generator, boolean islands) throws Exception {
            this.nativeGenerator = generator;
            this.islands = islands;
            range = 0;
            field(MapGenBase.class, "rand").set(generator, rand);
            field(MapGenBase.class, "world").set(generator, BetweenlandsNativeFixture.this.world);
            recursive = generator.getClass().getDeclaredMethod("recursiveGenerate", World.class,
                    int.class, int.class, int.class, int.class, ChunkPrimer.class);
            recursive.setAccessible(true);
            results = field(generator.getClass(), islands ? "islands" : "giantRootGens");
            roots = field(generator.getClass(), "giantRootGens");
        }

        BlockPos candidate(int x, int z) throws Exception {
            ((List<?>) results.get(nativeGenerator)).clear();
            ((List<?>) roots.get(nativeGenerator)).clear();
            // 由 Minecraft 的 MapGenBase 初始化随机源；不借用被测预测代码的种子函数。
            super.generate(BetweenlandsNativeFixture.this.world, x, z, null);
            List<?> found = (List<?>) results.get(nativeGenerator);
            return found.isEmpty() ? null : islands ? (BlockPos) found.get(0)
                    : (BlockPos) found.get(0).getClass().getField("start").get(found.get(0));
        }

        @Override
        protected void recursiveGenerate(World source, int x, int z, int originalX, int originalZ, ChunkPrimer primer) {
            try {
                recursive.invoke(nativeGenerator, source, x, z, originalX, originalZ, primer);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        }

        ChunkPrimer generateLandmark(BlockPos position) throws Exception {
            int x = position.getX() >> 4;
            int z = position.getZ() >> 4;
            ChunkPrimer primer = new ChunkPrimer();
            // 使用原生完整 generate：候选筛选、岛体/树根几何及跨区块裁切均执行。
            ((MapGenBase) nativeGenerator).generate(BetweenlandsNativeFixture.this.world, x, z, primer);
            return primer;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            ClassNode node = InstalledModClasses.read(JAR, name.replace('.', '/'));
            for (MethodNode method : node.methods) {
                if (method.name.equals("generateIsland")) {
                    removeIslandRegistration(method);
                }
                replaceBlockResources(method);
            }
            ClassWriter original = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(original);
            ClassWriter writer = new ClassWriter(0);
            new ClassReader(original.toByteArray()).accept(new ClassRemapper(writer, new Remapper() {

                @Override
                public String map(String type) {
                    switch (type) {
                        case ROOT + "common/registries/BiomeRegistry": return TYPES + "$Biomes";
                        case ROOT + "common/world/biome/BiomeBetweenlands": return TYPES + "$WeightedBiome";
                        case ROOT + "api/entity/spawning/IWeightProvider": return TYPES + "$Weight";
                        case ROOT + "common/config/BetweenlandsConfig": return TYPES + "$Config";
                        case ROOT + "common/config/BetweenlandsConfig$Debug": return TYPES + "$Debug";
                        case ROOT + "common/config/properties/StringSetProperty": return "java/util/HashSet";
                        case ROOT + "common/world/gen/biome/feature/CoarseIslandsFeature": return TYPES + "$CoarseFeature";
                        default: return type;
                    }
                }

                @Override
                public String mapMethodName(String owner, String method, String descriptor) {
                    return names.getProperty(method, method);
                }

                @Override
                public String mapFieldName(String owner, String field, String descriptor) {
                    return names.getProperty(field, field);
                }
            }), ClassReader.EXPAND_FRAMES);
            byte[] bytes = writer.toByteArray();
            return defineClass(name, bytes, 0, bytes.length);
        } catch (Exception exception) {
            throw new ClassNotFoundException(name, exception);
        }
    }

    private static void removeIslandRegistration(MethodNode method) {
        LabelNode afterRegistration = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (afterRegistration == null && instruction instanceof JumpInsnNode) {
                afterRegistration = ((JumpInsnNode) instruction).label;
            }
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.endsWith("/BetweenlandsWorldStorage") && call.name.equals("forWorld")) {
                    AbstractInsnNode cursor = call.getPrevious().getPrevious();
                    while (cursor != afterRegistration) {
                        AbstractInsnNode next = cursor.getNext();
                        method.instructions.remove(cursor);
                        cursor = next;
                    }
                    return;
                }
            }
        }
        throw new AssertionError("Native island registration changed");
    }

    private static void replaceBlockResources(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) instruction;
                if (field.owner.equals(ROOT + "common/registries/BlockRegistry")) {
                    method.instructions.insertBefore(field, new LdcInsnNode(field.name));
                    method.instructions.set(field, new MethodInsnNode(Opcodes.INVOKESTATIC, TYPES, "block",
                            "(Ljava/lang/String;)Lnet/minecraft/block/Block;", false));
                } else if (field.owner.startsWith(ROOT + "common/block/")) {
                    method.instructions.set(field, new InsnNode(Opcodes.ACONST_NULL));
                }
            } else if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.name.equals("func_177226_a")) {
                    call.setOpcode(Opcodes.INVOKESTATIC);
                    call.owner = TYPES;
                    call.name = "property";
                    call.desc = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/block/properties/IProperty;"
                            + "Ljava/lang/Comparable;)Lnet/minecraft/block/state/IBlockState;";
                    call.itf = false;
                } else if (call.owner.startsWith(ROOT + "common/block/") && call.name.equals("func_176223_P")) {
                    call.owner = "net/minecraft/block/Block";
                }
            }
        }
    }

    static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}

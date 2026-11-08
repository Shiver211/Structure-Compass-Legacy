package com.shiver.structurecompasslegacy.compat.aether;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.shiver.structurecompasslegacy.compat.InstalledModClasses;
import com.shiver.structurecompasslegacy.compat.aether.BronzeNativeTypes.Terrain;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
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
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** 执行安装版布局与空间检查；受控地形和资源替身只用于验证生成依赖。 */
public final class BronzeNativeFixture extends ClassLoader {

    static final String JAR = "aether-1.12.2-v1.5.4.1.jar";
    private static final String ROOT = "com/gildedgames/the_aether/";
    private static final String FIXTURE = BronzeNativeFixture.class.getName().replace('.', '/');
    private static final String TYPES = BronzeNativeTypes.class.getName().replace('.', '/');
    private static final ThreadLocal<Random> SHUFFLE = new ThreadLocal<>();
    private static final Map<String, String> NAMES = names();

    private final Object dungeon;
    private final Method generate;
    private final Object provider;
    private final Method flush;
    final Terrain terrain = new Terrain();

    BronzeNativeFixture() throws Exception {
        super(BronzeNativeFixture.class.getClassLoader());
        String directory = System.getProperty("compatModsDir");
        Assumptions.assumeTrue(directory != null && Files.isRegularFile(Paths.get(directory, JAR)),
                "Supply Aether 1.5.4.1 through -PcompatModsDir");
        Class<?> type = loadClass((ROOT + "world/dungeon/BronzeDungeon").replace('/', '.'));
        dungeon = type.getConstructor().newInstance();
        generate = type.getMethod("generate", Terrain.class, Random.class, BlockPos.class);
        Class<?> providerType = loadClass((ROOT + "world/ChunkProviderAether").replace('/', '.'));
        provider = providerType.getConstructor().newInstance();
        field(providerType, "worldObj").set(provider, terrain);
        field(providerType, "dungeon_bronze").set(provider, dungeon);
        flush = providerType.getDeclaredMethod("generateBronzeDungeon", BlockPos.class);
        flush.setAccessible(true);
    }

    // entryRandomSeed 表示进入 BronzeDungeon.generate 时的随机状态，不是世界种子。
    boolean generate(BlockPos origin, long entryRandomSeed, long shuffleSeed) throws Exception {
        SHUFFLE.set(new Random(shuffleSeed));
        try {
            return (Boolean) generate.invoke(dungeon, terrain, new Random(entryRandomSeed), origin);
        } finally {
            SHUFFLE.remove();
        }
    }

    @SuppressWarnings("unchecked")
    Map<BlockPos, IBlockState> storage(String name) throws Exception {
        return (Map<BlockPos, IBlockState>) dungeon.getClass().getField(name).get(dungeon);
    }

    boolean hasBoss(BlockPos origin) {
        BlockPos center = origin.add(8, 2, 8);
        return terrain.sliders.stream().anyMatch(slider -> !slider.dead && center.equals(slider.position));
    }

    void flush() throws Exception {
        flush.invoke(provider, BlockPos.ORIGIN);
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static void shuffle(List<?> values) {
        // 两个固定状态代表原生 Collections 全局随机源可能具有的状态，不改世界随机源。
        Collections.shuffle(values, SHUFFLE.get());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            ClassNode node = InstalledModClasses.read(JAR, name.replace('.', '/'));
            if (name.endsWith("ChunkProviderAether")) {
                retainPlacement(node);
            }
            for (MethodNode method : node.methods) {
                replaceResources(method);
            }
            ClassWriter original = new ClassWriter(0);
            node.accept(original);
            ClassWriter writer = new ClassWriter(0);
            new ClassReader(original.toByteArray()).accept(new ClassRemapper(writer, new Remapper() {

                @Override
                public String map(String type) {
                    switch (type) {
                        case "net/minecraft/world/World": return TYPES + "$Terrain";
                        case "net/minecraft/world/gen/feature/WorldGenerator": return "java/lang/Object";
                        case "net/minecraft/entity/Entity":
                        case ROOT + "entities/bosses/slider/EntitySlider": return TYPES + "$Slider";
                        case ROOT + "AetherConfig": return TYPES + "$Config";
                        case ROOT + "AetherConfig$WorldGenOptions": return TYPES + "$WorldGen";
                        case ROOT + "blocks/dungeon/BlockTreasureChest": return TYPES + "$Chest";
                        case "net/minecraft/tileentity/TileEntity":
                        case "net/minecraft/tileentity/TileEntityChest": return TYPES + "$ChestTile";
                        case ROOT + "registry/AetherLootTables": return TYPES + "$Loot";
                        default: return type;
                    }
                }

                @Override
                public String mapMethodName(String owner, String method, String descriptor) {
                    return NAMES.getOrDefault(method, method);
                }

                @Override
                public String mapFieldName(String owner, String field, String descriptor) {
                    return NAMES.getOrDefault(field, field);
                }
            }), ClassReader.EXPAND_FRAMES);
            byte[] bytes = writer.toByteArray();
            return defineClass(name, bytes, 0, bytes.length);
        } catch (Exception exception) {
            throw new ClassNotFoundException(name, exception);
        }
    }

    private static void retainPlacement(ClassNode node) {
        node.interfaces.clear();
        node.fields.removeIf(field -> !field.name.equals("worldObj") && !field.name.equals("dungeon_bronze"));
        node.methods.removeIf(method -> !method.name.equals("generateBronzeDungeon"));
        MethodNode placement = node.methods.get(0);
        // 已单独执行原生 generate；这里只保留紧随其后的三类方块放置与队列清理。
        for (AbstractInsnNode instruction : placement.instructions.toArray()) {
            placement.instructions.remove(instruction);
            if (instruction.getOpcode() == Opcodes.POP) {
                break;
            }
        }
        placement.localVariables = null;
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        constructor.maxStack = 1;
        constructor.maxLocals = 1;
        node.methods.add(constructor);
    }

    private static void replaceResources(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) instruction;
                if (field.owner.equals(ROOT + "blocks/BlocksAether")) {
                    method.instructions.insertBefore(field, new LdcInsnNode(field.name));
                    method.instructions.set(field, new MethodInsnNode(Opcodes.INVOKESTATIC, TYPES, "block",
                            "(Ljava/lang/String;)Lnet/minecraft/block/Block;", false));
                } else if (field.owner.startsWith(ROOT + "blocks/")) {
                    method.instructions.set(field, new InsnNode(Opcodes.ACONST_NULL));
                }
            } else if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.equals("java/util/Collections") && call.name.equals("shuffle")) {
                    call.owner = FIXTURE;
                } else if (call.name.equals("func_176223_P")) {
                    call.owner = "net/minecraft/block/Block";
                } else if (call.name.equals("func_177226_a")) {
                    call.setOpcode(Opcodes.INVOKESTATIC);
                    call.owner = TYPES;
                    call.name = "property";
                    call.desc = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/block/properties/IProperty;"
                            + "Ljava/lang/Comparable;)Lnet/minecraft/block/state/IBlockState;";
                    call.itf = false;
                }
            }
        }
    }

    private static Map<String, String> names() {
        Map<String, String> names = new HashMap<>();
        names.put("func_180709_b", "generate");
        names.put("func_177958_n", "getX");
        names.put("func_177956_o", "getY");
        names.put("func_177952_p", "getZ");
        names.put("func_177982_a", "add");
        names.put("func_181079_c", "setPos");
        names.put("func_175667_e", "isBlockLoaded");
        names.put("func_180495_p", "getBlockState");
        names.put("func_176223_P", "getDefaultState");
        names.put("func_177230_c", "getBlock");
        names.put("field_150350_a", "AIR");
        names.put("field_72995_K", "isRemote");
        names.put("func_72838_d", "spawnEntity");
        names.put("func_70107_b", "setPosition");
        names.put("func_70106_y", "setDead");
        names.put("field_70165_t", "posX");
        names.put("field_70163_u", "posY");
        names.put("field_70161_v", "posZ");
        names.put("func_180501_a", "setBlockState");
        names.put("func_175625_s", "getTileEntity");
        names.put("func_189404_a", "setLootTable");
        return names;
    }
}

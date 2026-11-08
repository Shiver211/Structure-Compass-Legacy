package com.shiver.structurecompasslegacy.compat.divinerpg;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarFile;

import com.shiver.structurecompasslegacy.compat.InstalledModClasses;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.ISaveHandler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/** 执行安装 JAR 的概率、旋转、高度与组件布局；不加载实体和战利品处理。 */
final class DivineNativeFixture extends ClassLoader {

    private static final String JAR = "divinerpg-1.0.0-patched.jar";
    private static final String[] TYPES = {
            "iceika.archerdungeon.ArcherDungeon", "iceika.rollum.RollumDungeon",
            "vethea.crypt1.Crypt1", "vethea.crypt2.Crypt2", "vethea.hive.Hive",
            "vethea.temple1.Temple1", "vethea.temple2.Temple2", "vethea.quadroticpost.QuadroticPost",
            "vethea.karosmadhouse.KarosMadhouse", "vethea.evergarden.Evergarden",
            "vethea.raglokchamber.RaglokChamber", "vethea.wreckhall.WreckHall"
    };
    private static final Set<String> METHODS = new HashSet<>(Arrays.asList(
            "<init>", "<clinit>", "func_143025_a", "func_75047_a", "func_75049_b",
            "getResourceLocationHashCode", "setupComponents", "getComponent",
            "seedRandomWithOffset", "seedRandomWithWorldAndChunks", "getRandomRotation"));

    private final Map<ResourceLocation, Template> templates = new HashMap<>();
    private final AtomicLong seed = new AtomicLong();
    private final World world;
    private final MapGenStructure[] generators = new MapGenStructure[TYPES.length];
    private final Method canSpawn;
    private final Method createStart;

    DivineNativeFixture() throws Exception {
        super(DivineNativeFixture.class.getClassLoader());
        TemplateManager manager = mock(TemplateManager.class, call -> {
            ResourceLocation location = call.getArgument(1);
            if (!templates.containsKey(location)) {
                templates.put(location, readTemplate(location));
            }
            return templates.get(location);
        });
        ISaveHandler saveHandler = mock(ISaveHandler.class);
        when(saveHandler.getStructureTemplateManager()).thenReturn(manager);
        world = mock(World.class, withSettings().stubOnly().defaultAnswer(call -> {
            switch (call.getMethod().getName()) {
                case "getSeed":
                    return seed.get();
                case "getSaveHandler":
                    return saveHandler;
                default:
                    throw new AssertionError("Unexpected world access: " + call.getMethod());
            }
        }));

        Field worldField = MapGenBase.class.getDeclaredField("world");
        worldField.setAccessible(true);
        for (DivineStructure structure : DivineStructure.values()) {
            Class<?> type = loadClass("divinerpg.structure." + TYPES[structure.ordinal()]);
            Object generator = structure.layer == 0
                    ? type.getConstructor(World.class).newInstance(world)
                    : type.getConstructor(World.class, int.class).newInstance(world, naturalAltitude(type));
            worldField.set(generator, world);
            generators[structure.ordinal()] = (MapGenStructure) generator;
        }

        canSpawn = MapGenStructure.class.getDeclaredMethod("canSpawnStructureAtCoords", int.class, int.class);
        canSpawn.setAccessible(true);
        createStart = MapGenStructure.class.getDeclaredMethod("getStructureStart", int.class, int.class);
        createStart.setAccessible(true);
    }

    void seed(long value) {
        seed.set(value);
    }

    boolean canSpawn(DivineStructure structure, int x, int z) throws Exception {
        return (Boolean) canSpawn.invoke(generators[structure.ordinal()], x, z);
    }

    StructureStart start(DivineStructure structure, int x, int z) throws Exception {
        return (StructureStart) createStart.invoke(generators[structure.ordinal()], x, z);
    }

    private static int naturalAltitude(Class<?> type) throws Exception {
        ClassNode node = InstalledModClasses.read(JAR, "divinerpg/dimensions/vethea/VetheaChunkGenerator");
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.name.equals("<init>") && call.owner.equals(type.getName().replace('.', '/'))) {
                        return ((IntInsnNode) instruction.getPrevious()).operand;
                    }
                }
            }
        }
        throw new AssertionError("No natural generation entry: " + type);
    }

    private static Template readTemplate(ResourceLocation location) throws Exception {
        try (JarFile jar = new JarFile(Paths.get(System.getProperty("compatModsDir"), JAR).toFile());
                InputStream input = jar.getInputStream(jar.getJarEntry(
                        "assets/divinerpg/structures/" + location.getPath() + ".nbt"))) {
            Template template = new Template();
            template.read(CompressedStreamTools.readCompressed(input));
            return template;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            ClassNode node = InstalledModClasses.read(JAR, name.replace('.', '/'));
            node.methods.removeIf(method -> !METHODS.contains(method.name));
            if (name.endsWith("Component")) {
                MethodNode marker = new MethodNode(Opcodes.ACC_PROTECTED, "handleDataMarker",
                        "(Ljava/lang/String;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/World;"
                                + "Ljava/util/Random;Lnet/minecraft/world/gen/structure/StructureBoundingBox;)V",
                        null, null);
                marker.instructions.add(new InsnNode(Opcodes.RETURN));
                node.methods.add(marker);
            }
            ClassWriter original = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(original);
            ClassWriter writer = new ClassWriter(0);
            new ClassReader(original.toByteArray()).accept(new ClassRemapper(writer, new Remapper() {

                @Override
                public String mapMethodName(String owner, String method, String descriptor) {
                    return mappedName(method);
                }

                @Override
                public String mapFieldName(String owner, String field, String descriptor) {
                    return mappedName(field);
                }
            }), ClassReader.EXPAND_FRAMES);
            byte[] bytes = writer.toByteArray();
            return defineClass(name, bytes, 0, bytes.length);
        } catch (Exception exception) {
            throw new ClassNotFoundException(name, exception);
        }
    }

    private static String mappedName(String name) {
        switch (name) {
            case "func_143025_a": return "getStructureName";
            case "func_75047_a": return "canSpawnStructureAtCoords";
            case "func_75049_b": return "getStructureStart";
            case "func_72860_G": return "getSaveHandler";
            case "func_186340_h": return "getStructureTemplateManager";
            case "func_72905_C": return "getSeed";
            case "func_75072_c": return "updateBoundingBox";
            case "func_110624_b": return "getNamespace";
            case "func_110623_a": return "getPath";
            case "func_180331_a": return "getBlock";
            case "func_186222_a": return "setIgnoreEntities";
            case "func_186225_a": return "setReplacedBlock";
            case "func_186220_a": return "setRotation";
            case "func_186237_a": return "getTemplate";
            case "func_186173_a": return "setup";
            case "field_75038_b": return "rand";
            case "field_75039_c": return "world";
            case "field_75075_a": return "components";
            case "field_150350_a": return "AIR";
            case "field_186177_b": return "placeSettings";
            default: return name;
        }
    }
}

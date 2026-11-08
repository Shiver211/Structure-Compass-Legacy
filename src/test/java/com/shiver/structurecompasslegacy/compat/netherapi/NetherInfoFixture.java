package com.shiver.structurecompasslegacy.compat.netherapi;

import java.util.HashMap;
import java.util.Map;

import com.shiver.structurecompasslegacy.compat.InstalledModClasses;
import com.shiver.structurecompasslegacy.simulation.WorldInfoCopies;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSavedData;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

/** 直接执行目标 JAR 的世界信息类；只将 SRG 方法名映射到测试环境的 MCP 名称。 */
final class NetherInfoFixture {

    private static final String INFO = "git.jbredwards.nether_api.mod.common.world.DerivedWorldInfoMulti";
    private final Class<? extends WorldInfo> infoType;
    private final Class<?> dataType;

    NetherInfoFixture() throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        classes.put(INFO, mappedClass(INFO));
        classes.put(INFO + "$Data", mappedClass(INFO + "$Data"));
        ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = classes.get(name);
                if (bytes == null) {
                    throw new ClassNotFoundException(name);
                }
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
        infoType = loader.loadClass(INFO).asSubclass(WorldInfo.class);
        dataType = loader.loadClass(INFO + "$Data");
        NetherWorldInfo adapter = new NetherWorldInfo(infoType, dataType);
        WorldInfoCopies.register(infoType, adapter::snapshot);
    }

    WorldInfo info(WorldInfo parent, World owner, int dimension) throws Exception {
        return infoType.getConstructor(WorldInfo.class, World.class, int.class).newInstance(parent, owner, dimension);
    }

    WorldSavedData spawn(int dimension) throws Exception {
        Object data = dataType.getConstructor(String.class).newInstance("nether_api:spawn_point_data_" + dimension);
        dataType.getField("initialized").setBoolean(data, true);
        dataType.getField("x").setInt(data, 123);
        dataType.getField("y").setInt(data, 67);
        dataType.getField("z").setInt(data, -456);
        return (WorldSavedData) data;
    }

    private static byte[] mappedClass(String name) throws Exception {
        ClassNode node = InstalledModClasses.read("Nether-API-v1.4.5-mc1.12.2.jar", name.replace('.', '/'));
        Map<String, String> methods = new HashMap<>();
        String[][] names = {
                {"func_76082_a", "cloneNBTCompound"}, {"func_76070_v", "isInitialized"},
                {"func_76091_d", "setServerInitialized"}, {"func_76079_c", "getSpawnX"},
                {"func_76075_d", "getSpawnY"}, {"func_76074_e", "getSpawnZ"},
                {"func_176143_a", "setSpawn"}, {"func_72943_a", "loadData"},
                {"func_72823_a", "setData"}, {"func_76184_a", "readFromNBT"},
                {"func_189551_b", "writeToNBT"}, {"func_76185_a", "markDirty"},
                {"func_74757_a", "setBoolean"}, {"func_74768_a", "setInteger"},
                {"func_74767_n", "getBoolean"}, {"func_74762_e", "getInteger"},
                {"func_177958_n", "getX"}, {"func_177956_o", "getY"}, {"func_177952_p", "getZ"}
        };
        for (String[] entry : names) {
            methods.put(entry[0], entry[1]);
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(new ClassRemapper(writer, new Remapper() {

            @Override
            public String mapMethodName(String owner, String method, String descriptor) {
                return methods.getOrDefault(method, method);
            }
        }));
        return writer.toByteArray();
    }
}

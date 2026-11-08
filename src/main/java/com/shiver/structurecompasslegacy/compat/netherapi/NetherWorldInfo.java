package com.shiver.structurecompasslegacy.compat.netherapi;

import java.lang.reflect.Field;
import java.util.Map;

import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.simulation.WorldInfoCopies;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/** Nether API 的序列化和出生点 getter 都会调用 Data.load；只复制已经加载的出生点状态。 */
final class NetherWorldInfo {

    private final Field parent;
    private final Field owner;
    private final Field dimension;
    private final Field loadedData;
    private final Field initialized;
    private final Field x;
    private final Field y;
    private final Field z;

    NetherWorldInfo(Class<? extends WorldInfo> infoType, Class<?> dataType) {
        parent = ReflectionHelper.findField(DerivedWorldInfo.class, new String[] {"delegate", "field_76115_a"});
        owner = ReflectionHelper.findField(infoType, new String[] {"delegate", "field_76115_a"});
        dimension = ReflectionHelper.findField(infoType, new String[] {"dimension", "field_76105_j"});
        loadedData = ReflectionHelper.findField(MapStorage.class, new String[] {"loadedDataMap", "field_75749_b"});
        initialized = ReflectionHelper.findField(dataType, "initialized");
        x = ReflectionHelper.findField(dataType, "x");
        y = ReflectionHelper.findField(dataType, "y");
        z = ReflectionHelper.findField(dataType, "z");
    }

    NBTTagCompound snapshot(WorldInfo source) {
        WorldInfo delegate = (WorldInfo) ModFields.read(parent, source);
        NBTTagCompound snapshot = WorldInfoCopies.copy(delegate).cloneNBTCompound(null);
        int id = (Integer) ModFields.read(dimension, source);
        if (id == 0) {
            return snapshot;
        }

        World world = (World) ModFields.read(owner, source);
        Map<?, ?> entries = (Map<?, ?>) ModFields.read(loadedData, world.getMapStorage());
        Object spawn = entries.get("nether_api:spawn_point_data_" + id);
        if (spawn == null) {
            // 不用默认出生点掩盖缺失数据，也不触发原方法的磁盘加载或记录创建。
            throw new UnsupportedOperationException("Nether API spawn data is not loaded for dimension " + id);
        }

        snapshot.setBoolean("initialized", (Boolean) ModFields.read(initialized, spawn));
        snapshot.setInteger("SpawnX", (Integer) ModFields.read(x, spawn));
        snapshot.setInteger("SpawnY", (Integer) ModFields.read(y, spawn));
        snapshot.setInteger("SpawnZ", (Integer) ModFields.read(z, spawn));
        return snapshot;
    }
}

package com.shiver.structurecompasslegacy.verification;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import static com.shiver.structurecompasslegacy.verification.WorldStateSnapshot.require;

/** 只读取已审核字段；不调用未知模组 WorldSavedData.writeToNBT。 */
final class SharedStateSnapshot {

    private static final Field DATA = field(MapStorage.class, "loadedDataMap", "field_75749_b");
    private static final Field IDS = field(MapStorage.class, "idCounts", "field_75748_d");

    private static final Field ENTITY_IDS = field(Entity.class, "nextEntityID", "field_70152_a");
    private static final Field TICK_IDS = field(NextTickListEntry.class, "nextTickEntryID", "field_77177_f");

    // 仅用于独立测试 JVM；这里没有游戏客户端并发分配 ID。
    private final Object entityIds = read(ENTITY_IDS, null);
    private final Object tickIds = read(TICK_IDS, null);
    private final boolean falling = BlockFalling.fallInstantly;
    private final Map<Biome, IBlockState[]> terrain = new IdentityHashMap<>();
    private final StorageState[] storage;

    SharedStateSnapshot(MapStorage global, MapStorage dimension) {
        for (Biome biome : Biome.REGISTRY) {
            terrain.put(biome, new IBlockState[] {biome.topBlock, biome.fillerBlock});
        }
        storage = new StorageState[] {new StorageState(global), new StorageState(dimension)};
    }

    void verify() {
        require(entityIds.equals(read(ENTITY_IDS, null)), "real entity ID counter changed");
        require(tickIds.equals(read(TICK_IDS, null)), "real block-tick ID counter changed");
        require(falling == BlockFalling.fallInstantly, "global falling-block mode changed");
        terrain.forEach((biome, states) -> require(biome.topBlock == states[0] && biome.fillerBlock == states[1],
                "shared biome surface changed: " + biome.getRegistryName()));
        for (StorageState state : storage) {
            state.verify();
        }
    }

    private static Object read(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot inspect " + field.getName(), exception);
        }
    }

    private static Field field(Class<?> owner, String... names) {
        return ReflectionHelper.findField(owner, names);
    }

    private static final class StorageState {

        private final MapStorage owner;
        private final Map<?, ?> entries;
        private final Map<?, ?> ids;
        private final Map<WorldSavedData, Boolean> dirty = new IdentityHashMap<>();
        private final Map<MapGenStructureData, NBTTagCompound> structures = new IdentityHashMap<>();

        private StorageState(MapStorage owner) {
            this.owner = owner;
            entries = new HashMap<>((Map<?, ?>) read(DATA, owner));
            ids = new HashMap<>((Map<?, ?>) read(IDS, owner));
            for (Object value : entries.values()) {
                WorldSavedData data = (WorldSavedData) value;
                dirty.put(data, data.isDirty());
                if (data.getClass() == MapGenStructureData.class) {
                    MapGenStructureData structure = (MapGenStructureData) data;
                    structures.put(structure, structure.getTagCompound().copy());
                }
            }
        }

        private void verify() {
            require(entries.equals(read(DATA, owner)), "real saved-data entries changed");
            require(ids.equals(read(IDS, owner)), "real saved-data IDs changed");
            dirty.forEach((data, wasDirty) -> require(data.isDirty() == wasDirty, "real saved-data dirty flag changed"));
            structures.forEach((data, nbt) -> require(nbt.equals(data.getTagCompound()), "real structure data changed"));
        }
    }
}

package com.shiver.structurecompasslegacy.api;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public final class DimensionRule implements Predicate<WorldServer> {

    private static final Field TYPE_PROVIDER_CLASS = findProviderClassField();
    private static final DimensionRule NONE = new DimensionRule(Kind.NONE, new int[0], "");

    private enum Kind {
        NONE,
        IDS,
        PROVIDER
    }

    private final Kind kind;
    private final int[] ids;
    private final String providerClass;

    private DimensionRule(Kind kind, int[] ids, String providerClass) {
        this.kind = kind;
        this.ids = ids;
        this.providerClass = providerClass;
    }

    public static DimensionRule none() {
        return NONE;
    }

    public static DimensionRule ofIds(int... ids) {
        if (ids == null || ids.length == 0) {
            return new DimensionRule(Kind.IDS, new int[0], "");
        }

        List<Integer> unique = new ArrayList<>();
        for (int id : ids) {
            if (!unique.contains(id)) {
                unique.add(id);
            }
        }
        int[] copy = new int[unique.size()];
        for (int index = 0; index < unique.size(); index++) {
            copy[index] = unique.get(index);
        }
        return new DimensionRule(Kind.IDS, copy, "");
    }

    public static DimensionRule ofProvider(String providerClass) {
        if (providerClass == null || providerClass.isEmpty()) {
            return NONE;
        }
        return new DimensionRule(Kind.PROVIDER, new int[0], providerClass);
    }

    @Override
    public boolean test(WorldServer world) {
        if (world == null) {
            return false;
        }
        if (kind == Kind.IDS) {
            int dimension = world.provider.getDimension();
            for (int id : ids) {
                if (id == dimension) {
                    return true;
                }
            }
            return false;
        }
        if (kind == Kind.PROVIDER) {
            return world.provider.getClass().getName().equals(providerClass);
        }
        return false;
    }

    public NBTTagList toNbt() {
        NBTTagList result = new NBTTagList();
        if (kind == Kind.IDS) {
            for (int id : ids) {
                if (DimensionManager.isDimensionRegistered(id)) {
                    DimensionType type = DimensionManager.getProviderType(id);
                    result.appendTag(dimensionTag(true, id, type.getName(), providerClassFor(id, type)));
                } else {
                    result.appendTag(dimensionTag(true, id, "", ""));
                }
            }
        } else if (kind == Kind.PROVIDER) {
            Integer[] registered = DimensionManager.getStaticDimensionIDs();
            Arrays.sort(registered);
            boolean matched = false;
            for (int id : registered) {
                DimensionType type = DimensionManager.getProviderType(id);
                if (providerClass.equals(providerClassFor(id, type))) {
                    result.appendTag(dimensionTag(true, id, type.getName(), providerClass));
                    matched = true;
                }
            }
            if (!matched) {
                result.appendTag(dimensionTag(false, 0, "", providerClass));
            }
        }
        return result;
    }

    private static NBTTagCompound dimensionTag(boolean hasId, int id, String typeName, String provider) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("hasId", hasId);
        tag.setInteger("id", id);
        tag.setString("typeName", typeName == null ? "" : typeName);
        tag.setString("provider", provider == null ? "" : provider);
        return tag;
    }

    private static String providerClassFor(int dimId, DimensionType type) {
        WorldServer loaded = DimensionManager.getWorld(dimId);
        if (loaded != null) {
            return loaded.provider.getClass().getName();
        }
        if (TYPE_PROVIDER_CLASS != null) {
            try {
                Object value = TYPE_PROVIDER_CLASS.get(type);
                if (value instanceof Class<?>) {
                    return ((Class<?>) value).getName();
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return "";
    }

    private static Field findProviderClassField() {
        for (Field field : DimensionType.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType() == Class.class) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }
}

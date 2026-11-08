package com.shiver.structurecompasslegacy.compat;

import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public final class ModFields {

    private ModFields() {
    }

    public static Field bind(String type, String name) {
        try {
            return Class.forName(type).getField(name);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot bind field: " + type + "." + name, exception);
        }
    }

    public static Object read(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read " + field, exception);
        }
    }

    public static IntSupplier integer(String type, String name) {
        Field field = bind(type, name);
        return () -> ((Number) read(field, null)).intValue();
    }

    public static BooleanSupplier flag(String type, String name) {
        Field field = bind(type, name);
        return () -> (Boolean) read(field, null);
    }
}

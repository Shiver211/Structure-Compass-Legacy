package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.shiver.structurecompasslegacy.compat.ModConstructor;

final class BetweenlandsReflection {

    static Field field(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot bind Betweenlands field: " + type.getName() + "." + name, exception);
        }
    }

    static Object read(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read Betweenlands field: " + field, exception);
        }
    }

    static Object readSetting(Object owner, String name) {
        return read(field(owner.getClass(), name), owner);
    }

    static void write(Field field, Object owner, Object value) {
        try {
            field.set(owner, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot write Betweenlands field: " + field, exception);
        }
    }

    static Method method(String type, String name, Class<?>... parameters) {
        try {
            Method method = ModConstructor.type(type).getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot bind Betweenlands method: " + type + "." + name, exception);
        }
    }

    static Object call(Method method, Object owner, Object... arguments) {
        try {
            return method.invoke(owner, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Betweenlands generation failed", cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot call Betweenlands method: " + method, exception);
        }
    }

    static Object construct(Class<?> type, Class<?>[] parameters, Object... arguments) {
        try {
            return type.getConstructor(parameters).newInstance(arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot create private Betweenlands generator: " + type.getName(), exception);
        }
    }

    private BetweenlandsReflection() {
    }
}

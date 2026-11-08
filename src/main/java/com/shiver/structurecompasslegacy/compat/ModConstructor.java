package com.shiver.structurecompasslegacy.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class ModConstructor<T> {

    private final Constructor<? extends T> constructor;

    private ModConstructor(Constructor<? extends T> constructor) {
        this.constructor = constructor;
    }

    public static <T> ModConstructor<T> bind(String name, Class<T> type, Class<?>... parameters) {
        try {
            Class<? extends T> implementation = type(name).asSubclass(type);
            return new ModConstructor<>(implementation.getConstructor(parameters));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot bind constructor: " + name, exception);
        }
    }

    public static Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Missing mod class: " + name, exception);
        }
    }

    public Class<?> type() {
        return constructor.getDeclaringClass();
    }

    public T create(Object... arguments) {
        try {
            return constructor.newInstance(arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("Mod constructor failed: " + constructor.getDeclaringClass().getName(), cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot construct " + constructor.getDeclaringClass().getName(), exception);
        }
    }
}

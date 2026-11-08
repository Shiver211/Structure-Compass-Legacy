package com.shiver.structurecompasslegacy.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** 绑定一个经过模块审核的公开方法。 */
public final class ModMethod {

    private final Method method;

    private ModMethod(Method method) {
        this.method = method;
    }

    public static ModMethod bind(Class<?> owner, String name, Class<?>... parameters) {
        try {
            return new ModMethod(owner.getMethod(name, parameters));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot bind method: " + owner.getName() + "." + name, exception);
        }
    }

    public Object call(Object owner, Object... arguments) {
        try {
            return method.invoke(owner, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("Mod method failed: " + method, cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot invoke " + method, exception);
        }
    }
}

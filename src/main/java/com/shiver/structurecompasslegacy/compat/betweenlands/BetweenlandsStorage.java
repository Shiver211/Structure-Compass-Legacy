package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

final class BetweenlandsStorage {

    private static final String STORAGE = "thebetweenlands.common.world.storage.";
    private static final Class<?> BASE = ModConstructor.type(STORAGE + "WorldStorageImpl");
    private static final Class<?> LOCAL_HANDLER = ModConstructor.type("thebetweenlands.api.storage.ILocalStorageHandler");
    private static final ModConstructor<Object> FACTORY = ModConstructor.bind(STORAGE + "BetweenlandsWorldStorage", Object.class);
    private static final ModConstructor<Object> ENVIRONMENT = ModConstructor.bind(
            "thebetweenlands.common.world.event.BLEnvironmentEventRegistry", Object.class, World.class);
    private static final Field CAPABILITY = BetweenlandsReflection.field(BASE, "CAPABILITY_INSTANCE");
    private static final Field WORLD = BetweenlandsReflection.field(BASE, "world");
    private static final Field LOCAL_STORAGE = BetweenlandsReflection.field(BASE, "localStorageHandler");
    private static final Field ENVIRONMENT_EVENTS = BetweenlandsReflection.field(FACTORY.type(), "environmentEventRegistry");

    @SuppressWarnings("unchecked")
    static void attach(SimulationWorld world) {
        Capability<Object> capability = (Capability<Object>) BetweenlandsReflection.read(CAPABILITY, null);
        world.setSimulationCapability(capability, create(world));
    }

    static Object create(World world) {
        Object storage = FACTORY.create();
        Object handler = Proxy.newProxyInstance(LOCAL_HANDLER.getClassLoader(), new Class<?>[]{LOCAL_HANDLER},
                (proxy, method, arguments) -> localStorageCall(storage, method));
        BetweenlandsReflection.write(WORLD, storage, world);
        BetweenlandsReflection.write(LOCAL_STORAGE, storage, handler);
        // 仅供 provider 的环境查询；不初始化默认事件、不广播、不执行天气 tick。
        BetweenlandsReflection.write(ENVIRONMENT_EVENTS, storage, ENVIRONMENT.create(world));
        return storage;
    }

    private static Object localStorageCall(Object storage, Method method) {
        // 前置结构只提交区域标记，不查询该标记；候选结束后随 capability 一起释放。
        switch (method.getName()) {
            case "getWorldStorage":
                return storage;
            case "addLocalStorage":
                return true;
            case "getLocalStorages":
            case "getLoadedStorages":
                return Collections.emptyList();
            case "getLocalStorage":
                return null;
            default:
                throw new UnsupportedOperationException("Unaudited Betweenlands storage call: " + method);
        }
    }

    private BetweenlandsStorage() {
    }
}

package com.shiver.structurecompasslegacy.compat.netherapi;

import java.lang.reflect.Field;

import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.SimulationSession;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSavedData;

import static org.mockito.Mockito.mock;

/** 在实际存储保护 Mixin 生效时重现本次异常，再通过相同的会话构造入口验证修复。 */
public final class NetherInfoRuntimeCheck {

    private NetherInfoRuntimeCheck() {
    }

    public static void verify(WorldServer source) throws Exception {
        if (System.getProperty("compatModsDir") == null) {
            System.out.println("NETHER_WORLD_INFO_SKIPPED: supply compatModsDir for the installed-class check");
            return;
        }
        NetherInfoFixture fixture = new NetherInfoFixture();
        WorldInfo originalInfo = source.getWorldInfo();
        MapStorage originalStorage = source.getMapStorage();
        MapStorage persistent = new MapStorage(mock(ISaveHandler.class));
        WorldSavedData spawn = fixture.spawn(1);
        persistent.setData(spawn.mapName, spawn);
        NBTTagCompound before = spawn.writeToNBT(new NBTTagCompound());
        WorldInfo derived = fixture.info(originalInfo, source, 1);
        try {
            set(source, "mapStorage", persistent);
            set(source, "worldInfo", derived);
            verifyOriginalFailure(derived);
            try (SimulationSession session = new SimulationSession(source)) {
                check(session.world().getWorldInfo().getSpawnX() == 123, "spawn X was lost");
                check(session.world().getWorldInfo().getSpawnZ() == -456, "spawn Z was lost");
                session.beginCandidate();
                check(session.world().getWorldInfo().getSpawnY() == 67, "candidate reset lost spawn Y");
                check(session.world().getWorldInfo().getSeed() == originalInfo.getSeed(), "seed changed");
                check(session.generatedChunks() == 0, "copy generated terrain");
            }
            check(source.getChunkProvider().getLoadedChunkCount() == 0, "copy loaded real chunks");
            check(before.equals(spawn.writeToNBT(new NBTTagCompound())), "real spawn data changed");
            check(!spawn.isDirty(), "real spawn data marked dirty");
            System.out.println("NETHER_WORLD_INFO_PASSED: native failure reproduced, session copied, reset and closed");
        } finally {
            set(source, "worldInfo", originalInfo);
            set(source, "mapStorage", originalStorage);
        }
    }

    private static void verifyOriginalFailure(WorldInfo derived) {
        try (SimulationScope ignored = SimulationScope.enter()) {
            try {
                derived.cloneNBTCompound(null);
            } catch (UnsupportedOperationException expected) {
                check(expected.getMessage().equals("Simulation cannot access persistent storage"), "wrong native failure");
                return;
            }
        }
        throw new AssertionError("The original Nether API serializer did not reach the persistent-storage guard");
    }

    private static void set(World source, String name, Object value) throws Exception {
        Field field = World.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(source, value);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

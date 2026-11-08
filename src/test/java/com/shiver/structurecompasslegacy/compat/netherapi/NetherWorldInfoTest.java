package com.shiver.structurecompasslegacy.compat.netherapi;

import java.lang.reflect.Field;

import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import com.shiver.structurecompasslegacy.simulation.WorldInfoCopies;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.CompoundDataFixer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.IFMLSidedHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;

class NetherWorldInfoTest {

    private static Field sidedDelegate;
    private static Object previousDelegate;

    @BeforeAll
    static void initializeMinecraft() throws Exception {
        Bootstrap.register();
        sidedDelegate = FMLCommonHandler.class.getDeclaredField("sidedDelegate");
        sidedDelegate.setAccessible(true);
        previousDelegate = sidedDelegate.get(FMLCommonHandler.instance());
        IFMLSidedHandler host = mock(IFMLSidedHandler.class);
        when(host.getDataFixer()).thenReturn(new CompoundDataFixer(new DataFixer(1343)));
        sidedDelegate.set(FMLCommonHandler.instance(), host);
    }

    @AfterAll
    static void restoreForgeHost() throws Exception {
        sidedDelegate.set(FMLCommonHandler.instance(), previousDelegate);
    }

    @Test
    void nativeDerivedInfoCopiesSeedOptionsAndSpawnWithoutLoadingPersistentData() throws Exception {
        NetherInfoFixture fixture = new NetherInfoFixture();
        MapStorage storage = new MapStorage(mock(ISaveHandler.class));
        WorldSavedData spawn = fixture.spawn(1);
        storage.setData(spawn.mapName, spawn);
        World world = rejectingWorld(storage);
        WorldInfo parent = parent();
        WorldInfo derived = fixture.info(parent, world, 1);
        NBTTagCompound before = spawn.writeToNBT(new NBTTagCompound());

        // 原始方法确实进入 World.loadData；修复后的复制不能触发它。
        assertThrows(AssertionError.class, () -> derived.cloneNBTCompound(null));
        try (SimulationScope ignored = SimulationScope.enter()) {
            WorldInfo copy = WorldInfoCopies.copy(derived);
            assertEquals(WorldInfo.class, copy.getClass());
            assertEquals(parent.getSeed(), copy.getSeed());
            assertEquals(parent.getGeneratorOptions(), copy.getGeneratorOptions());
            assertEquals(WorldType.LARGE_BIOMES, copy.getTerrainType());
            assertEquals(123, copy.getSpawnX());
            assertEquals(67, copy.getSpawnY());
            assertEquals(-456, copy.getSpawnZ());
            assertTrue(copy.isInitialized());
            copy.getGameRulesInstance().setOrCreateGameRule("doDaylightCycle", "false");
            copy.getDimensionData(1).setInteger("probe", 99);
        }
        assertTrue(parent.getGameRulesInstance().getBoolean("doDaylightCycle"));
        assertEquals(7, parent.getDimensionData(1).getInteger("probe"));
        assertEquals(before, spawn.writeToNBT(new NBTTagCompound()));
        assertFalse(spawn.isDirty());
    }

    @Test
    void missingSpawnDataIsRejectedWithoutReadingOrCreatingIt() throws Exception {
        NetherInfoFixture fixture = new NetherInfoFixture();
        World world = rejectingWorld(new MapStorage(mock(ISaveHandler.class)));
        WorldInfo derived = fixture.info(parent(), world, 1);
        try (SimulationScope ignored = SimulationScope.enter()) {
            UnsupportedOperationException failure = assertThrows(
                    UnsupportedOperationException.class, () -> WorldInfoCopies.copy(derived));
            assertTrue(failure.getMessage().contains("spawn data is not loaded"));
        }
    }

    @Test
    void dimensionZeroUsesTheParentWithoutAccessingSpawnStorage() throws Exception {
        NetherInfoFixture fixture = new NetherInfoFixture();
        WorldInfo parent = parent();
        WorldInfo copy = WorldInfoCopies.copy(fixture.info(parent, rejectingWorld(null), 0));
        assertEquals(parent.getSeed(), copy.getSeed());
        assertEquals(parent.getSpawnX(), copy.getSpawnX());
    }

    private static WorldInfo parent() {
        WorldSettings settings = new WorldSettings(-901234567890123L, GameType.SURVIVAL, true, false, WorldType.LARGE_BIOMES);
        settings.setGeneratorOptions("{\"coordinateScale\":684.412}");
        WorldInfo parent = new WorldInfo(settings, "fixture");
        NBTTagCompound dimension = new NBTTagCompound();
        dimension.setInteger("probe", 7);
        parent.setDimensionData(1, dimension);
        return parent;
    }

    private static World rejectingWorld(MapStorage storage) {
        return mock(World.class, withSettings().defaultAnswer(call -> {
            if (call.getMethod().getName().equals("getMapStorage")) {
                return storage;
            }
            throw new AssertionError("Snapshot called a real-world method: " + call.getMethod().getName());
        }));
    }
}

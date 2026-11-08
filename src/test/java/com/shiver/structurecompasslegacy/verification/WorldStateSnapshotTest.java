package com.shiver.structurecompasslegacy.verification;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.MapStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldStateSnapshotTest {

    @BeforeAll
    static void initializeMinecraft() {
        Bootstrap.register();
    }

    @Test
    void serializationTimeDoesNotReportAMutationOrConsumeWorldRandom() throws Exception {
        Fixture fixture = new Fixture();
        WorldStateSnapshot snapshot = new WorldStateSnapshot(fixture.world);
        assertDoesNotThrow(snapshot::close);
        assertEquals(2L, fixture.serializations.get());
        assertTrue(fixture.data.hasKey("LastPlayed"), "Normalization must not edit the source tag");
        assertEquals(new Random(42).nextLong(), fixture.world.rand.nextLong());
    }

    @Test
    void realChangesRemainVisibleWithoutLoggingTheirValues() throws Exception {
        Fixture fixture = new Fixture();
        NBTTagCompound rules = new NBTTagCompound();
        rules.setString("doDaylightCycle", "true");
        fixture.data.setTag("GameRules", rules);
        WorldStateSnapshot snapshot = new WorldStateSnapshot(fixture.world);
        fixture.data.setLong("Time", 200);
        rules.setString("doDaylightCycle", "false");
        IllegalStateException failure = assertThrows(IllegalStateException.class, snapshot::close);
        assertEquals("Foundation verification: real WorldInfo changed: GameRules, Time", failure.getMessage());
    }

    @Test
    void snapshotFailureDoesNotHideTheOriginalGenerationFailure() throws Exception {
        Fixture fixture = new Fixture();
        UnsupportedOperationException original = new UnsupportedOperationException("generator not adapted");
        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class, () -> {
            try (WorldStateSnapshot snapshot = new WorldStateSnapshot(fixture.world)) {
                fixture.data.setLong("Time", 100);
                throw original;
            }
        });
        assertSame(original, failure);
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("Foundation verification: real WorldInfo changed: Time",
                failure.getSuppressed()[0].getMessage());
    }

    private static final class Fixture {

        private final WorldServer world = mock(WorldServer.class);
        private final NBTTagCompound data = new NBTTagCompound();
        private final AtomicLong serializations = new AtomicLong();

        private Fixture() throws Exception {
            when(world.getMapStorage()).thenReturn(new MapStorage(null));
            when(world.getPerWorldStorage()).thenReturn(new MapStorage(null));
            setField("provider", new WorldProviderSurface());
            setField("rand", new Random(42));
            setField("loadedEntityList", new ArrayList<>());
            ChunkProviderServer chunks = mock(ChunkProviderServer.class);
            when(chunks.getLoadedChunks()).thenReturn(Collections.emptyList());
            when(world.getChunkProvider()).thenReturn(chunks);
            WorldInfo info = mock(WorldInfo.class);
            when(world.getWorldInfo()).thenReturn(info);
            when(info.cloneNBTCompound(isNull())).thenAnswer(call -> {
                // 模拟原版序列化时写入新的系统时间，不依赖测试机器的计时精度。
                data.setLong("LastPlayed", serializations.incrementAndGet());
                return data;
            });
        }

        private void setField(String name, Object value) throws Exception {
            Field field = World.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(world, value);
        }
    }
}

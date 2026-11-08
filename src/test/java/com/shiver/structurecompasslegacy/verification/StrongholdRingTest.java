package com.shiver.structurecompasslegacy.verification;

import java.lang.reflect.Field;

import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StrongholdRingTest {

    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    void incrementalCandidatesMatchEveryNativePositionAcrossSeedsAndWorldTypes() throws Exception {
        for (long seed : new long[] {0, 1, -17, 8675309}) {
            for (WorldType type : new WorldType[] {WorldType.DEFAULT, WorldType.LARGE_BIOMES}) {
                WorldInfo info = new WorldInfo(new WorldSettings(seed, GameType.SURVIVAL, true, false, type), "test");
                World world = mock(World.class);
                when(world.getSeed()).thenReturn(seed);
                when(world.getBiomeProvider()).thenReturn(new BiomeProvider(info));
                when(world.getPerWorldStorage()).thenReturn(new MapStorage(null));
                NativeStronghold reference = new NativeStronghold(world);
                reference.initialize();
                Field field = MapGenStronghold.class.getDeclaredField("structureCoords");
                field.setAccessible(true);
                ChunkPos[] expected = (ChunkPos[]) field.get(reference);

                StrongholdRing ring = new StrongholdRing(seed);
                BiomeProvider independentBiomes = new BiomeProvider(info);
                for (int i = 0; i < 128; i++) {
                    assertFalse(ring.complete());
                    ring.advance(independentBiomes);
                }
                assertTrue(ring.complete());
                for (ChunkPos position : expected) {
                    assertTrue(ring.intersects(position.x, position.z, position.x, position.z));
                }
                assertFalse(ring.intersects(0, 0, 0, 0));
            }
        }
    }

    private static final class NativeStronghold extends MapGenStronghold {
        private NativeStronghold(World world) {
            this.world = world;
        }

        private void initialize() {
            canSpawnStructureAtCoords(0, 0);
        }
    }
}

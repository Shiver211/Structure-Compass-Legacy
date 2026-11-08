package com.shiver.structurecompasslegacy.compat.divinerpg;

import com.shiver.structurecompasslegacy.api.SearchTarget;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureStart;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DivinePlacementTest {

    @BeforeAll
    static void initializeMinecraft() {
        Bootstrap.register();
    }

    @Test
    void probabilityAndLayoutMatchInstalledNaturalGenerators() throws Exception {
        DivineNativeFixture nativeCode = new DivineNativeFixture();
        for (long seed : new long[] {0L, 72931L, -1L, Long.MIN_VALUE, Long.MAX_VALUE}) {
            nativeCode.seed(seed);
            DivinePlacement prediction = new DivinePlacement(seed);
            for (DivineStructure structure : DivineStructure.values()) {
                int matches = 0;
                for (int x = -80; x <= 80; x++) {
                    for (int z = -80; z <= 80; z++) {
                        boolean expected = nativeCode.canSpawn(structure, x, z);
                        if (expected != prediction.canSpawn(structure, x, z)) {
                            throw new AssertionError(structure + " seed=" + seed + " chunk=" + x + "," + z);
                        }
                        if (expected) {
                            matches++;
                            SearchTarget target = prediction.layout(structure, x, z);
                            StructureStart start = nativeCode.start(structure, x, z);
                            assertTrue(start.isSizeableStructure());
                            assertArrayEquals(start.getBoundingBox().toNBTTagIntArray().getIntArray(),
                                    target.bounds.toNBTTagIntArray().getIntArray(), structure.name());
                            assertTrue(start.getComponents().get(0).getBoundingBox().isVecInside(target.position));
                        }
                    }
                }
                assertTrue(matches > 0, structure + " must include successful candidates");
            }
        }
    }

    @Test
    void acceptedTargetsHaveNoCompetingNativeLayoutInTheirPopulationWindows() throws Exception {
        DivineNativeFixture nativeCode = new DivineNativeFixture();
        nativeCode.seed(0L);
        DivinePlacement prediction = new DivinePlacement(0L);
        for (DivineStructure structure : DivineStructure.values()) {
            int accepted = 0;
            int rejected = 0;
            for (int x = -80; x <= 80 && (accepted < 4 || rejected < 1); x++) {
                for (int z = -80; z <= 80 && (accepted < 4 || rejected < 1); z++) {
                    if (!prediction.canSpawn(structure, x, z)) {
                        continue;
                    }
                    SearchTarget target = prediction.layout(structure, x, z);
                    if (prediction.overlapsNeighbour(structure, x, z, target.bounds)) {
                        rejected++;
                        continue;
                    }
                    accepted++;
                    checkNativeNeighbours(nativeCode, structure, x, z, target.bounds);
                }
            }
            assertTrue(accepted >= 4, structure + " must remain searchable");
            assertTrue(rejected >= 1, structure + " must exercise overlapping layouts");
        }
    }

    private static void checkNativeNeighbours(DivineNativeFixture nativeCode, DivineStructure structure,
            int chunkX, int chunkZ, StructureBoundingBox bounds) throws Exception {
        // 用原生布局和每个 populate 窗口作独立对照，包含边界、旋转和相邻区块起点。
        for (DivineStructure other : DivineStructure.values()) {
            if (other.layer != structure.layer) {
                continue;
            }
            for (int x = chunkX - 8; x <= chunkX + 8; x++) {
                for (int z = chunkZ - 8; z <= chunkZ + 8; z++) {
                    if (other == structure && x == chunkX && z == chunkZ) {
                        continue;
                    }
                    if (!nativeCode.canSpawn(other, x, z)) {
                        continue;
                    }
                    StructureBoundingBox neighbour = nativeCode.start(other, x, z).getBoundingBox();
                    for (int px = chunkX - 4; px <= chunkX + 4; px++) {
                        for (int pz = chunkZ - 4; pz <= chunkZ + 4; pz++) {
                            int minX = px * 16 + 8;
                            int minZ = pz * 16 + 8;
                            if (bounds.intersectsWith(minX, minZ, minX + 15, minZ + 15)) {
                                assertFalse(neighbour.intersectsWith(minX, minZ, minX + 15, minZ + 15));
                            }
                        }
                    }
                }
            }
        }
    }
}

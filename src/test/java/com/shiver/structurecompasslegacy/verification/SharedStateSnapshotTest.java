package com.shiver.structurecompasslegacy.verification;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedStateSnapshotTest {

    @BeforeAll
    static void initializeMinecraft() {
        Bootstrap.register();
    }

    @Test
    void detectsStructureNbtAndDirtyChanges() {
        MapStorage storage = new MapStorage(null);
        MapGenStructureData structure = new MapGenStructureData("fixture");
        storage.setData("fixture", structure);
        SharedStateSnapshot snapshot = snapshot(storage);
        structure.getTagCompound().setInteger("probe", 1);
        assertFailure(snapshot, "structure data changed");
        structure.getTagCompound().removeTag("probe");
        assertDoesNotThrow(snapshot::verify);
        structure.markDirty();
        assertFailure(snapshot, "dirty flag changed");
    }

    @Test
    void detectsSavedDataIdsAndEntriesWithoutSerializingModData() {
        MapStorage storage = new MapStorage(null);
        storage.setData("mod", new UnserializableData());
        SharedStateSnapshot snapshot = snapshot(storage);
        assertDoesNotThrow(snapshot::verify);
        storage.getUniqueDataId("probe");
        assertFailure(snapshot, "saved-data IDs changed");

        SharedStateSnapshot next = snapshot(storage);
        storage.setData("other", new UnserializableData());
        assertFailure(next, "saved-data entries changed");
    }

    @Test
    void detectsAndRestoresSharedBiomeChanges() {
        SharedStateSnapshot snapshot = snapshot(new MapStorage(null));
        IBlockState original = Biomes.PLAINS.topBlock;
        try {
            Biomes.PLAINS.topBlock = Blocks.GOLD_BLOCK.getDefaultState();
            assertFailure(snapshot, "shared biome surface changed");
        } finally {
            Biomes.PLAINS.topBlock = original;
        }
        assertDoesNotThrow(snapshot::verify);
    }

    private SharedStateSnapshot snapshot(MapStorage storage) {
        return new SharedStateSnapshot(storage, new MapStorage(null));
    }

    private void assertFailure(SharedStateSnapshot snapshot, String message) {
        IllegalStateException failure = assertThrows(IllegalStateException.class, snapshot::verify);
        assertTrue(failure.getMessage().contains(message), failure.getMessage());
    }

    private static final class UnserializableData extends WorldSavedData {

        private UnserializableData() {
            super("mod");
        }

        @Override
        public void readFromNBT(NBTTagCompound compound) {
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound compound) {
            throw new AssertionError("Unknown mod data must not be serialized by the verifier");
        }
    }
}

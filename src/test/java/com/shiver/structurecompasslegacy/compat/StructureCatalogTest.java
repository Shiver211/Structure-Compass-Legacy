package com.shiver.structurecompasslegacy.compat;

import com.shiver.structurecompasslegacy.api.SearchMethod;
import com.shiver.structurecompasslegacy.api.StructureDefinition;
import com.shiver.structurecompasslegacy.api.StructureRegistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureCatalogTest {

    @Test
    void unseededAurorianStructuresKeepTheirCatalogEntriesButCannotBeSearched() {
        StructureRegistry registry = new StructureRegistry();
        StructureCatalog.register(registry, modId -> modId.equals("theaurorian"));

        String[] structures = {"graveyard", "umbra_tower", "surface_ruin", "underground_ruin"};
        for (String id : structures) {
            StructureDefinition definition = registry.get("theaurorian:" + id);
            assertFalse(definition.hasLocator());
            assertNull(definition.selectRoute(null));
            assertEquals("structurecompasslegacy.limitation.aurorian_unseeded_random",
                    definition.unavailableReason(null));
        }
    }

    @Test
    void absentOptionalModsDoNotLoadTheirEntriesOrClasses() {
        StructureRegistry registry = new StructureRegistry();
        StructureCatalog.register(registry, modId -> false);

        assertEquals(11, registry.entries().size());
        assertNull(registry.get("atum:pyramid"));
        assertNull(registry.get("endexpansion:lamented_islands"));
        assertNull(registry.get("raids:pillager_outpost"));
        assertNull(registry.get("divinerpg:hive"));
        assertNull(registry.get("journey:corba_village"));
        assertNull(registry.get("thebetweenlands:floating_island"));
    }

    @Test
    void betweenlandsExclusionsAreAbsentAndPendingBuildingsKeepTheirReasons() {
        StructureRegistry registry = new StructureRegistry();
        StructureCatalog.register(registry, modId -> modId.equals("thebetweenlands"));
        assertEquals(27, registry.entries().size());
        for (String id : new String[] {"underground_ruins", "underwater_ruins", "spawner", "small_ruins"}) {
            assertNull(registry.get("thebetweenlands:" + id));
        }
        assertFalse(registry.get("thebetweenlands:spirit_tree").hasLocator());
        assertEquals("structurecompasslegacy.limitation.betweenlands_spirit_tree",
                registry.get("thebetweenlands:spirit_tree").unavailableReason(null));
    }

    @Test
    void orderDependentStructuresKeepTheirSpecificReason() {
        StructureRegistry registry = new StructureRegistry();
        StructureCatalog.register(registry, modId -> modId.equals("endexpansion") || modId.equals("raids"));
        assertFalse(registry.get("endexpansion:ash_tower").hasLocator());
        assertEquals("structurecompasslegacy.limitation.endexpansion_counter",
                registry.get("endexpansion:ash_tower").unavailableReason(null));
        assertEquals("structurecompasslegacy.limitation.raids_village_state",
                registry.get("raids:pillager_outpost").unavailableReason(null));
    }

    @Test
    void failedBindingsKeepNamesAndAnUnavailableReason() {
        StructureRegistry registry = new StructureRegistry();
        StructureCatalog.register(registry, modId -> modId.equals("aether_legacy"));
        registry.activate("aether_legacy:silver_dungeon", SearchMethod.PREDICTION,
                world -> true, context -> null, "", () -> true);

        // 模拟后续黄金地下城绑定失败；已绑定条目仍可用，其余条目仍有明确状态。
        assertEquals(14, registry.entries().size());
        assertTrue(registry.get("aether_legacy:silver_dungeon").supports(null));
        StructureDefinition failed = registry.get("aether_legacy:golden_dungeon");
        assertEquals("黄金地下城", failed.chineseName);
        assertEquals("structurecompasslegacy.limitation.binding_failed", failed.unavailableReason(null));
        assertFalse(failed.supports(null));
        assertEquals("structurecompasslegacy.limitation.aether_bronze",
                registry.get("aether_legacy:bronze_dungeon").unavailableReason(null));
    }
}

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
    void unseededAurorianStructuresRemainVisibleButCannotBeSearched() {
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

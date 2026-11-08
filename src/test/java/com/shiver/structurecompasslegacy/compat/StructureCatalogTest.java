package com.shiver.structurecompasslegacy.compat;

import com.shiver.structurecompasslegacy.api.SearchMethod;
import com.shiver.structurecompasslegacy.api.StructureDefinition;
import com.shiver.structurecompasslegacy.api.StructureRegistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureCatalogTest {

    @Test
    void unsupportedAndPendingStructuresAreNotDeclared() {
        StructureRegistry registry = new StructureRegistry();
        StructureCatalog.register(registry, modId -> true);

        assertEquals(61, registry.entries().size());
        String[] structures = {
                "aether_legacy:bronze_dungeon", "theaurorian:graveyard", "theaurorian:umbra_tower",
                "theaurorian:surface_ruin", "theaurorian:underground_ruin", "endexpansion:ash_tower",
                "raids:pillager_outpost", "netherized:bastion_remnant", "astralsorcery:ancient_shrine",
                "journey:corba_village", "da:high_city"
        };
        for (String id : structures) {
            assertNull(registry.get(id), id);
        }
        assertNotNull(registry.get("endexpansion:lamented_islands"));
        assertNotNull(registry.get("theaurorian:moon_temple"));
        assertNotNull(registry.get("da:nether_arena"));
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
        assertNull(registry.get("da:nether_arena"));
        assertNull(registry.get("AS_BattleTowers:surface_tower"));
    }

    @Test
    void failedBindingsKeepNamesAndAnUnavailableReason() {
        StructureRegistry registry = new StructureRegistry();
        StructureCatalog.register(registry, modId -> modId.equals("aether_legacy"));
        registry.activate("aether_legacy:silver_dungeon", SearchMethod.PREDICTION,
                world -> true, context -> null, "", () -> true);

        // 绑定失败的定义保留诊断信息，但网络目录仅发送有定位器的条目。
        assertEquals(13, registry.entries().size());
        assertTrue(registry.get("aether_legacy:silver_dungeon").supports(null));
        StructureDefinition failed = registry.get("aether_legacy:golden_dungeon");
        assertEquals("黄金地下城", failed.chineseName);
        assertEquals("structurecompasslegacy.limitation.binding_failed", failed.unavailableReason(null));
        assertFalse(failed.supports(null));
        assertNull(registry.get("aether_legacy:bronze_dungeon"));
    }
}

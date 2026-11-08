package com.shiver.structurecompasslegacy.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;

public final class StructureRegistry {

    private final Map<ResourceLocation, StructureDefinition> entries = new LinkedHashMap<>();

    public void register(StructureDefinition definition) {
        if (entries.putIfAbsent(definition.id, definition) != null) {
            throw new IllegalArgumentException("Duplicate structure: " + definition.id);
        }
    }

    public StructureDefinition get(String id) {
        return entries.get(new ResourceLocation(id));
    }

    public void activate(
            String id,
            SearchMethod method,
            Predicate<WorldServer> dimensions,
            StructureLocator locator,
            String limitation,
            BooleanSupplier enabled) {
        SearchRoute route = new SearchRoute(method, locator, limitation);
        activate(id, dimensions,
                method == SearchMethod.PREDICTION ? route : null,
                method == SearchMethod.SIMULATION ? route : null, enabled);
    }

    public void activate(
            String id,
            Predicate<WorldServer> dimensions,
            SearchRoute prediction,
            SearchRoute simulation,
            BooleanSupplier enabled) {
        StructureDefinition declared = get(id);
        if (declared == null) {
            throw new IllegalArgumentException("Undeclared structure: " + id);
        }
        if (declared.hasLocator()) {
            throw new IllegalArgumentException("Structure already bound: " + id);
        }

        entries.put(declared.id, new StructureDefinition(
                id, declared.modName, declared.englishName, declared.chineseName,
                dimensions, prediction, simulation, declared.limitation, enabled));
    }

    public Collection<StructureDefinition> entries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public void clear() {
        entries.clear();
    }
}

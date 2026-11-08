package com.shiver.structurecompasslegacy.compat;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

import com.shiver.structurecompasslegacy.api.DimensionRule;
import com.shiver.structurecompasslegacy.api.StructureLocator;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.search.MapStructureTask;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

import net.minecraft.world.gen.structure.MapGenStructure;

public final class ModuleRegistration {

    private final StructureRegistry registry;
    private final String modId;
    private final DimensionRule dimensions;

    public ModuleRegistration(StructureRegistry registry, String modId, String providerClass) {
        this(registry, modId, DimensionRule.ofProvider(providerClass));
    }

    public ModuleRegistration(StructureRegistry registry, String modId, int... dimensionIds) {
        this(registry, modId, DimensionRule.ofIds(dimensionIds));
    }

    public ModuleRegistration(StructureRegistry registry, String modId, DimensionRule dimensions) {
        this.registry = registry;
        this.modId = modId;
        this.dimensions = dimensions;
    }

    public void map(
            String id,
            Function<SimulationWorld, MapGenStructure> factory,
            boolean validateLayout) {
        prediction(id, MapStructureTask.locator(factory, null, validateLayout));
    }

    public void prediction(String id, StructureLocator locator) {
        prediction(id, locator, () -> true);
    }

    public void prediction(
            String id,
            StructureLocator locator,
            BooleanSupplier enabled) {
        prediction(id, locator, enabled, "");
    }

    public void prediction(
            String id,
            StructureLocator locator,
            BooleanSupplier enabled,
            String limitation) {
        registry.activate(modId + ":" + id, dimensions, locator, limitation, enabled);
    }
}

package com.shiver.structurecompasslegacy.compat;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.BooleanSupplier;

import com.shiver.structurecompasslegacy.api.SearchMethod;
import com.shiver.structurecompasslegacy.api.StructureLocator;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.search.MapStructureTask;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.MapGenStructure;

/** 将条目声明留在各模组包，公共层只处理一致的注册格式。 */
public final class ModuleRegistration {

    private final StructureRegistry registry;
    private final String modId;
    private final Predicate<WorldServer> dimensions;

    public ModuleRegistration(StructureRegistry registry, String modId, String providerClass) {
        this(registry, modId, world -> world.provider.getClass().getName().equals(providerClass));
    }

    public ModuleRegistration(StructureRegistry registry, String modId, Predicate<WorldServer> dimensions) {
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
        registry.activate(modId + ":" + id, SearchMethod.PREDICTION, dimensions, locator, limitation, enabled);
    }

    public void simulation(
            String id,
            StructureLocator locator,
            String limitation,
            BooleanSupplier enabled) {
        registry.activate(modId + ":" + id, SearchMethod.SIMULATION,
                dimensions, locator, "structurecompasslegacy.limitation." + limitation, enabled);
    }
}

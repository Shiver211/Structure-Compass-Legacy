package com.shiver.structurecompasslegacy.api;

import java.util.function.Predicate;
import java.util.function.BooleanSupplier;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;

public final class StructureDefinition {

    public final ResourceLocation id;
    public final String modName;
    public final String englishName;
    public final String chineseName;
    public final String limitation;
    public final Predicate<WorldServer> dimensions;
    public final SearchRoute prediction;
    public final SearchRoute simulation;
    private final BooleanSupplier enabled;

    public StructureDefinition(
            String id,
            String modName,
            String englishName,
            String chineseName,
            SearchMethod method,
            Predicate<WorldServer> dimensions,
            StructureLocator locator,
            String limitation) {
        this(id, modName, englishName, chineseName, method, dimensions, locator, limitation, () -> true);
    }

    public StructureDefinition(
            String id,
            String modName,
            String englishName,
            String chineseName,
            SearchMethod method,
            Predicate<WorldServer> dimensions,
            StructureLocator locator,
            String limitation,
            BooleanSupplier enabled) {
        this(id, modName, englishName, chineseName, dimensions,
                method == SearchMethod.PREDICTION && locator != null
                        ? new SearchRoute(method, locator, limitation) : null,
                method == SearchMethod.SIMULATION && locator != null
                        ? new SearchRoute(method, locator, limitation) : null,
                limitation, enabled);
    }

    public StructureDefinition(
            String id,
            String modName,
            String englishName,
            String chineseName,
            Predicate<WorldServer> dimensions,
            SearchRoute prediction,
            SearchRoute simulation,
            String limitation,
            BooleanSupplier enabled) {
        this.id = new ResourceLocation(id);
        this.modName = modName;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.dimensions = dimensions;
        this.prediction = prediction;
        this.simulation = simulation;
        this.limitation = limitation;
        this.enabled = enabled;
    }

    public boolean supports(WorldServer world) {
        return unavailableReason(world).isEmpty();
    }

    public String unavailableReason(WorldServer world) {
        if (!hasLocator()) {
            return limitation.isEmpty() ? "structurecompasslegacy.method.unsupported" : limitation;
        }
        if (!dimensions.test(world)) {
            return "structurecompasslegacy.reason.wrong_dimension";
        }
        if (!enabled.getAsBoolean()) {
            return "structurecompasslegacy.reason.generation_disabled";
        }
        return selectRoute(world).locator.unavailableReason(world);
    }

    public boolean hasLocator() {
        return prediction != null || simulation != null;
    }

    public SearchRoute selectRoute(WorldServer world) {
        if (prediction != null && prediction.locator.unavailableReason(world).isEmpty()) {
            return prediction;
        }
        if (simulation != null && simulation.locator.unavailableReason(world).isEmpty()) {
            return simulation;
        }
        // 均不可用时保留优先路线，以便显示其具体失败原因。
        return prediction != null ? prediction : simulation;
    }

    public String translationKey() {
        return "structurecompasslegacy.structure." + id.getNamespace() + "." + id.getPath();
    }
}

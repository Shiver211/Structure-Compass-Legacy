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
    public final SearchMethod method;
    public final boolean experimental;
    public final String limitation;
    public final Predicate<WorldServer> dimensions;
    public final StructureLocator locator;
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
        this.id = new ResourceLocation(id);
        this.modName = modName;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.method = method;
        this.experimental = method == SearchMethod.SIMULATION;
        this.dimensions = dimensions;
        this.locator = locator;
        this.limitation = limitation;
        this.enabled = enabled;
    }

    public boolean supports(WorldServer world) {
        return unavailableReason(world).isEmpty();
    }

    public String unavailableReason(WorldServer world) {
        if (locator == null) {
            return limitation.isEmpty() ? "structurecompasslegacy.method.unsupported" : limitation;
        }
        if (!dimensions.test(world)) {
            return "structurecompasslegacy.reason.wrong_dimension";
        }
        if (!enabled.getAsBoolean()) {
            return "structurecompasslegacy.reason.generation_disabled";
        }
        return locator.unavailableReason(world);
    }

    public String translationKey() {
        return "structurecompasslegacy.structure." + id.getNamespace() + "." + id.getPath();
    }
}

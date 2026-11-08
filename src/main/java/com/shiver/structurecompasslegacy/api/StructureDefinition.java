package com.shiver.structurecompasslegacy.api;

import java.util.function.BooleanSupplier;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;

public final class StructureDefinition {

    public final ResourceLocation id;
    public final String modName;
    public final String englishName;
    public final String chineseName;
    public final String limitation;
    public final DimensionRule dimensions;
    public final StructureLocator locator;
    private final BooleanSupplier enabled;

    public StructureDefinition(
            String id,
            String modName,
            String englishName,
            String chineseName,
            DimensionRule dimensions,
            StructureLocator locator,
            String limitation,
            BooleanSupplier enabled) {
        this.id = new ResourceLocation(id);
        this.modName = modName;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.dimensions = dimensions;
        this.locator = locator;
        this.limitation = limitation;
        this.enabled = enabled;
    }

    public String unavailableReason(WorldServer world) {
        if (!hasLocator()) {
            return "structurecompasslegacy.limitation.binding_failed";
        }
        if (!dimensions.test(world)) {
            return "structurecompasslegacy.reason.wrong_dimension";
        }
        if (!enabled.getAsBoolean()) {
            return "structurecompasslegacy.reason.generation_disabled";
        }
        return locator.unavailableReason(world);
    }

    public boolean hasLocator() {
        return locator != null;
    }
}

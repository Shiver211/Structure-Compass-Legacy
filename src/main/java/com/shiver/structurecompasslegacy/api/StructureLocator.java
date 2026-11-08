package com.shiver.structurecompasslegacy.api;

import net.minecraft.world.WorldServer;

@FunctionalInterface
public interface StructureLocator {

    SearchTask createTask(SearchContext context) throws Exception;

    default String unavailableReason(WorldServer world) {
        return "";
    }
}

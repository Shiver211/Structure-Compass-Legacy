package com.shiver.structurecompasslegacy.mixin;

import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MapGenStructure.class)
public interface MapGenStructureInvoker {

    @Invoker("canSpawnStructureAtCoords")
    boolean compassCanSpawn(int chunkX, int chunkZ);

    @Invoker("getStructureStart")
    StructureStart compassCreateStart(int chunkX, int chunkZ);
}

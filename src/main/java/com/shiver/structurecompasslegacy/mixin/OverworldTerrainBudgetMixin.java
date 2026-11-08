package com.shiver.structurecompasslegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;
import com.shiver.structurecompasslegacy.simulation.VanillaBiomeSafety;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGeneratorOverworld.class)
public abstract class OverworldTerrainBudgetMixin {

    @Shadow
    @Final
    private World world;

    @Inject(method = "replaceBiomeBlocks", at = @At("HEAD"))
    private void checkBiomes(int x, int z, ChunkPrimer primer, Biome[] biomes, CallbackInfo callback) {
        if (world instanceof SimulationWorld) {
            VanillaBiomeSafety.requireSafe(biomes);
        }
    }

    @WrapMethod(method = "setBlocksInChunk")
    private void compassCountTerrain(int x, int z, ChunkPrimer primer, Operation<Void> original) {
        if (world instanceof SimulationWorld) {
            ((SimulationWorld) world).simulationChunks().prepareTerrain(x, z, () -> original.call(x, z, primer));
        } else {
            original.call(x, z, primer);
        }
    }
}

package com.shiver.structurecompasslegacy.compat.biomesoplenty.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;
import com.shiver.structurecompasslegacy.simulation.VanillaBiomeSafety;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "biomesoplenty.common.world.ChunkGeneratorOverworldBOP", remap = false)
public abstract class BopTerrainMixin {

    @Shadow
    private World world;

    @WrapMethod(method = "setChunkAirStoneWater")
    private void countTerrain(int x, int z, ChunkPrimer primer, Operation<Void> original) {
        if (world instanceof SimulationWorld) {
            ((SimulationWorld) world).simulationChunks().prepareTerrain(x, z, () -> original.call(x, z, primer));
        } else {
            original.call(x, z, primer);
        }
    }

    @Inject(method = "replaceBlocksForBiome", at = @At("HEAD"))
    private void guardSurface(int x, int z, ChunkPrimer primer, Biome[] biomes, CallbackInfo callback) {
        if (world instanceof SimulationWorld) {
            // 府邸只需要基础地形；完整地表中的共享群系写入尚未适配。
            VanillaBiomeSafety.requireSafe(biomes);
        }
    }
}

package com.shiver.structurecompasslegacy.compat.biomesoplenty.mixin;

import com.shiver.structurecompasslegacy.compat.biomesoplenty.BiomesOPlentyModule;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "biomesoplenty.common.world.WorldTypeBOP", remap = false)
public abstract class WorldTypeBopMixin {

    @Inject(method = "getBiomeProvider", at = @At("HEAD"), cancellable = true)
    private void privateBiomeProvider(World world, CallbackInfoReturnable<BiomeProvider> callback) {
        if (world instanceof SimulationWorld) {
            callback.setReturnValue(BiomesOPlentyModule.createBiomes(world));
        }
    }
}

package com.shiver.structurecompasslegacy.mixin;

import com.shiver.structurecompasslegacy.simulation.SimulationMarker;
import com.shiver.structurecompasslegacy.simulation.SimulationAccess;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DimensionManager.class, remap = false)
public abstract class DimensionManagerMixin {

    @Inject(method = "setWorld", at = @At("HEAD"), cancellable = true)
    private static void preventSimulationRegistration(
            int dimension,
            WorldServer world,
            MinecraftServer server,
            CallbackInfo callback) {
        if (world instanceof SimulationMarker) {
            callback.cancel();
        } else {
            SimulationAccess.requireWorld(world);
        }
    }
}

package com.shiver.structurecompasslegacy.mixin;

import com.shiver.structurecompasslegacy.simulation.SimulationAccess;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldServer.class)
public abstract class WorldServerMutationMixin {

    @Inject(method = {"spawnEntity", "addScheduledTask(Ljava/lang/Runnable;)Lcom/google/common/util/concurrent/ListenableFuture;"},
            at = @At("HEAD"))
    private void guardResult(CallbackInfoReturnable<?> callback) {
        SimulationAccess.requireWorld((WorldServer) (Object) this);
    }

    @Inject(method = {"updateBlockTick", "scheduleUpdate", "addBlockEvent", "saveAllChunks"}, at = @At("HEAD"))
    private void guardChanges(CallbackInfo callback) {
        SimulationAccess.requireWorld((WorldServer) (Object) this);
    }
}

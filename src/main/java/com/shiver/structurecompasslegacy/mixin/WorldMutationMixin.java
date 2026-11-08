package com.shiver.structurecompasslegacy.mixin;

import com.shiver.structurecompasslegacy.simulation.SimulationAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMutationMixin {

    @Inject(method = {"setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z",
            "spawnEntity"}, at = @At("HEAD"))
    private void guardBlockAndEntity(CallbackInfoReturnable<Boolean> callback) {
        SimulationAccess.requireWorld((World) (Object) this);
    }

    @Inject(method = {"setTileEntity", "removeTileEntity", "removeEntity", "setWorldTime", "setTotalWorldTime",
            "setSpawnPoint", "setSeaLevel", "notifyNeighborsOfStateChange"}, at = @At("HEAD"))
    private void guardWorldState(CallbackInfo callback) {
        SimulationAccess.requireWorld((World) (Object) this);
    }
}

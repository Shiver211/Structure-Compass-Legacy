package com.shiver.structurecompasslegacy.mixin;

import com.shiver.structurecompasslegacy.simulation.SimulationAccess;
import net.minecraft.world.storage.SaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SaveHandler.class)
public abstract class SaveHandlerAccessMixin {

    @Inject(method = {"getWorldDirectory", "getMapFileFromName", "loadWorldInfo", "getPlayerNBTManager",
            "getStructureTemplateManager", "readPlayerData"}, at = @At("HEAD"))
    private void guardFiles(CallbackInfoReturnable<?> callback) {
        SimulationAccess.requireMemoryStorage(false);
    }

    @Inject(method = {"saveWorldInfoWithPlayer", "saveWorldInfo", "writePlayerData"}, at = @At("HEAD"))
    private void guardWrites(CallbackInfo callback) {
        SimulationAccess.requireMemoryStorage(false);
    }
}

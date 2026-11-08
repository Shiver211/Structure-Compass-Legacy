package com.shiver.structurecompasslegacy.mixin;

import com.shiver.structurecompasslegacy.simulation.SimulationAccess;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapStorage.class)
public abstract class MapStorageAccessMixin {

    @Shadow
    @Final
    private ISaveHandler saveHandler;

    @Inject(method = {"getOrLoadData", "getUniqueDataId"}, at = @At("HEAD"))
    private void guardRead(CallbackInfoReturnable<?> callback) {
        SimulationAccess.requireMemoryStorage(saveHandler == null);
    }

    @Inject(method = {"setData", "saveAllData"}, at = @At("HEAD"))
    private void guardWrite(CallbackInfo callback) {
        SimulationAccess.requireMemoryStorage(saveHandler == null);
    }
}

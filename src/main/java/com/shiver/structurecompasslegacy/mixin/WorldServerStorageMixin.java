package com.shiver.structurecompasslegacy.mixin;

import com.shiver.structurecompasslegacy.simulation.SimulationMarker;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WorldServer.class)
public abstract class WorldServerStorageMixin {

    @ModifyArg(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/storage/MapStorage;<init>(Lnet/minecraft/world/storage/ISaveHandler;)V"),
            index = 0)
    private ISaveHandler compassUseMemoryMapStorage(ISaveHandler saveHandler) {
        // 父类构造期间就会读取 idcounts，等 SimulationWorld 构造后再替换已经太晚。
        return (Object) this instanceof SimulationMarker ? null : saveHandler;
    }
}

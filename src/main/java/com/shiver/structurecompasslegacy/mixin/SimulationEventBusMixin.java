package com.shiver.structurecompasslegacy.mixin;

import com.shiver.structurecompasslegacy.simulation.SimulationScope;

import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EventBus.class, remap = false)
public abstract class SimulationEventBusMixin {

    @Inject(method = "post", at = @At("HEAD"), cancellable = true)
    private void isolateSimulationEvents(Event event, CallbackInfoReturnable<Boolean> callback) {
        // 适配器显式调用经过检查的生成入口，不能广播给全部模组的监听器。
        if (SimulationScope.isActive()) {
            callback.setReturnValue(false);
        }
    }
}

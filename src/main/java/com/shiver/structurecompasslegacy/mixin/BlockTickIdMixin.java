package com.shiver.structurecompasslegacy.mixin;

import com.shiver.structurecompasslegacy.simulation.SimulationIds;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import net.minecraft.world.NextTickListEntry;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NextTickListEntry.class)
public abstract class BlockTickIdMixin {

    @Shadow
    private static long nextTickEntryID;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/NextTickListEntry;nextTickEntryID:J",
            opcode = Opcodes.GETSTATIC))
    private static long allocateId() {
        return SimulationScope.isActive() ? SimulationIds.blockTick() : nextTickEntryID;
    }

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/NextTickListEntry;nextTickEntryID:J",
            opcode = Opcodes.PUTSTATIC))
    private static void keepRealCounter(long next) {
        if (!SimulationScope.isActive()) {
            nextTickEntryID = next;
        }
    }
}

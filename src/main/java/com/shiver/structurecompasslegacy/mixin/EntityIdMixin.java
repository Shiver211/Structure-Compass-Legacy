package com.shiver.structurecompasslegacy.mixin;

import com.shiver.structurecompasslegacy.simulation.SimulationAccess;
import com.shiver.structurecompasslegacy.simulation.SimulationIds;
import com.shiver.structurecompasslegacy.simulation.SimulationMarker;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityIdMixin {

    @Shadow
    private static int nextEntityID;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;nextEntityID:I",
            opcode = Opcodes.GETSTATIC))
    private static int allocateId(World world) {
        SimulationAccess.requireWorld(world);
        return world instanceof SimulationMarker ? SimulationIds.entity() : nextEntityID;
    }

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;nextEntityID:I",
            opcode = Opcodes.PUTSTATIC))
    private static void keepRealCounter(int next, World world) {
        if (!(world instanceof SimulationMarker)) {
            nextEntityID = next;
        }
    }
}

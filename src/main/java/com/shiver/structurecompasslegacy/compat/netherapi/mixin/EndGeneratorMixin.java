package com.shiver.structurecompasslegacy.compat.netherapi.mixin;

import java.util.List;
import java.util.stream.Stream;

import com.shiver.structurecompasslegacy.simulation.SimulationScope;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "git.jbredwards.nether_api.mod.common.world.gen.ChunkGeneratorTheEnd", remap = false)
public abstract class EndGeneratorMixin {

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;"),
            require = 1,
            allow = 1)
    private Stream<?> compassSkipUnrelatedStructures(List<?> entries) {
        if (SimulationScope.isActive()) {
            return Stream.empty();
        }
        return entries.stream();
    }
}

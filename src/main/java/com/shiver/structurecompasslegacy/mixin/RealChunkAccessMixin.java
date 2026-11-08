package com.shiver.structurecompasslegacy.mixin;

import com.shiver.structurecompasslegacy.simulation.SimulationAccess;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkProviderServer.class)
public abstract class RealChunkAccessMixin {

    @Shadow
    @Final
    private WorldServer world;

    @Inject(method = {"provideChunk", "loadChunk(II)Lnet/minecraft/world/chunk/Chunk;"}, at = @At("HEAD"))
    private void guardLoading(CallbackInfoReturnable<Chunk> callback) {
        SimulationAccess.requireWorld(world);
    }

    @Inject(method = "loadChunk(IILjava/lang/Runnable;)Lnet/minecraft/world/chunk/Chunk;", at = @At("HEAD"), remap = false)
    private void guardForgeLoading(CallbackInfoReturnable<Chunk> callback) {
        SimulationAccess.requireWorld(world);
    }
}

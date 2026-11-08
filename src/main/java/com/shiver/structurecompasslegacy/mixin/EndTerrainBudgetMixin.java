package com.shiver.structurecompasslegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorEnd;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkGeneratorEnd.class)
public abstract class EndTerrainBudgetMixin {

    @Shadow
    @Final
    private World world;

    @WrapMethod(method = "setBlocksInChunk")
    private void compassCountTerrain(int x, int z, ChunkPrimer primer, Operation<Void> original) {
        if (world instanceof SimulationWorld) {
            ((SimulationWorld) world).simulationChunks().prepareTerrain(x, z, () -> original.call(x, z, primer));
        } else {
            original.call(x, z, primer);
        }
    }
}

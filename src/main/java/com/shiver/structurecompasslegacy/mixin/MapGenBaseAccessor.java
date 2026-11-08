package com.shiver.structurecompasslegacy.mixin;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.MapGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapGenBase.class)
public interface MapGenBaseAccessor {

    @Accessor("world")
    void compassSetWorld(World world);

    @Accessor("rand")
    void compassSetRandom(Random random);
}

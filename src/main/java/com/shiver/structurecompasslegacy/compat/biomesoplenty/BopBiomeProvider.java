package com.shiver.structurecompasslegacy.compat.biomesoplenty;

import java.lang.reflect.Field;

import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/** 直接安装私人图层，避开原生构造器的 applySettings 和全局 Generators 写入。 */
final class BopBiomeProvider extends BiomeProvider {

    private static final Field GENERATION = ReflectionHelper.findField(BiomeProvider.class,
            new String[] {"genBiomes", "field_76944_d"});
    private static final Field INDEX = ReflectionHelper.findField(BiomeProvider.class,
            new String[] {"biomeIndexLayer", "field_76945_e"});

    BopBiomeProvider(GenLayer[] layers) {
        try {
            GENERATION.set(this, layers[0]);
            INDEX.set(this, layers[1]);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot install private BOP biome layers", exception);
        }
    }
}

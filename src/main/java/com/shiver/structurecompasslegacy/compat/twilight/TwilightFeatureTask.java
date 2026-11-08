package com.shiver.structurecompasslegacy.compat.twilight;

import java.lang.reflect.Field;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.search.ChunkSearchTask;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

final class TwilightFeatureTask extends ChunkSearchTask {

    private final String feature;
    private final ModConstructor<BiomeProvider> biomeFactory;
    private final Field containedFeature;

    private BiomeProvider biomes;

    TwilightFeatureTask(
            SearchContext context,
            String feature,
            ModConstructor<BiomeProvider> biomeFactory,
            Field containedFeature) {
        super(context);
        this.feature = feature;
        this.biomeFactory = biomeFactory;
        this.containedFeature = containedFeature;
    }

    @Override
    protected SearchTarget inspect(int chunkX, int chunkZ) {
        BlockPos center = TwilightPlacement.center(chunkX, chunkZ);
        if ((center.getX() >> 4) != chunkX || (center.getZ() >> 4) != chunkZ) {
            return null;
        }

        if (biomes == null) {
            // 此构造器仅读取 world.getSeed()，内部图层与缓存均为本次搜索新建。
            biomes = biomeFactory.create(context.source);
        }

        int sampleX = TwilightPlacement.biomeSample(chunkX);
        int sampleZ = TwilightPlacement.biomeSample(chunkZ);
        Biome biome = biomes.getBiome(new BlockPos(sampleX * 16 + 8, 0, sampleZ * 16 + 8));
        String selected = "NOTHING";
        if (containedFeature.getDeclaringClass().isInstance(biome)) {
            selected = ((Enum<?>) ModFields.read(containedFeature, biome)).name();
        }
        if (selected.equals("NOTHING")) {
            selected = TwilightPlacement.defaultFeature(context.seed, sampleX, sampleZ);
        }

        return feature.equals(selected) ? new SearchTarget(center) : null;
    }

    @Override
    public void close() {
        biomes = null;
    }
}

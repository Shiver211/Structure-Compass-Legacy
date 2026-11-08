package com.shiver.structurecompasslegacy.compat.twilight;

import java.lang.reflect.Field;
import java.util.Locale;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;

public final class TwilightModule implements CompatModule {

    private static final String[] FEATURES = {
            "SMALL_HILL",
            "MEDIUM_HILL",
            "LARGE_HILL",
            "HEDGE_MAZE",
            "NAGA_COURTYARD",
            "LICH_TOWER",
            "ICE_TOWER",
            "QUEST_GROVE",
            "HYDRA_LAIR",
            "LABYRINTH",
            "DARK_TOWER",
            "KNIGHT_STRONGHOLD",
            "YETI_CAVE",
            "TROLL_CAVE",
            "FINAL_CASTLE",
            "MUSHROOM_TOWER"
    };

    @Override
    public String modId() {
        return "twilightforest";
    }

    @Override
    public void register(StructureRegistry registry) {
        ModConstructor<BiomeProvider> biomes = ModConstructor.bind(
                "twilightforest.world.TFBiomeProvider", BiomeProvider.class, World.class);
        Field feature = ModFields.bind("twilightforest.biomes.TFBiomeBase", "containedFeature");
        ModuleRegistration entries = new ModuleRegistration(
                registry, modId(), "twilightforest.world.WorldProviderTwilightForest");

        for (String featureName : FEATURES) {
            entries.prediction(featureName.toLowerCase(Locale.ROOT),
                    context -> new TwilightFeatureTask(context, featureName, biomes, feature));
        }
    }
}

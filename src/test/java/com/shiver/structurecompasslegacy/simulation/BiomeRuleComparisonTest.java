package com.shiver.structurecompasslegacy.simulation;

import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.storage.WorldInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BiomeRuleComparisonTest {

    @BeforeAll
    static void initialize() {
        Bootstrap.register();
    }

    @Test
    void acceptsIndependentRulesAndRejectsChangedSeedsOrSharedProviders() {
        BiomeProvider source = provider(42L);
        assertTrue(BiomeRuleComparison.matches(source, provider(42L)));
        assertFalse(BiomeRuleComparison.matches(source, provider(43L)));
        assertFalse(BiomeRuleComparison.matches(source, source));
    }

    private static BiomeProvider provider(long seed) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("RandomSeed", seed);
        tag.setString("generatorName", "default");
        return new BiomeProvider(new WorldInfo(tag));
    }
}

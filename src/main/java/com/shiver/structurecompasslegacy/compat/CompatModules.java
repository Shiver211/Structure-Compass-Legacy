package com.shiver.structurecompasslegacy.compat;

import java.util.Arrays;
import java.util.List;

import com.shiver.structurecompasslegacy.StructureCompassLegacy;
import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.abyssalcraft.AbyssalCraftModule;
import com.shiver.structurecompasslegacy.compat.aether.AetherModule;
import com.shiver.structurecompasslegacy.compat.atum.AtumModule;
import com.shiver.structurecompasslegacy.compat.aurorian.AurorianModule;
import com.shiver.structurecompasslegacy.compat.biomesoplenty.BiomesOPlentyModule;
import com.shiver.structurecompasslegacy.compat.betweenlands.BetweenlandsModule;
import com.shiver.structurecompasslegacy.compat.draconic.DraconicModule;
import com.shiver.structurecompasslegacy.compat.divinerpg.DivineRpgModule;
import com.shiver.structurecompasslegacy.compat.endexpansion.EndExpansionModule;
import com.shiver.structurecompasslegacy.compat.erebus.ErebusModule;
import com.shiver.structurecompasslegacy.compat.dungeonadditions.DungeonAdditionsModule;
import com.shiver.structurecompasslegacy.compat.gctadditions.GctAdditionsModule;
import com.shiver.structurecompasslegacy.compat.lostaether.LostAetherModule;
import com.shiver.structurecompasslegacy.compat.netherapi.NetherApiModule;
import com.shiver.structurecompasslegacy.compat.thaumicaugmentation.ThaumicAugmentationModule;
import com.shiver.structurecompasslegacy.compat.tofu.TofuModule;
import com.shiver.structurecompasslegacy.compat.twilight.TwilightModule;

import net.minecraftforge.fml.common.Loader;

public final class CompatModules {

    private CompatModules() {
    }

    public static void register(StructureRegistry registry) {
        List<CompatModule> modules = Arrays.asList(
                new BiomesOPlentyModule(),
                new NetherApiModule(),
                new BetweenlandsModule(),
                new ErebusModule(),
                new EndExpansionModule(),
                new DivineRpgModule(),
                new DungeonAdditionsModule(),
                new AbyssalCraftModule(),
                new AetherModule(),
                new LostAetherModule(),
                new AtumModule(),
                new TofuModule(),
                new TwilightModule(),
                new DraconicModule(),
                new ThaumicAugmentationModule(),
                new AurorianModule(),
                new GctAdditionsModule());

        for (CompatModule module : modules) {
            if (!Loader.isModLoaded(module.modId())) {
                continue;
            }

            try {
                module.register(registry);
                StructureCompassLegacy.logger.info("Registered structure compatibility: {}", module.modId());
            } catch (RuntimeException | LinkageError exception) {
                StructureCompassLegacy.logger.error("Cannot register structure compatibility: {}", module.modId(), exception);
            }
        }
    }
}

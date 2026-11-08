package com.shiver.structurecompasslegacy.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

public final class CompassLateMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        List<String> configs = new ArrayList<>();
        if (Loader.isModLoaded("biomesoplenty")) {
            configs.add("mixins.structurecompasslegacy.biomesoplenty.json");
        }
        if (Loader.isModLoaded("nether_api")) {
            configs.add("mixins.structurecompasslegacy.netherapi.json");
        }
        return configs;
    }
}

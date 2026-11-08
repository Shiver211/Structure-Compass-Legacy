package com.shiver.structurecompasslegacy.core;

import java.util.Collections;
import java.util.List;

import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

public final class CompassLateMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        if (Loader.isModLoaded("nether_api")) {
            return Collections.singletonList("mixins.structurecompasslegacy.netherapi.json");
        }
        return Collections.emptyList();
    }
}

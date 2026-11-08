package com.shiver.structurecompasslegacy.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

@IFMLLoadingPlugin.Name("StructureCompassLegacy")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public final class CompassLoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.structurecompasslegacy.json");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}

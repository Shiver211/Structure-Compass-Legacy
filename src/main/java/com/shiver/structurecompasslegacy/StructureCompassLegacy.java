package com.shiver.structurecompasslegacy;

import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.CompatModules;
import com.shiver.structurecompasslegacy.compat.StructureCatalog;
import com.shiver.structurecompasslegacy.compat.vanilla.VanillaModule;
import com.shiver.structurecompasslegacy.item.ItemStructureCompass;
import com.shiver.structurecompasslegacy.network.CompassNetwork;
import com.shiver.structurecompasslegacy.search.SearchScheduler;

import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = StructureCompassLegacy.MOD_ID, name = "Structure Compass Legacy", version = "0.1.0")
public final class StructureCompassLegacy {

    public static final String MOD_ID = "structurecompasslegacy";
    public static final StructureRegistry STRUCTURES = new StructureRegistry();
    public static final SearchScheduler SEARCHES = new SearchScheduler();
    public static final ItemStructureCompass COMPASS = new ItemStructureCompass();

    public static Logger logger;

    @SidedProxy(
            clientSide = "com.shiver.structurecompasslegacy.client.ClientProxy",
            serverSide = "com.shiver.structurecompasslegacy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        CompassConfig.load(event.getSuggestedConfigurationFile());
        CompassNetwork.initialize();
        MinecraftForge.EVENT_BUS.register(SEARCHES);
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        StructureCatalog.register(STRUCTURES, Loader::isModLoaded);
        new VanillaModule().register(STRUCTURES);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        CompatModules.register(STRUCTURES);
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        SEARCHES.clear();
    }

    @Mod.EventBusSubscriber(modid = MOD_ID)
    public static final class Registration {

        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            event.getRegistry().register(COMPASS);
        }
    }
}

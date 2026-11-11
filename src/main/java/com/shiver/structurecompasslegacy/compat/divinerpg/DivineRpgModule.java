package com.shiver.structurecompasslegacy.compat.divinerpg;

import com.shiver.structurecompasslegacy.api.CompatModule;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModuleRegistration;
import com.shiver.structurecompasslegacy.simulation.GeneratorFactories;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;

public final class DivineRpgModule implements CompatModule {

    @Override
    public String modId() {
        return "divinerpg";
    }

    @Override
    public void register(StructureRegistry registry) {
        ModuleRegistration iceika = new ModuleRegistration(registry, modId(),
                "divinerpg.dimensions.iceika.WorldProviderIceika");
        ModuleRegistration vethea = new ModuleRegistration(registry, modId(),
                "divinerpg.dimensions.vethea.VetheaWorldProvider");

        for (DivineStructure structure : DivineStructure.values()) {
            ModuleRegistration dimension = structure.layer == 0 ? iceika : vethea;
            dimension.prediction(structure.id, context -> new DivineStructureTask(context, structure));
        }

        for (DivineAltar altar : DivineAltar.values()) {
            ModConstructor<IChunkGenerator> terrain = ModConstructor.bind(
                    altar.generatorClass, IChunkGenerator.class, World.class);
            Block grass = (Block) ModFields.read(
                    ModFields.bind("divinerpg.registry.BlockRegistry", altar.grassField), null);
            Block spawner = (Block) ModFields.read(
                    ModFields.bind("divinerpg.registry.BlockRegistry", altar.spawnerField), null);
            GeneratorFactories.register(altar.providerClass, world -> terrain.create(world));
            ModuleRegistration dimension = new ModuleRegistration(registry, modId(), altar.providerClass);
            dimension.prediction(altar.id, context -> new DivineAltarTask(context, altar, grass, spawner),
                    () -> true, "structurecompasslegacy.limitation.divinerpg_altar");
        }
    }
}

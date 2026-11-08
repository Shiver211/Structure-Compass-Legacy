package com.shiver.structurecompasslegacy.client;

import com.shiver.structurecompasslegacy.CommonProxy;
import com.shiver.structurecompasslegacy.StructureCompassLegacy;
import com.shiver.structurecompasslegacy.item.CompassTarget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new CompassHud());
        StructureCompassLegacy.COMPASS.addPropertyOverride(new ResourceLocation("angle"), (stack, world, entity) -> {
            CompassTarget target = CompassTarget.read(stack);
            if (target == null || entity == null || entity.dimension != target.dimension) {
                return 0.0F;
            }

            double angle = Math.atan2(target.position.getZ() + 0.5D - entity.posZ,
                    target.position.getX() + 0.5D - entity.posX);
            double turns = 0.5D - (entity.rotationYaw / 360.0D - 0.25D - angle / (Math.PI * 2.0D));
            return (float) MathHelper.positiveModulo(turns, 1.0D);
        });
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(StructureCompassLegacy.COMPASS, 0,
                new ModelResourceLocation(StructureCompassLegacy.COMPASS.getRegistryName(), "inventory"));
    }

    @Override
    public void receiveClientMessage(NBTTagCompound message) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> {
            if (message.getString("type").equals("catalog")) {
                minecraft.displayGuiScreen(new CompassScreen(message));
            } else if (minecraft.currentScreen instanceof CompassScreen) {
                ((CompassScreen) minecraft.currentScreen).updateProgress(message);
            }
        });
    }
}

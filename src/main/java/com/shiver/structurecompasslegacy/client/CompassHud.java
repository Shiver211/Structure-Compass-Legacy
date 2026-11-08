package com.shiver.structurecompasslegacy.client;

import com.shiver.structurecompasslegacy.StructureCompassLegacy;
import com.shiver.structurecompasslegacy.item.CompassTarget;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class CompassHud {

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || minecraft.player == null
                || minecraft.gameSettings.showDebugInfo) {
            return;
        }

        ItemStack stack = minecraft.player.getHeldItemMainhand();
        if (stack.getItem() != StructureCompassLegacy.COMPASS) {
            stack = minecraft.player.getHeldItemOffhand();
        }
        if (stack.getItem() != StructureCompassLegacy.COMPASS) {
            return;
        }

        CompassTarget target = CompassTarget.read(stack);
        if (target == null) {
            return;
        }

        String name = ClientText.name(target.englishName, target.chineseName);
        minecraft.fontRenderer.drawStringWithShadow(name, 6, 6, 0xFFFFFF);
        if (minecraft.player.dimension != target.dimension) {
            minecraft.fontRenderer.drawStringWithShadow(ClientText.translate("other_dimension", target.dimension),
                    6, 18, 0xFFCC66);
            return;
        }

        String location = ClientText.translate("target", target.position.getX(), target.position.getY(),
                target.position.getZ(), Math.round(target.distance(minecraft.player.posX, minecraft.player.posZ)));
        minecraft.fontRenderer.drawStringWithShadow(location, 6, 18, 0xDDDDDD);
    }
}

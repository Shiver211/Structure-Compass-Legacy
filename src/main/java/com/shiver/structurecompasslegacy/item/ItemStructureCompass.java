package com.shiver.structurecompasslegacy.item;

import com.shiver.structurecompasslegacy.StructureCompassLegacy;
import com.shiver.structurecompasslegacy.network.CompassNetwork;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public final class ItemStructureCompass extends Item {

    public ItemStructureCompass() {
        setRegistryName(StructureCompassLegacy.MOD_ID, "structure_compass");
        setTranslationKey(StructureCompassLegacy.MOD_ID + ".structure_compass");
        setCreativeTab(CreativeTabs.TOOLS);
        setMaxStackSize(1);
    }

    @Override
    @MethodsReturnNonnullByDefault
    public ActionResult<ItemStack> onItemRightClick(World world, @Nonnull EntityPlayer player, @Nonnull EnumHand hand) {
        if (!world.isRemote) {
            CompassNetwork.openScreen((EntityPlayerMP) player, hand);
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }
}

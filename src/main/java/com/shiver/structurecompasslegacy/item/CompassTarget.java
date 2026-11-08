package com.shiver.structurecompasslegacy.item;

import com.shiver.structurecompasslegacy.api.SearchTarget;
import com.shiver.structurecompasslegacy.api.StructureDefinition;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/** 物品只保留当前导航目标，清除或覆盖后不留下搜索历史。 */
public final class CompassTarget {

    private static final String TARGET_KEY = "CompassTarget";

    public final String englishName;
    public final String chineseName;
    public final int dimension;
    public final BlockPos position;

    private CompassTarget(NBTTagCompound tag) {
        englishName = tag.getString("english");
        chineseName = tag.getString("chinese");
        dimension = tag.getInteger("dimension");
        position = BlockPos.fromLong(tag.getLong("position"));
    }

    public static CompassTarget read(ItemStack stack) {
        NBTTagCompound root = stack.getTagCompound();
        if (root == null || !root.hasKey(TARGET_KEY, 10)) {
            return null;
        }

        return new CompassTarget(root.getCompoundTag(TARGET_KEY));
    }

    public static void write(
            ItemStack stack, StructureDefinition structure, SearchTarget target, int dimension) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("structure", structure.id.toString());
        tag.setString("english", structure.englishName);
        tag.setString("chinese", structure.chineseName);
        tag.setInteger("dimension", dimension);
        tag.setLong("position", target.position.toLong());
        stack.setTagInfo(TARGET_KEY, tag);
    }

    public static void clear(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag(TARGET_KEY);
        }
    }

    public double distance(double x, double z) {
        double deltaX = position.getX() + 0.5D - x;
        double deltaZ = position.getZ() + 0.5D - z;
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }
}

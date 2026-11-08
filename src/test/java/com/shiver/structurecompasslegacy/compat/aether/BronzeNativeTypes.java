package com.shiver.structurecompasslegacy.compat.aether;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

/** 仅替换地形、材质及实体资源；原生概率、空间检查和房间几何由安装 JAR 执行。 */
public final class BronzeNativeTypes {

    private BronzeNativeTypes() {
    }

    public static Block block(String name) {
        switch (name) {
            case "locked_dungeon_block": return Blocks.BEDROCK;
            case "dungeon_block": return Blocks.STONEBRICK;
            case "holystone": return Blocks.COBBLESTONE;
            case "mossy_holystone": return Blocks.MOSSY_COBBLESTONE;
            case "treasure_chest": return Chest.INSTANCE;
            case "dungeon_trap": return Blocks.DISPENSER;
            case "dungeon_chest": return Blocks.CHEST;
            case "chest_mimic": return Blocks.TRAPPED_CHEST;
            case "aether_leaves": return Blocks.LEAVES;
            case "crystal_leaves": return Blocks.LEAVES2;
            case "holiday_leaves": return Blocks.SPONGE;
            case "aether_log": return Blocks.LOG;
            case "aercloud": return Blocks.WOOL;
            default: throw new AssertionError("Unexpected native block: " + name);
        }
    }

    public static IBlockState property(IBlockState state,
            IProperty<?> property, Comparable<?> value) {
        return state;
    }

    public static final class Terrain {

        public boolean isRemote;
        Predicate<BlockPos> loaded = position -> true;
        Predicate<BlockPos> solid = position -> position.getY() >= 16 && position.getY() <= 96;
        final Map<BlockPos, IBlockState> blocks = new HashMap<>();
        final List<Slider> sliders = new ArrayList<>();

        public boolean isBlockLoaded(BlockPos position) {
            return loaded.test(position);
        }

        public IBlockState getBlockState(BlockPos position) {
            return blocks.getOrDefault(position, solid.test(position)
                    ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState());
        }

        public boolean spawnEntity(Slider slider) {
            sliders.add(slider);
            return true;
        }

        public boolean setBlockState(BlockPos position, IBlockState state, int flags) {
            blocks.put(position, state);
            return true;
        }

        public ChestTile getTileEntity(BlockPos position) {
            return new ChestTile();
        }
    }

    public static final class Slider {

        public double posX;
        public double posY;
        public double posZ;
        BlockPos position;
        boolean dead;

        public Slider(Terrain terrain) {
        }

        public void setPosition(double x, double y, double z) {
            posX = x;
            posY = y;
            posZ = z;
            position = new BlockPos(x, y, z);
        }

        public void setDungeon(double x, double y, double z) {
        }

        public void setDead() {
            dead = true;
        }
    }

    public static final class Chest extends Block {

        static final Chest INSTANCE = new Chest();

        private Chest() {
            super(Material.WOOD);
        }

        public IBlockState correctFacing(Terrain world, BlockPos position, IBlockState state) {
            return state;
        }
    }

    public static final class Config {
        public static final WorldGen world_gen = new WorldGen();
    }

    public static final class Loot {
        public static final ResourceLocation bronze_dungeon_chest = new ResourceLocation("test:bronze");
    }

    public static final class ChestTile {
        public void setLootTable(ResourceLocation table, long seed) {
        }
    }

    public static final class WorldGen {
        public int bronze_dungeon_primary_chance = 15;
        public int bronze_dungeon_secondary_chance = 40;
    }
}

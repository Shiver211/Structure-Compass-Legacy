package com.shiver.structurecompasslegacy.compat.iceandfire;

import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModMethod;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.lang.reflect.Field;

/**
 * Only reads already loaded chunks. Unopened loot tables are inspected without rolling the loot.
 */
final class IceEvidence {
    private static final String ENTITY = "com.github.alexthe666.iceandfire.entity.";
    private static final Field HOME = ModFields.bind(ENTITY + "EntityDragonBase", "homePos");
    private static final ModMethod POSITION = ModMethod.bind(HOME.getType(), "getPosition");
    private static final ModMethod DIMENSION = ModMethod.bind(HOME.getType(), "getDimension");

    static BlockPos find(WorldServer world, Chunk chunk, IceLandmark kind, BlockPos last, BlockPos prediction) {
        BlockPos center = new BlockPos(chunk.x * 16 + 8, 0, chunk.z * 16 + 8);
        if (kind.material != null) {
            if (inColumn(last, center) && dragonSite(world, kind, last)) {
                return last;
            }
            String element = kind.id.substring(0, kind.id.indexOf('_'));
            String entity =
                ENTITY + "Entity" + Character.toUpperCase(element.charAt(0)) + element.substring(1) + "Dragon";
            for (Iterable<Entity> section : chunk.getEntityLists()) {
                for (Entity e : section) {
                    if (!entity.equals(e.getClass().getName()) || ((EntityTameable) e).isTamed()) {
                        continue;
                    }
                    Object home = ModFields.read(HOME, e);
                    if (home == null || !Integer.valueOf(world.provider.getDimension()).equals(DIMENSION.call(home))) {
                        continue;
                    }
                    BlockPos pos = (BlockPos) POSITION.call(home);
                    if (inColumn(pos, center) && dragonSite(world, kind, pos)) {
                        return pos;
                    }
                }
            }
            if (!kind.roost() && prediction != null && dragonSite(world, kind, prediction)) {
                return prediction;
            }
        }
        if (kind == IceLandmark.SIREN_ISLAND) {
            // Stone alone or a wandering siren is not evidence. Require a native/seeded anchor,
            // warm ocean biome and the island's mixed stone cap above water.
            int y = inColumn(last, center) ? last.getY() : world.getSeaLevel();
            if (!inColumn(last, center) && prediction == null) {
                return null;
            }
            net.minecraft.world.biome.Biome b = chunk.getBiome(center, world.getBiomeProvider());
            if (!net.minecraftforge.common.BiomeDictionary.hasType(b,
                net.minecraftforge.common.BiomeDictionary.Type.OCEAN)
                || net.minecraftforge.common.BiomeDictionary.hasType(b,
                net.minecraftforge.common.BiomeDictionary.Type.COLD)) {
                return null;
            }
            int stone = 0, moss = 0;
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    for (int h = y; h <= y + 4; h++) {
                        Block block = chunk.getBlockState(center.getX() + x, h, center.getZ() + z).getBlock();
                        if (block == Blocks.MOSSY_COBBLESTONE) {
                            moss++;
                        }
                        if (block == Blocks.COBBLESTONE) {
                            stone++;
                        }
                    }
                }
            }
            return moss > 0 && stone >= 3 ? center.up(y) : null;
        }
        for (TileEntity te : chunk.getTileEntityMap().values()) {
            BlockPos pos = te.getPos();
            if (te.isInvalid()) {
                continue;
            }
            if (kind == IceLandmark.MAUSOLEUM && named(te.getBlockType(), "dread_spawner")) {
                // Template: spawner (10,7,8) and door lock (10,2,2), through all rotations.
                for (EnumFacing face : EnumFacing.HORIZONTALS) {
                    if (named(block(world, pos.down(5).offset(face, 6)), "dreadwood_planks_lock")) {
                        return pos;
                    }
                }
            }
            if (!(te instanceof TileEntityChest)) {
                continue;
            }
            ResourceLocation loot = ((TileEntityChest) te).getLootTable();
            if (loot == null || !"iceandfire".equals(loot.getNamespace())) {
                continue;
            }
            if (kind.material != null && !kind.roost()) {
                // Roosts use the female cave table too. Only the male table is unique to a den.
                String element = kind.id.substring(0, kind.id.indexOf('_'));
                if (loot.getPath().equals(element + "_dragon_male_cave") && pos.getY() <= 40) {
                    return pos;
                }
            }
        }
        return null;
    }

    private static boolean inColumn(BlockPos pos, BlockPos center) {
        return pos != null && pos.getX() == center.getX() && pos.getZ() == center.getZ();
    }

    private static boolean dragonSite(WorldServer world, IceLandmark k, BlockPos pos) {
        if (k.roost()) {
            int ground = 0;
            for (EnumFacing dir : EnumFacing.HORIZONTALS) {
                Block block = block(world, pos.down().offset(dir, 3));
                if (named(block, k.material + "_dirt") || named(block, k.material + "_gravel")) {
                    ground++;
                }
            }
            return ground >= 3;
        }
        if (pos.getY() < 20 || pos.getY() > 39 || block(world, pos) != Blocks.AIR) {
            return false;
        }
        // The hollow chamber has a typed shell above and below its native center.
        return shell(world, k, pos, 1) && shell(world, k, pos, -1);
    }

    private static boolean shell(WorldServer world, IceLandmark k, BlockPos pos, int direction) {
        for (int d = 4; d <= 18; d++) {
            Block b = block(world, pos.up(d * direction));
            if (named(b, k.material + "_stone") || named(b, k.material + "_cobblestone")) {
                return true;
            }
        }
        return false;
    }

    private static Block block(WorldServer world, BlockPos pos) {
        Chunk c = world.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);
        return c == null ? null : c.getBlockState(pos).getBlock();
    }

    private static boolean named(Block b, String name) {
        ResourceLocation id = b == null ? null : b.getRegistryName();
        return id != null && "iceandfire".equals(id.getNamespace()) && name.equals(id.getPath());
    }
}

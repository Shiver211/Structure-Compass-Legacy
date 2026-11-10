package com.shiver.structurecompasslegacy.compat.erebus;

import java.lang.reflect.Field;
import java.util.Random;

import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModMethod;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;

import net.minecraft.world.World;

/** 每个候选使用私人装饰器，同步调用后恢复原生共享矿物工具的全部可变字段。 */
final class ErebusDecoration {

    private static final String BASE = "erebus.world.biomes.decorators.BiomeDecoratorBaseErebus";
    private static final String ORES = "erebus.world.biomes.decorators.data.OreSettings";
    private final ModConstructor<Object> decorators = ModConstructor.bind(
            "erebus.world.biomes.decorators.BiomeDecoratorUlteriorOutback", Object.class);
    private final ModMethod populate = ModMethod.bind(ModConstructor.type(BASE),
            "populate", World.class, Random.class, int.class, int.class);
    private final ModMethod decorate = ModMethod.bind(ModConstructor.type(BASE),
            "decorate", World.class, Random.class, int.class, int.class);
    private final Object settings = ModFields.read(field(BASE, "oreGen"), null);
    private final Object minable = ModFields.read(field(ORES, "genMinable"), null);
    private final Field[] fields = {
            field(ORES, "oreType"), field(ORES, "chance"), field(ORES, "minIterations"),
            field(ORES, "maxIterations"), field(ORES, "minAmount"), field(ORES, "maxAmount"),
            field(ORES, "minY"), field(ORES, "maxY"), field(ORES, "checkArea"),
            field("erebus.world.feature.decoration.WorldGenErebusMinable", "minableBlock"),
            field("erebus.world.feature.decoration.WorldGenErebusMinable", "minableBlockMeta"),
            field("erebus.world.feature.decoration.WorldGenErebusMinable", "numberOfBlocks"),
            field("erebus.world.feature.decoration.WorldGenErebusMinable", "blockToReplace")
    };

    void populate(SimulationWorld world, int chunkX, int chunkZ) {
        // 与 ChunkProviderErebus.populate 相同；不用地形生成器的 Random，避免临时邻区块生成重置它。
        Random random = new Random(world.getSeed());
        long seedX = random.nextLong() / 2L * 2L + 1L;
        long seedZ = random.nextLong() / 2L * 2L + 1L;
        random.setSeed(chunkX * seedX + chunkZ * seedZ ^ world.getSeed());
        Object decorator = decorators.create();
        Object[] previous = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            previous[i] = ModFields.read(fields[i], owner(i));
        }
        // SimulationSession 保证在服务器主线程调用；还原在同一步内完成，异常也不会残留状态。
        try {
            populate.call(decorator, world, random, chunkX * 16, chunkZ * 16);
            decorate.call(decorator, world, random, chunkX * 16, chunkZ * 16);
        } finally {
            try {
                for (int i = 0; i < fields.length; i++) {
                    fields[i].set(owner(i), previous[i]);
                }
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Cannot restore Erebus ore generation state", exception);
            }
        }
        // 原生 population 刷怪不改变地形；不执行实体或目标结构本体生成。
    }

    private Object owner(int index) {
        return index < 9 ? settings : minable;
    }

    private static Field field(String type, String name) {
        try {
            Field field = ModConstructor.type(type).getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot bind Erebus generation field: " + type + "." + name, exception);
        }
    }
}

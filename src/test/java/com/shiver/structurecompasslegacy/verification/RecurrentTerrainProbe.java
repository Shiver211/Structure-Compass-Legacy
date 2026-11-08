package com.shiver.structurecompasslegacy.verification;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import com.shiver.structurecompasslegacy.simulation.SimulationWorld;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

/** RC 1.4.8.7 的真实 FactorMatch 检查；不访问结构注册表或生成记录。 */
final class RecurrentTerrainProbe {

    private final Constructor<?> cacheConstructor;
    private final Method weight;
    private final Object factor;

    RecurrentTerrainProbe() throws ReflectiveOperationException {
        Class<?> cacheClass = Class.forName("ivorius.ivtoolkit.world.WorldCache");
        Class<?> factorClass = Class.forName(
                "ivorius.reccomplex.world.gen.feature.structure.generic.placement.FactorMatch");
        cacheConstructor = cacheClass.getConstructor(World.class, StructureBoundingBox.class);
        factor = factorClass.getConstructor(float.class, String.class, String.class, float.class)
                .newInstance(1.0F, "", "!is:air", 1.0F);
        weight = factorClass.getDeclaredMethod("weight", cacheClass, Set.class, float.class);
        weight.setAccessible(true);
    }

    void verify(SimulationWorld world, BlockPos origin) throws ReflectiveOperationException {
        Set<BlockPos> floor = new HashSet<>();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                floor.add(world.getHeight(origin.add(x, 0, z)).down());
            }
        }
        StructureBoundingBox bounds = new StructureBoundingBox(
                origin.getX(), 0, origin.getZ(), origin.getX() + 2, 255, origin.getZ() + 2);
        Object cache = cacheConstructor.newInstance(world, bounds);
        WorldStateSnapshot.require((float) weight.invoke(factor, cache, floor, 1.0F) == 1.0F,
                "RC rejected a complete terrain floor");

        // 使用新缓存，确认 RC 能看到同一模拟候选中的临时修改。
        for (BlockPos position : floor) {
            world.setBlockState(position, Blocks.AIR.getDefaultState(), 2);
        }
        Object changedCache = cacheConstructor.newInstance(world, bounds);
        WorldStateSnapshot.require((float) weight.invoke(factor, changedCache, floor, 1.0F) == 0.0F,
                "RC accepted a removed terrain floor");
    }
}

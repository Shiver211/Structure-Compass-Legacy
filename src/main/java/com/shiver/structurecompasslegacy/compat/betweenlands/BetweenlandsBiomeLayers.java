package com.shiver.structurecompasslegacy.compat.betweenlands;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.shiver.structurecompasslegacy.compat.ModConstructor;
import com.shiver.structurecompasslegacy.compat.ModFields;
import com.shiver.structurecompasslegacy.compat.ModMethod;
import com.shiver.structurecompasslegacy.simulation.SimulationScope;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

final class BetweenlandsBiomeLayers {

    static final String PROVIDER = "thebetweenlands.common.world.gen.biome.BiomeProviderBetweenlands";
    private static final String PACKAGE = "thebetweenlands.common.world.gen.layer.";

    private final Field indexLayer = ReflectionHelper.findField(BiomeProvider.class,
            new String[] {"biomeIndexLayer", "field_76945_e"});
    private final List<Field> baseFields = Arrays.asList(
            ReflectionHelper.findField(GenLayer.class, new String[] {"parent", "field_75909_a"}),
            ReflectionHelper.findField(GenLayer.class, new String[] {"baseSeed", "field_75906_d"}),
            ReflectionHelper.findField(GenLayer.class, new String[] {"worldGenSeed", "field_75907_b"}));
    private final Map<Class<?>, List<Field>> ruleFields = new HashMap<>();
    private final ModMethod factory;

    BetweenlandsBiomeLayers() {
        Class<?> baseType = ModConstructor.type(PACKAGE + "GenLayerBetweenlands");
        factory = ModMethod.bind(baseType, "initializeAllBiomeGenerators", long.class, WorldType.class);
        for (String name : new String[] {
                "GenLayerBetweenlandsBiome", "GenLayerZoomIncrement", "GenLayerSurrounded",
                "GenLayerMask", "GenLayerThinMask", "GenLayerCircleMask", "GenLayerMixMask",
                "GenLayerResetCache", "GenLayerVoronoiZoomInstanced"}) {
            Class<?> type;
            try {
                type = Class.forName(PACKAGE + name, true, baseType.getClassLoader());
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Missing Betweenlands biome layer: " + name, exception);
            }
            List<Field> fields = new ArrayList<>(baseFields);
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            ruleFields.put(type, fields);
        }
    }

    GenLayer create(long seed, WorldType worldType, BiomeProvider source) {
        GenLayer[] layers;
        try (SimulationScope ignored = SimulationScope.enter()) {
            // 不把图层创建事件广播到未审核的模组；若真实规则被事件修改，对照会拒绝搜索。
            layers = (GenLayer[]) factory.call(null, seed, worldType);
        }
        GenLayer original = (GenLayer) ModFields.read(indexLayer, source);
        return matches(original, layers[1]) ? layers[1] : null;
    }

    boolean matches(GenLayer original, GenLayer copy) {
        if (original == null || copy == null) {
            return original == copy;
        }
        if (original == copy || original.getClass() != copy.getClass()) {
            return false;
        }
        List<Field> fields = ruleFields.get(original.getClass());
        if (fields == null) {
            return false;
        }
        for (Field field : fields) {
            Object left = ModFields.read(field, original);
            Object right = ModFields.read(field, copy);
            if (GenLayer.class.isAssignableFrom(field.getType())) {
                if (!matches((GenLayer) left, (GenLayer) right)) {
                    return false;
                }
            } else if (!Objects.equals(left, right)) {
                return false;
            }
        }
        // chunkSeed 是查询游标，InstancedIntCache 是临时数组池；两者均由新图层独立持有。
        return true;
    }
}

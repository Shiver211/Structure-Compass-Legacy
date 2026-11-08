package com.shiver.structurecompasslegacy.simulation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.shiver.structurecompasslegacy.compat.ModFields;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.common.BiomeManager;

/** 创建时屏蔽事件后，核对真实图层规则；只读参数，不在真实图层上执行查询。 */
public final class BiomeRuleComparison {

    private static final String NETHER = "git.jbredwards.nether_api.mod.common.world.";
    private static final Field INDEX = ReflectionHelper.findField(BiomeProvider.class,
            new String[] {"biomeIndexLayer", "field_76945_e"});
    private static final Map<Class<?>, List<Field>> FIELDS = new HashMap<>();

    private BiomeRuleComparison() {
    }

    public static boolean matches(BiomeProvider source, BiomeProvider copy) {
        if (source == copy || source.getClass() != copy.getClass()) {
            return false;
        }
        if (source.isFixedBiome()) {
            return copy.isFixedBiome() && source.getFixedBiome() == copy.getFixedBiome();
        }
        String name = source.getClass().getName();
        if (!name.equals(BiomeProvider.class.getName())
                && !name.equals(NETHER + "biome.BiomeProviderNether")
                && !name.equals(NETHER + "biome.BiomeProviderTheEnd")) {
            return false;
        }
        return matches((GenLayer) ModFields.read(INDEX, source), (GenLayer) ModFields.read(INDEX, copy),
                new IdentityHashMap<>());
    }

    private static boolean matches(GenLayer source, GenLayer copy, Map<GenLayer, GenLayer> compared) {
        if (source == null || copy == null) {
            return source == copy;
        }
        if (source == copy || source.getClass() != copy.getClass()) {
            return false;
        }
        if (compared.containsKey(source)) {
            return compared.get(source) == copy;
        }
        String name = source.getClass().getName();
        if (!name.startsWith("net.minecraft.world.gen.layer.")
                && !name.equals(NETHER + "gen.layer.GenLayerNetherBiomes")
                && !name.equals(NETHER + "gen.layer.GenLayerNetherSubBiomes")
                && !name.equals(NETHER + "gen.layer.GenLayerNetherEdgeBiomes")
                && !name.equals(NETHER + "gen.layer.GenLayerEnd")) {
            return false;
        }
        compared.put(source, copy);
        for (Field field : FIELDS.computeIfAbsent(source.getClass(), BiomeRuleComparison::fields)) {
            Object left = ModFields.read(field, source);
            Object right = ModFields.read(field, copy);
            if (GenLayer.class.isAssignableFrom(field.getType())) {
                if (!matches((GenLayer) left, (GenLayer) right, compared)) {
                    return false;
                }
            } else if (BiomeProvider.class.isAssignableFrom(field.getType())) {
                if (!sameEndNoise(left, right)) {
                    return false;
                }
            } else if (field.getType() == net.minecraft.world.gen.ChunkGeneratorSettings.class) {
                if (!sameSettings(left, right)) {
                    return false;
                }
            } else if (field.getType() == List[].class) {
                if (!sameBiomeLists((List<?>[]) left, (List<?>[]) right)) {
                    return false;
                }
            } else if (!Objects.deepEquals(left, right)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameBiomeLists(List<?>[] source, List<?>[] copy) {
        if (source.length != copy.length) {
            return false;
        }
        for (int i = 0; i < source.length; i++) {
            if (source[i].size() != copy[i].size()) {
                return false;
            }
            for (int j = 0; j < source[i].size(); j++) {
                Object left = source[i].get(j);
                Object right = copy[i].get(j);
                if (left.getClass() != BiomeManager.BiomeEntry.class || right.getClass() != left.getClass()) {
                    return false;
                }
                BiomeManager.BiomeEntry a = (BiomeManager.BiomeEntry) left;
                BiomeManager.BiomeEntry b = (BiomeManager.BiomeEntry) right;
                if (a.biome != b.biome || a.itemWeight != b.itemWeight) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean sameSettings(Object source, Object copy) {
        if (source == null || copy == null) {
            return source == copy;
        }
        for (Field field : FIELDS.computeIfAbsent(source.getClass(), BiomeRuleComparison::fields)) {
            if (!Objects.deepEquals(ModFields.read(field, source), ModFields.read(field, copy))) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameEndNoise(Object source, Object copy) {
        if (source == copy || source.getClass() != copy.getClass()
                || !source.getClass().getName().equals(NETHER + "biome.BiomeProviderTheEnd")) {
            return false;
        }
        Field noise = ModFields.bind(source.getClass().getName(), "islandNoise");
        Object left = ModFields.read(noise, source);
        Object right = ModFields.read(noise, copy);
        if (left == right || left.getClass() != net.minecraft.world.gen.NoiseGeneratorSimplex.class
                || right.getClass() != left.getClass()) {
            return false;
        }
        for (Field field : FIELDS.computeIfAbsent(left.getClass(), BiomeRuleComparison::fields)) {
            if (!Objects.deepEquals(ModFields.read(field, left), ModFields.read(field, right))) {
                return false;
            }
        }
        return true;
    }

    private static List<Field> fields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> owner = type; owner != Object.class; owner = owner.getSuperclass()) {
            for (Field field : owner.getDeclaredFields()) {
                // chunkSeed 是查询游标，由每次 initChunkSeed 覆盖，不是生成规则。
                if (!Modifier.isStatic(field.getModifiers()) && !field.getName().equals("chunkSeed")
                        && !field.getName().equals("field_75908_c")) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
        }
        return fields;
    }
}

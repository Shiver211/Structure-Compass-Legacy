package com.shiver.structurecompasslegacy.compat.dungeonadditions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.shiver.structurecompasslegacy.compat.dungeonadditions.DungeonArenaTask.Arena;
import com.shiver.structurecompasslegacy.api.StructureRegistry;
import com.shiver.structurecompasslegacy.compat.StructureCatalog;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.util.datafix.DataFixer;
import net.minecraftforge.fml.common.Loader;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public class DungeonArenaTest {

    private static final String ROOT = "com.dungeon_additions.da.";

    @BeforeAll
    static void initialize() throws Exception {
        Assumptions.assumeTrue(System.getProperty("compatModsDir") != null);
        Bootstrap.register();
        Field mods = field(Loader.class, "namedMods");
        if (mods.get(Loader.instance()) == null) {
            mods.set(Loader.instance(), Collections.emptyMap());
        }
    }

    @Test
    void gridMatchesInstalledMethodsAcrossSeedsAndNegativeCoordinates() throws Exception {
        Class<?> config = Class.forName(ROOT + "config.WorldConfig");
        AtomicLong seed = new AtomicLong();
        World world = mock(World.class, withSettings().stubOnly().defaultAnswer(call -> {
            if (call.getMethod().getName().equals("setRandomSeed")) {
                int x = call.getArgument(0);
                int z = call.getArgument(1);
                int salt = call.getArgument(2);
                return new Random(x * 341873128712L + z * 132897987541L + seed.get() + salt);
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(call);
        }));
        int hits = 0;
        for (Arena arena : Arena.values()) {
            boolean nether = arena == Arena.NETHER;
            Class<?> generator = Class.forName(ROOT + "world." + (nether
                    ? "nether_arena.WorldGenNetherArena" : "obsidilith_arena.WorldGenObsidilithArena"));
            Object instance = generator.getConstructor().newInstance();
            Method candidate = generator.getDeclaredMethod(nether ? "canStructureSpawnAtPos" : "canSpawnStructureAtPos",
                    World.class, int.class, int.class);
            candidate.setAccessible(true);
            Field spacing = nether ? config.getField("burning_arena_weight") : field(generator, "spacing");
            Object owner = nether ? null : instance;
            int originalSpacing = spacing.getInt(owner);
            Field types = field(generator, nether ? "burningFlameBiomeTypes" : "highCityBiomeTypes");
            Object originalTypes = types.get(null);
            // 空黑名单放行；白名单用含测试群系的已登记类型。
            net.minecraft.world.WorldProvider dimension = mock(net.minecraft.world.WorldProvider.class);
            field(World.class, "provider").set(world, dimension);
            org.mockito.Mockito.when(dimension.getBiomeForCoords(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(net.minecraft.init.Biomes.PLAINS);
            List<net.minecraftforge.common.BiomeDictionary.Type> allowed = new ArrayList<>();
            if (!nether) {
                allowed.add(net.minecraftforge.common.BiomeDictionary.Type.PLAINS);
            }
            types.set(null, allowed);
            try {
                for (long value : new long[] {0L, -1L, 918273645L, Long.MIN_VALUE}) {
                    seed.set(value);
                    for (int distance : new int[] {17, 32, 100}) {
                        spacing.setInt(owner, distance);
                        for (int x = -105; x <= 105; x++) {
                            for (int z = -105; z <= 105; z++) {
                                boolean expected = (Boolean) candidate.invoke(instance, world, x, z);
                                assertEquals(expected, DungeonArenaTask.candidate(arena, value, x, z, distance));
                                if (expected) {
                                    hits++;
                                }
                            }
                        }
                    }
                }
            } finally {
                spacing.setInt(owner, originalSpacing);
                types.set(null, originalTypes);
            }
        }
        assertTrue(hits > 100);
    }

    @Test
    void nativeLayoutsKeepTheStartingBuildingForAllNetherVariantsAndRotations() throws Exception {
        TemplateManager templates = new TemplateManager("unused-test-templates", new DataFixer(1343));
        String[][] layouts = {
                {"nether_arena.NetherArena", "startVault"},
                {"nether_arena.NetherArenaNBDefault", "startVault"},
                {"nether_arena.NetherArenaCrimson", "startVault"},
                {"nether_arena.NetherArenaWarped", "startVault"},
                {"obsidilith_arena.ObsidilithArena", "startArena"}
        };
        for (String[] entry : layouts) {
            Class<?> layout = Class.forName(ROOT + "world." + entry[0]);
            for (Rotation rotation : Rotation.values()) {
                for (int state = 0; state < 4; state++) {
                    World world = mock(World.class);
                    field(World.class, "rand").set(world, new Random(state));
                    List<StructureComponent> components = new ArrayList<>();
                    Object nativeLayout = layout.getConstructor(World.class, TemplateManager.class, List.class)
                            .newInstance(world, templates, components);
                    BlockPos origin = new BlockPos(-504, 80, 1016);
                    layout.getMethod(entry[1], BlockPos.class, Rotation.class).invoke(nativeLayout, origin, rotation);
                    assertTrue(components.size() >= 4);
                    Object first = components.get(0);
                    assertEquals(origin, first.getClass().getMethod("getTemplatePosition").invoke(first));
                    net.minecraft.world.gen.structure.template.Template template =
                            (net.minecraft.world.gen.structure.template.Template) first.getClass().getMethod("getTemplate").invoke(first);
                    assertTrue(template.getSize().getX() > 10, "native start template resource was missing");
                    assertTrue(template.getSize().getY() > 2, "native start template resource was empty");
                }
            }
        }
    }

    @Test
    void nativeStartsUseThePredictedAnchor() throws Exception {
        TemplateManager templates = new TemplateManager("unused-test-templates", new DataFixer(1343));
        ISaveHandler save = mock(ISaveHandler.class);
        org.mockito.Mockito.when(save.getStructureTemplateManager()).thenReturn(templates);
        Class<?> config = Class.forName(ROOT + "config.WorldConfig");
        for (Arena arena : Arena.values()) {
            boolean nether = arena == Arena.NETHER;
            Class<?> generator = Class.forName(ROOT + "world." + (nether
                    ? "nether_arena.WorldGenNetherArena" : "obsidilith_arena.WorldGenObsidilithArena"));
            Method start = generator.getDeclaredMethod("getStructureStart", World.class, int.class, int.class, Random.class);
            start.setAccessible(true);
            int height = config.getField(nether ? "burning_arena_y_level" : "obsidilith_y_height").getInt(null);
            for (int x : new int[] {-117, -1, 0, 89}) {
                World world = mock(World.class);
                field(World.class, "rand").set(world, new Random(x));
                org.mockito.Mockito.when(world.getSaveHandler()).thenReturn(save);
                StructureStart result = (StructureStart) start.invoke(generator.getConstructor().newInstance(), world, x, -37, new Random(0));
                assertTrue(result.isSizeableStructure());
                Object first = result.getComponents().get(0);
                assertEquals(DungeonArenaTask.anchor(arena, x, -37, height),
                        first.getClass().getMethod("getTemplatePosition").invoke(first));
            }
        }
    }

    @Test
    void moduleBindsBothInstalledEntries() {
        StructureRegistry registry = new StructureRegistry();
        StructureCatalog.register(registry, id -> id.equals("da"));
        new DungeonAdditionsModule().register(registry);
        assertTrue(registry.get("da:nether_arena").hasLocator());
        assertTrue(registry.get("da:obsidilith_arena").hasLocator());
        org.junit.jupiter.api.Assertions.assertNull(registry.get("da:high_city"));
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}

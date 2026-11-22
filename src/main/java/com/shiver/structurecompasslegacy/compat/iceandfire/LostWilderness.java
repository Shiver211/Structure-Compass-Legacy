package com.shiver.structurecompasslegacy.compat.iceandfire;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Random;

import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.MapGenRavine;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraftforge.event.terraingen.InitMapGenEvent;

/**
 * Read-only input from Lost Cities 3.0.0. Never call its generator or its static layout caches.
 */
final class LostWilderness {
    static final String ROOT = "mcjty.lostcities.dimensions.world.";
    static final String PROVIDER = ROOT + "LostWorldProvider";
    private static final String BETTER = "com.yungnickyoung.minecraft.bettercaves.world.";
    final long seed;
    final NoiseGeneratorOctaves low, high, main, depth;
    final NoiseGeneratorPerlin surface;
    final MapGenBase caves, ravines;
    final int mask, minRadius, maxRadius;
    final float cityChance;

    static LostWilderness match(WorldServer source) {
        Object gen = source.getChunkProvider().chunkGenerator;
        if (!gen.getClass().getName().equals(ROOT + "LostCityChunkGenerator")) {
            return null;
        }
        try {
            return new LostWilderness(source, gen);
        } catch (ReflectiveOperationException | UnsupportedOperationException ignored) {
            return null; // Unrecognized generator/profile: loaded evidence remains available.
        }
    }

    private LostWilderness(WorldServer source, Object gen) throws ReflectiveOperationException {
        Object p = field(gen, "profile");
        seed = (long) field(gen, "seed");
        require(seed == ((source.getSeed() >> 3) ^ 0x7FE293405L) && field(gen, "otherGenerator") == null
            && "chisel_optimized".equals(field(p, "name")) &&
            ((Enum<?>) field(p, "LANDSCAPE_TYPE")).name().equals("DEFAULT")
            && "".equals(field(p, "GENERATOR_OPTIONS")) && "minecraft:stone".equals(field(p, "BASE_BLOCK"))
            && "minecraft:water".equals(field(p, "LIQUID_BLOCK")) && source.getSeaLevel() == 63
            && (int) field(p, "GROUNDLEVEL") - (int) field(p, "WATERLEVEL_OFFSET") == 63
            && (float) field(p, "EXPLOSION_CHANCE") == 0 && (float) field(p, "MINI_EXPLOSION_CHANCE") == 0
            && (float) field(p, "CITY_THRESSHOLD") > 0 && (int) field(p, "MAX_CAVE_HEIGHT") == 128);
        Class<?> assets = Class.forName(ROOT + "lost.cityassets.AssetRegistries");
        for (String key : new String[]{"PREDEFINED_CITIES", "PREDEFINED_HIGHWAYS"}) {
            Object registry = assets.getField(key).get(null);
            for (Object asset : (Iterable<?>) registry.getClass().getMethod("getIterable").invoke(registry)) {
                require(
                    (int) asset.getClass().getMethod("getDimension").invoke(asset) != source.provider.getDimension());
            }
        }
        cityChance = (float) field(p, "CITY_CHANCE");
        mask = (int) field(p, "HIGHWAY_DISTANCE_MASK");
        minRadius = (int) field(p, "CITY_MINRADIUS");
        maxRadius = (int) field(p, "CITY_MAXRADIUS");
        require(minRadius > 0 && minRadius < maxRadius && maxRadius <= 256);
        // These exact vanilla noise classes only read their permutation tables and fill caller-owned buffers.
        Object t = field(gen, "terrainGenerator");
        high = noise(t, "maxLimitPerlinNoise", NoiseGeneratorOctaves.class);
        low = noise(t, "minLimitPerlinNoise", NoiseGeneratorOctaves.class);
        main = noise(t, "mainPerlinNoise", NoiseGeneratorOctaves.class);
        depth = noise(t, "depthNoise", NoiseGeneratorOctaves.class);
        surface = noise(t, "surfaceNoise", NoiseGeneratorPerlin.class);
        caves = (boolean) field(p, "GENERATE_CAVES") ? carver(field(gen, "caveGenerator"), true) : null;
        ravines = (boolean) field(p, "GENERATE_RAVINES") ? carver(field(gen, "ravineGenerator"), false) : null;
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(owner);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static <T> T noise(Object owner, String name, Class<T> type) throws ReflectiveOperationException {
        Object value = field(owner, name);
        require(value != null && value.getClass() == type);
        return type.cast(value);
    }

    private static MapGenBase carver(Object live, boolean cave) throws ReflectiveOperationException {
        // At MAX_CAVE_HEIGHT=128 LostGenCaves uses vanilla's nextInt(120) + 8.
        MapGenBase local = cave ? new MapGenCaves() : new MapGenRavine();
        String nativeType = cave ? ROOT + "LostGenCaves" : MapGenRavine.class.getName();
        if (live.getClass().getName().equals(nativeType)) {
            return local;
        }
        require(live.getClass().getName().equals(BETTER + (cave ? "MapGenBetterCaves" : "ravine.MapGenBetterRavine")));
        require(field(live, cave ? "defaultCaveGen" : "defaultRavineGen").getClass().getName().equals(nativeType));
        Constructor<InitMapGenEvent> ctor =
            InitMapGenEvent.class.getDeclaredConstructor(InitMapGenEvent.EventType.class, MapGenBase.class);
        ctor.setAccessible(true);
        InitMapGenEvent event =
            ctor.newInstance(cave ? InitMapGenEvent.EventType.CAVE : InitMapGenEvent.EventType.RAVINE, local);
        return (MapGenBase) live.getClass().getConstructor(InitMapGenEvent.class).newInstance(event);
    }

    private static void require(boolean supported) {
        if (!supported) {
            throw new UnsupportedOperationException();
        }
    }

    boolean wilderness(int x, int z) {
        return wilderness(seed, mask, minRadius, maxRadius, cityChance, x, z);
    }

    static boolean wilderness(long seed, int mask, int min, int max, float chance, int x, int z) {
        // Rails, adjacent railway dungeons and highway grading. Floor-mod also covers negative chunks.
        int rz = Math.floorMod(z + 1, 10);
        if (Math.floorMod(x + 1, 10) == 5 || rz <= 1 || rz == 9) {
            return false;
        }
        for (int i = -1; mask > 0 && i <= 1; i++) {
            if (((x + i) & mask) == 0 || ((z + i) & mask) == 0) {
                return false;
            }
        }
        // No possible city in the 3x3 neighbourhood, even before biome/structure exclusions.
        int reach = (max + 15) / 16 + 1;
        Random r = new Random(0);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                long cx = x + dx, cz = z + dz;
                r.setSeed(seed + cz * 797003437L + cx * 295075153L);
                r.nextFloat();
                r.nextFloat();
                if (r.nextFloat() >= chance) {
                    continue;
                }
                r.setSeed(seed + cz * 100001653L + cx * 295075153L);
                r.nextFloat();
                r.nextFloat();
                int radius = min + r.nextInt(max - min);
                int ax = Math.max(0, Math.abs(dx) - 1) * 16, az = Math.max(0, Math.abs(dz) - 1) * 16;
                if (ax * ax + az * az < radius * radius) {
                    return false;
                }
            }
        }
        return true;
    }
}

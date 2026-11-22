package com.shiver.structurecompasslegacy.compat.iceandfire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import com.shiver.structurecompasslegacy.api.SearchContext;
import com.shiver.structurecompasslegacy.compat.netherapi.NetherApiModule;

import net.minecraft.init.Biomes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraftforge.common.BiomeDictionary;

import static net.minecraftforge.common.BiomeDictionary.Type.*;
import static com.shiver.structurecompasslegacy.compat.iceandfire.IceLandmark.*;

/**
 * Follows WorldGenEvents up to the first feature whose random consumption we cannot reproduce.
 */
final class IcePlacement {
    private static final int SKIP_GORGON = 1, SKIP_CYCLOPS = 2, SKIP_PIXIES = 4, SKIP_ROOSTS = 8;
    private final long seed, seedX, seedZ;
    private final Random random = new Random(0);
    private final BlockPos spawn;
    private final Map<String, Object> settings;
    private final Function<String, BlockPos> previous;
    private final Map<Biome, Rules> biomeRules = new IdentityHashMap<>();
    private final Set<ResourceLocation> pixieBiomes = new HashSet<>();
    private final Map<IceLandmark, List<Biome>> candidates = new EnumMap<>(IceLandmark.class);
    private List<Biome> profiles;
    private final boolean dragons, structures;

    static IcePlacement forSearch(SearchContext context, IceAndFireModule config) {
        if (!NetherApiModule.hasExistingSpawn(context.source)) {
            return null;
        }
        BlockPos spawn = context.source.provider.getSpawnPoint();
        WorldBorder border = context.source.getWorldBorder();
        // Match World.getSpawnPoint's X/Z fallback without loading terrain just to obtain Y.
        if (!border.contains(spawn)) {
            spawn = new BlockPos(border.getCenterX(), 0, border.getCenterZ());
        }
        return new IcePlacement(context, config, spawn);
    }

    private IcePlacement(SearchContext context, IceAndFireModule config, BlockPos spawn) {
        this(context.seed, spawn, config.snapshot(), config.allows(context.dimension, true),
            config.allows(context.dimension, false), config::last);
    }

    IcePlacement(long seed, BlockPos spawn, Map<String, Object> settings, boolean dragons, boolean structures,
            Function<String, BlockPos> previous) {
        this.seed = seed;
        this.spawn = spawn.toImmutable();
        this.settings = settings;
        this.dragons = dragons;
        this.structures = structures;
        this.previous = previous;
        Random initial = new Random(seed);
        // 与 Forge generateWorld 保持相同的移位优先级和 long 溢出规则。
        seedX = initial.nextLong() >> 3;
        seedZ = initial.nextLong() >> 3;
        for (String id : (String[]) settings.get("pixieVillageBiomeBlacklist")) {
            pixieBiomes.add(new ResourceLocation(id));
        }
    }

    // Horizontally distant implies distant in 3D for any future terrain height. For ambiguous
    // spacing, abandon this candidate rather than guessing whether a preceding roll was consumed.
    private boolean clear(String field, BlockPos p, boolean ignoreSpacing) {
        BlockPos last = ignoreSpacing ? null : previous.apply(field);
        if (last == null) {
            return true;
        }
        long dx = (long) p.getX() - last.getX();
        long dz = (long) p.getZ() - last.getZ();
        int spacing = integer("worldGenDistance");
        return dx * dx + dz * dz >= (double) (spacing * spacing);
    }

    private boolean roll(Random random, String setting) {
        return random.nextInt(integer(setting) + 1) == 0;
    }

    // Returns the native anchor column; underground Y is the actual native draw, not a surface estimate.
    BlockPos predict(IceLandmark kind, Biome b, BlockPos p) {
        return predict(kind, b, p, null);
    }

    static final class MausoleumRoll {
        EnumFacing facing;
    }

    BlockPos predict(IceLandmark kind, Biome b, BlockPos p, MausoleumRoll tomb) {
        return predict(kind, b, p, tomb, false, 0, null);
    }

    boolean competes(IceLandmark kind, Biome targetBiome, Biome otherBiome, BlockPos other) {
        // 邻近候选不能用当前 last 排除：探索过程中 last 会被别的区块改变。
        Rules b = rules(otherBiome);
        int options = structures && flag("spawnGorgons") && b.beach ? SKIP_GORGON : 0;
        if (kind != SIREN_ISLAND) {
            if (flag("generateWanderingCyclops") && b.plains) {
                options |= SKIP_CYCLOPS;
            }
            if (structures && flag("spawnPixies") && b.pixie) {
                options |= SKIP_PIXIES;
            }
            if (dragons && flag("generateDragonRoosts")) {
                options |= SKIP_ROOSTS;
            }
        }
        // 这些前置分支在间距不满足时连概率都不抽；枚举允许/跳过的原生路径。
        for (int skipped = 0; skipped <= options; skipped++) {
            if ((skipped & ~options) == 0
                && predict(kind, otherBiome, other, null, true, skipped, targetBiome) != null) {
                return true;
            }
        }
        return false;
    }

    boolean mayPredict(IceLandmark kind, int x, int z) {
        BlockPos center = new BlockPos(x * 16 + 8, 0, z * 16 + 8);
        for (Biome biome : candidates.computeIfAbsent(kind, this::candidateBiomes)) {
            if (predict(kind, biome, center, null, true, 0, null) != null) {
                return true;
            }
        }
        return false;
    }

    boolean mayCompete(IceLandmark kind, Biome targetBiome, BlockPos center) {
        for (Biome biome : profiles()) {
            if (competes(kind, targetBiome, biome, center)) {
                return true;
            }
        }
        return false;
    }

    private List<Biome> candidateBiomes(IceLandmark kind) {
        List<Biome> result = new ArrayList<>();
        for (Biome biome : profiles()) {
            if (eligible(kind, biome)) {
                result.add(biome);
            }
        }
        return result;
    }

    private List<Biome> profiles() {
        if (profiles == null) {
            profiles = new ArrayList<>();
            Set<List<Boolean>> seen = new HashSet<>();
            for (Biome biome : Biome.REGISTRY) {
                Rules b = rules(biome);
                // 所有会影响随机调用路径的群系条件；相同条件只需试一个代表。
                List<Boolean> key = Arrays.asList(b.beach, b.ocean, b.cold, b.snowy, b.swamp, b.jungle,
                    b.savanna, b.lightning, b.fire, b.hills, b.mountain, b.plains, b.dry, b.sandy, b.hot,
                    b.nether, b.pixie, biome == Biomes.ICE_PLAINS);
                if (seen.add(key)) {
                    profiles.add(biome);
                }
            }
        }
        return profiles;
    }

    private BlockPos predict(IceLandmark kind, Biome b, BlockPos p, MausoleumRoll tomb,
            boolean ignoreSpacing, int skipped, Biome targetBiome) {
        if (targetBiome == null && !eligible(kind, b)) {
            return null;
        }
        Random r = random;
        r.setSeed((seedX * (p.getX() >> 4) + seedZ * (p.getZ() >> 4)) ^ seed);
        long dx = (long) p.getX() - spawn.getX(), dz = (long) p.getZ() - spawn.getZ();
        int limit = integer("dangerousWorldGenDistanceLimit");
        boolean far = dx * dx + dz * dz > (double) (limit * limit);
        Rules rules = rules(b);
        boolean beach = rules.beach, ocean = rules.ocean, cold = rules.cold, snowy = rules.snowy;
        boolean swamp = rules.swamp, jungle = rules.jungle, savanna = rules.savanna;
        boolean lightning = rules.lightning;

        if ((skipped & SKIP_GORGON) == 0 && structures && beach && flag("spawnGorgons")
            && (!clear("lastGorgonTemple", p, ignoreSpacing) || roll(r, "spawnGorgonsChance"))) {
            return interrupted(kind, targetBiome, b, p, "lastGorgonTemple");
        }
        if (far && ocean && !cold && flag("generateSirenIslands") && roll(r, "generateSirenChance")
            && structures) {
            if (kind == SIREN_ISLAND) {
                return clear(SIREN_ISLAND.last, p, ignoreSpacing) ? p : null;
            }
            return interrupted(kind, targetBiome, b, p, SIREN_ISLAND.last);
        }
        if (kind == SIREN_ISLAND) {
            return null;
        }
        // Not a search target, but its roll advances the RNG before the dragon and mausoleum rolls.
        if (far && beach && flag("generateCyclopsCaves") && roll(r, "spawnCyclopsCaveChance")
            && structures) {
            return interrupted(kind, targetBiome, b, p, "lastCyclopsCave");
        }
        if ((skipped & SKIP_CYCLOPS) == 0 && far && rules.plains && flag("generateWanderingCyclops")
            && (!clear("lastCyclopsCave", p, ignoreSpacing) || roll(r, "spawnWanderingCyclopsChance"))) {
            return interrupted(kind, targetBiome, b, p, "lastCyclopsCave");
        }
        if ((skipped & SKIP_PIXIES) == 0 && far && structures && flag("spawnPixies")) {
            if (rules.pixie && (!clear("lastPixieVillage", p, ignoreSpacing) || roll(r, "spawnPixiesChance"))) {
                return interrupted(kind, targetBiome, b, p, "lastPixieVillage");
            }
        }

        boolean fire = rules.fire;
        boolean hills = rules.hills, mountain = rules.mountain;
        if ((skipped & SKIP_ROOSTS) == 0 && far && dragons && flag("generateDragonRoosts")) {
            if (!clear(FIRE_ROOST.last, p, ignoreSpacing)) {
                return null;
            }
            int bound = integer("generateDragonRoostChance") * (hills || mountain && !snowy ? 1 : 2) + 1;
            if (fire && b != Biomes.ICE_PLAINS && r.nextInt(bound) == 0) {
                return kind == FIRE_ROOST ? p : interrupted(kind, targetBiome, b, p, FIRE_ROOST.last);
            }
            if (cold && snowy && r.nextInt(bound) == 0) {
                return kind == ICE_ROOST ? p : interrupted(kind, targetBiome, b, p, FIRE_ROOST.last);
            }
            if (lightning && r.nextInt(bound) == 0) {
                return kind == LIGHTNING_ROOST ? p : interrupted(kind, targetBiome, b, p, FIRE_ROOST.last);
            }
        }
        if (kind.roost() || targetBiome != null && !eligibleFamily(kind, b)) {
            return null;
        }
        if (dragons && flag("generateDragonSkeletons")) {
            if (rules.dry && rules.sandy) {
                skeleton(r);
            }
            if (cold && snowy) {
                skeleton(r);
            }
            if (lightning) {
                skeleton(r);
            }
        }
        if (ocean && flag("spawnHippocampus") && roll(r, "hippocampusSpawnChance")) {
            return interrupted(kind, targetBiome, b, p, null);
        }
        if (ocean && flag("spawnSeaSerpents") && roll(r, "seaSerpentSpawnChance")) {
            return interrupted(kind, targetBiome, b, p, null);
        }
        if (far && swamp && flag("spawnStymphalianBirds") && roll(r, "stymphalianBirdSpawnChance")) {
            return interrupted(kind, targetBiome, b, p, null);
        }
        if (far && dragons && flag("generateDragonDens")) {
            if (!clear(FIRE_CAVE.last, p, ignoreSpacing)) {
                return null;
            }
            int bound = integer("generateDragonDenChance") * (hills || mountain ? 1 : 2) + 1;
            if (fire && !beach && r.nextInt(bound) == 0) {
                return kind == FIRE_CAVE || targetBiome != null ? p.up(20 + r.nextInt(20)) : null;
            }
            if (cold && snowy && !beach && r.nextInt(bound) == 0) {
                return kind == ICE_CAVE || targetBiome != null ? p.up(20 + r.nextInt(20)) : null;
            }
            if (lightning && r.nextInt(bound) == 0) {
                return kind == LIGHTNING_CAVE || targetBiome != null ? p.up(20 + r.nextInt(20)) : null;
            }
        }
        if (kind.material != null) {
            return null;
        }
        // Veins consume random inside WorldGenMinable. No guessed RNG offset for modded ore placement.
        if (flag("generateSilverOre") || flag("generateCopperOre")) {
            return null;
        }
        if (snowy && flag("generateSapphireOre")) {
            consumeOre(r);
        }
        if (savanna && flag("generateAmythestOre")) {
            consumeOre(r);
        }
        if (flag("generateMyrmexColonies") && r.nextInt(integer("myrmexColonyGenChance")) == 0
            && far && structures && (jungle || rules.hot && rules.dry && rules.sandy)) {
            return null;
        }
        if (dragons) {
            if (cold && snowy) {
                r.nextInt(15);
            }
            if (rules.hot && rules.sandy) {
                r.nextInt(15);
            }
            if (rules.nether) {
                r.nextInt(15);
            }
            if (lightning) {
                r.nextInt(15);
            }
        }
        if (structures && cold && snowy && flag("generateMausoleums")) {
            if (!clear(MAUSOLEUM.last, p, ignoreSpacing)) {
                return null;
            }
            if (roll(r, "generateMausoleumChance")) {
                int direction = r.nextInt(3); // Native bound is 3, not 4.
                if (tomb != null) {
                    tomb.facing = EnumFacing.byHorizontalIndex(direction);
                }
                return p.add(r.nextInt(8) - 4, 0, r.nextInt(8) - 4);
            }
        }
        return null;
    }

    private void skeleton(Random r) {
        if (roll(r, "generateDragonSkeletonChance")) {
            r.nextInt(100);
            r.nextBoolean();
            r.nextInt(360);
        }
    }

    private static void consumeOre(Random r) {
        int count = 3 + r.nextInt(6);
        for (int i = 0; i < count; i++) {
            r.nextInt(28);
            r.nextInt(16);
            r.nextInt(16);
        }
    }

    int spacing() {
        return integer("worldGenDistance");
    }

    private boolean flag(String key) {
        return (Boolean) settings.get(key);
    }

    private int integer(String key) {
        return (Integer) settings.get(key);
    }

    private BlockPos interrupted(IceLandmark kind, Biome targetBiome, Biome otherBiome, BlockPos p, String field) {
        if (targetBiome == null) {
            return null;
        }
        // 前置结构内部消耗未知随机数时，邻近点仍可能有龙洞，不能当作不存在。
        return eligibleFamily(kind, otherBiome) || affectsPrefix(kind, targetBiome, field) ? p : null;
    }

    private boolean affectsPrefix(IceLandmark kind, Biome biome, String field) {
        Rules b = rules(biome);
        if ("lastGorgonTemple".equals(field)) {
            return structures && flag("spawnGorgons") && b.beach;
        }
        if (kind == SIREN_ISLAND) {
            return false;
        }
        if (SIREN_ISLAND.last.equals(field)) {
            return structures && flag("generateSirenIslands") && b.ocean && !b.cold;
        }
        if ("lastCyclopsCave".equals(field)) {
            return structures && flag("generateCyclopsCaves") && b.beach
                || flag("generateWanderingCyclops") && b.plains;
        }
        if ("lastPixieVillage".equals(field)) {
            return structures && flag("spawnPixies") && b.pixie;
        }
        return FIRE_ROOST.last.equals(field) && dragons && flag("generateDragonRoosts")
            && (b.fire && biome != Biomes.ICE_PLAINS || b.ice || b.lightning);
    }

    private boolean eligibleFamily(IceLandmark kind, Biome biome) {
        if (kind.material == null) {
            return eligible(kind, biome);
        }
        return eligible(FIRE_CAVE, biome) || eligible(ICE_CAVE, biome) || eligible(LIGHTNING_CAVE, biome);
    }

    private boolean eligible(IceLandmark kind, Biome biome) {
        Rules b = rules(biome);
        switch (kind) {
            case FIRE_ROOST:
                return b.fire && biome != Biomes.ICE_PLAINS;
            case ICE_ROOST:
                return b.ice;
            case LIGHTNING_ROOST:
                return b.lightning;
            case FIRE_CAVE:
                return b.fire && !b.beach;
            case ICE_CAVE:
                return b.ice && !b.beach;
            case LIGHTNING_CAVE:
                return b.lightning;
            case SIREN_ISLAND:
                return b.ocean && !b.cold;
            case MAUSOLEUM:
                return b.ice;
            default:
                throw new IllegalArgumentException(kind.name());
        }
    }

    private Rules rules(Biome biome) {
        return biomeRules.computeIfAbsent(biome, Rules::new);
    }

    private final class Rules {
        final boolean beach, ocean, cold, snowy, swamp, jungle, savanna, lightning, ice, fire;
        final boolean hills, mountain, plains, dry, sandy, hot, nether, pixie;

        Rules(Biome b) {
            beach = has(b, BEACH);
            ocean = has(b, OCEAN);
            cold = has(b, COLD);
            snowy = has(b, SNOWY);
            swamp = has(b, SWAMP);
            jungle = has(b, JUNGLE);
            savanna = has(b, SAVANNA);
            lightning = jungle || has(b, MESA) || savanna;
            ice = cold && snowy;
            fire = !b.getEnableSnow() && b.getDefaultTemperature() > 0 && !cold && !snowy
                && !has(b, WET) && !ocean && !has(b, RIVER);
            hills = has(b, HILLS);
            mountain = has(b, MOUNTAIN);
            plains = has(b, PLAINS);
            dry = has(b, DRY);
            sandy = has(b, SANDY);
            hot = has(b, HOT);
            nether = has(b, NETHER);
            pixie = pixieBiomes.contains(b.getRegistryName()) == flag("pixieVillageBiomeBlacklistIsWhitelist");
        }
    }

    private static boolean has(Biome biome, BiomeDictionary.Type type) {
        return BiomeDictionary.hasType(biome, type);
    }
}

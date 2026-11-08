package com.shiver.structurecompasslegacy.verification;

import java.util.ArrayList;
import java.util.List;

import com.shiver.structurecompasslegacy.api.SearchBudget;
import com.shiver.structurecompasslegacy.mixin.MapGenBaseAccessor;
import com.shiver.structurecompasslegacy.mixin.MapGenStructureInvoker;
import com.shiver.structurecompasslegacy.search.SeedMath;
import com.shiver.structurecompasslegacy.search.SearchWork;
import com.shiver.structurecompasslegacy.simulation.SimulationStage;
import com.shiver.structurecompasslegacy.simulation.SimulationWorld;
import com.shiver.structurecompasslegacy.simulation.VanillaBiomeSafety;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureOceanMonument;
import net.minecraft.world.gen.structure.WoodlandMansion;

/** 只审核本次开发验证需要的地形；不调用结构布局、村庄处理器或整包 populate。 */
final class DungeonTerrain implements SimulationStage {

    private final SimulationWorld world;
    private final List<MapGenStructure> structures;
    private final int chunkX;
    private final int chunkZ;
    private final StrongholdRing strongholds;
    private final SearchWork work;
    private int checkedBiomes;
    private int structureIndex;
    private int startIndex;
    private int generated;
    private String rejection = "";

    DungeonTerrain(SimulationWorld world, List<MapGenStructure> structures, StrongholdRing strongholds,
            SearchWork work, int chunkX, int chunkZ) {
        this.world = world;
        this.structures = structures;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.strongholds = strongholds;
        this.work = work;
    }

    static List<MapGenStructure> createStructureChecks(SimulationWorld world, ChunkGeneratorSettings settings) {
        List<MapGenStructure> checks = new ArrayList<>();
        if (!world.getWorldInfo().isMapFeaturesEnabled()) {
            return checks;
        }
        if (settings.useMineShafts) {
            checks.add(new MapGenMineshaft());
        }
        if (settings.useVillages) {
            checks.add(new MapGenVillage());
        }
        if (settings.useTemples) {
            checks.add(new MapGenScatteredFeature());
        }
        if (settings.useMonuments) {
            checks.add(new StructureOceanMonument());
        }
        if (settings.useMansions) {
            checks.add(new WoodlandMansion((ChunkGeneratorOverworld) world.getChunkProvider().chunkGenerator));
        }
        for (MapGenStructure structure : checks) {
            ((MapGenBaseAccessor) structure).compassSetWorld(world);
        }
        return checks;
    }

    @Override
    public Result advance(SearchBudget budget) {
        // 先审核全部 2×2 区块，再生成其中任何一个，避免边界群系漏检。
        while (checkedBiomes < 4 && !budget.exhausted()) {
            int x = chunkX + checkedBiomes % 2;
            int z = chunkZ + checkedBiomes / 2;
            Biome[] biomes = work.measure("biomes", () ->
                    world.getBiomeProvider().getBiomes(null, x * 16, z * 16, 16, 16));
            for (Biome biome : biomes) {
                if (!VanillaBiomeSafety.supports(biome)) {
                    rejection = "shared_biome_state";
                    return Result.REJECTED;
                }
            }
            checkedBiomes++;
        }
        if (checkedBiomes < 4) {
            return Result.RUNNING;
        }

        if (strongholds != null) {
            while (!strongholds.complete() && !budget.exhausted()) {
                work.run("stronghold.position", () -> strongholds.advance(world.getBiomeProvider()));
            }
            if (!strongholds.complete()) {
                return Result.RUNNING;
            }
            if (strongholds.intersects(chunkX - 8, chunkZ - 8, chunkX + 9, chunkZ + 9)) {
                rejection = "large_structure";
                return Result.REJECTED;
            }
        }

        // MapGenBase.range=8；2×2 地形的起点并集为 [chunk-8, chunk+9]。
        // 发现任何可能的起点即跳过，不生成未审核的结构组件。
        while (structureIndex < structures.size() && !budget.exhausted()) {
            MapGenStructure structure = structures.get(structureIndex);
            int x = chunkX - 8 + startIndex % 18;
            int z = chunkZ - 8 + startIndex / 18;
            ((MapGenBaseAccessor) structure).compassSetRandom(SeedMath.mapGen(world.getSeed(), x, z));
            if (work.measure("structure." + structure.getStructureName(), () ->
                    ((MapGenStructureInvoker) structure).compassCanSpawn(x, z))) {
                rejection = "large_structure";
                return Result.REJECTED;
            }
            startIndex++;
            if (startIndex == 18 * 18) {
                structureIndex++;
                startIndex = 0;
            }
        }
        if (structureIndex < structures.size()) {
            return Result.RUNNING;
        }

        while (generated < 4 && !budget.exhausted()) {
            work.run("terrain.chunk", () -> world.getChunk(chunkX + generated % 2, chunkZ + generated / 2));
            generated++;
        }
        return generated == 4 ? Result.COMPLETE : Result.RUNNING;
    }

    String rejection() {
        return rejection;
    }
}

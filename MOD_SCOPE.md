# 计划兼容范围与安装分支

范围来自 PLAN.md，分支由 2026-11-08 安装目录的 JAR 核对；不读取整合包配置来删减范围。
共 64 项模组/补丁，包含生成环境兼容。原版另见 COMPATIBILITY.md。
这张表用于追踪范围，不代表已经枚举全部自然结构。未完成审计的条目不得计作兼容完成。
文件名保留 patched / RotN 等分支标识；部分模组内置版本号落后于文件名，以列出的实际 JAR 为基线。

| 模组 | 兼容组 | 安装分支 | 当前工作 |
|---|---|---|---|
| The Aether (`aether_legacy`) | 规则预测 | `aether-1.12.2-v1.5.4.1.jar` | 已列 3 项，2 项有定位器；其余见逐结构表，继续核对缺项 |
| Atum (`atum`) | 规则预测 | `Atum-1.12.2-2.0.21.jar` | 已列 9 项，5 项有定位器；其余见逐结构表，继续核对缺项 |
| The Twilight Forest (`twilightforest`) | 规则预测 | `twilightforest-1.12.2-3.15.1.jar` | 已列 17 项，16 项有定位器；其余见逐结构表，继续核对缺项 |
| TofuCraftReload (`tofucraft`) | 规则预测 | `TofuCraftReload-0.1.0.9.jar` | 已列 4 项，3 项有定位器；其余见逐结构表，继续核对缺项 |
| Thaumic Augmentation (`thaumicaugmentation`) | 规则预测 | `ThaumicAugmentation-1.12.2-2.1.14.jar` | 已列 2 项，1 项有定位器；其余见逐结构表，继续核对缺项 |
| Draconic Evolution (`draconicevolution`) | 规则预测 | `Draconic-Evolution-1.12.2-2.3.28.193-universal_tweaked-patched.jar` | 已列 2 项，2 项有定位器；其余见逐结构表，继续核对缺项 |
| AbyssalCraft (`abyssalcraft`) | 混合生成 | `AbyssalCraft-1.12.2-1.11.3.jar` | 已列 20 项，3 项有定位器；其余见逐结构表，继续核对缺项 |
| Divinerpg (`divinerpg`) | 混合生成 | `divinerpg-1.0.0-patched.jar` | 已列 22 项，12 项有定位器；其余见逐结构表，继续核对缺项 |
| Journey-Into-The-Light (`journey`) | 混合生成 | `journey-1.0.5.jar` | 已列 7 项，0 项有定位器；其余见逐结构表，继续核对缺项 |
| The Aurorian (`theaurorian`) | 混合生成 | `theaurorian-1.12.2-1.2.jar` | 已列 7 项，3 项有定位器；其余见逐结构表，继续核对缺项 |
| End Expansion (`endexpansion`) | 混合生成 | `EndExpansion-1.2-fix.jar` | 已列 8 项，1 项有定位器；其余见逐结构表，继续核对缺项 |
| Depths of Madness (`db`) | 混合生成 | `The Depths of Madness - 0.2.jar` | 待逐结构审计；不等于已确认不支持 |
| Unseen's Dungeon Additions (`da`) | 混合生成 | `unseens_dungeon_additions-1.7.4.1.jar` | 待逐结构审计；不等于已确认不支持 |
| Raids (`raids`) | 混合生成 | `Raids-Backport-1.12.2-1.1.5.jar` | 已列 1 项，0 项有定位器；其余见逐结构表，继续核对缺项 |
| Lost Cities LTS (`lostcities`) | 混合生成 | `lostcities-1.12-3.0.0.jar` | 待逐结构审计；不等于已确认不支持 |
| Astral Sorcery (`astralsorcery`) | 常规生成 | `astralsorcery-1.12.2-1.10.27.jar` | 待逐结构审计；不等于已确认不支持 |
| Thaumcraft (`thaumcraft`) | 常规生成 | `Thaumcraft-1.12.2-6.1.BETA26.jar` | 待逐结构审计；不等于已确认不支持 |
| Defiled Lands (`defiledlands`) | 常规生成 | `defiledlands-1.12.2-1.4.3.jar` | 待逐结构审计；不等于已确认不支持 |
| Embers Unofficial Extended Life (`embers`) | 常规生成 | `embers-1.26.1.jar` | 待逐结构审计；不等于已确认不支持 |
| End: Reborn (`endreborn`) | 常规生成 | `EndReborn [0.3.9].jar` | 待逐结构审计；不等于已确认不支持 |
| EvilCraft (`evilcraft`) | 常规生成 | `EvilCraft-1.12.2-0.10.78.jar` | 待逐结构审计；不等于已确认不支持 |
| Lost Aether Content (`lost_aether`) | 常规生成 | `lost-aether-content-1.12.2-1.0.2.jar` | 待逐结构审计；不等于已确认不支持 |
| Mowzie's Mobs (`mowziesmobs`) | 常规生成 | `mowziesmobs_1.12.2_1.5.8.jar` | 待逐结构审计；不等于已确认不支持 |
| Oceanic Expanse (`oe`) | 常规生成 | `OceanicExpanse-1.2.2.jar` | 待逐结构审计；不等于已确认不支持 |
| Project: Vibrant Journeys (`pvj`) | 常规生成 | `ProjectVibrantJourneys-1.12.2-1.6.4.jar` | 待逐结构审计；不等于已确认不支持 |
| Cherry_on_1.12.2 (`suikecherry`) | 常规生成 | `Cherry_on-1.12.2-1.3.35.jar` | 待逐结构审计；不等于已确认不支持 |
| Quark: RotN Edition (`quark`) | 常规生成 | `QuarkRotN-r1.6-192.jar` | 待逐结构审计；不等于已确认不支持 |
| Quantum Things (`randomthings`) | 常规生成 | `QuantumThings-MC-1.12.2-1.3.0.jar` | 待逐结构审计；不等于已确认不支持 |
| Stygian End (`stygian`) | 常规生成 | `stygian-1.0.5.jar` | 待逐结构审计；不等于已确认不支持 |
| Moar TCon (`moretcon`) | 常规生成 | `moretcon-V15b7a3.jar` | 待逐结构审计；不等于已确认不支持 |
| CoFH World (`cofhworld`) | 常规生成 | `CoFHWorld-1.12.2-1.4.0.1-universal.jar` | 待逐结构审计；不等于已确认不支持 |
| AE2 Unofficial Extended Life (`appliedenergistics2`) | 状态适配 | `ae2-uel-0.57.0-patched.jar` | 待逐结构审计；不等于已确认不支持 |
| AE2 Meteorite Addon (`ae2_meteorite_addon`) | 状态适配 | `ae2_meteorite_addon-1.5.jar` | 待逐结构审计；不等于已确认不支持 |
| AtomicStryker's Battletowers (`AS_BattleTowers`) | 状态适配 | `BattleTowers-1.12.2.jar` | 待逐结构审计；不等于已确认不支持 |
| Battle Towers Fixes (`btfixes`) | 状态适配 | `BTFixes-1.12.2-1.2.2.jar` | 待逐结构审计；不等于已确认不支持 |
| Ruins Spawning System (`AS_Ruins`) | 状态适配 | `Ruins-1.12.2-patched.jar` | 待逐结构审计；不等于已确认不支持 |
| Recurrent Complex (`reccomplex`) | 状态适配 | `RecurrentComplex-1.4.8.7.jar` | 地形接口隔离已核对；完整自然前置流程与结构清单待适配 |
| GCT-Core (`gctcore`) | 状态适配 | `GCT-Core-1.0.41.jar` | 待逐结构审计；不等于已确认不支持 |
| Gct Additions (`gct_additions`) | 状态适配 | `gct_additions-1.1.1.jar` | 待逐结构审计；不等于已确认不支持 |
| Ice and Fire: RotN Edition (`iceandfire`) | 状态适配 | `IceAndFireRotN-1.9.1-1.3.3.jar` | 待逐结构审计；不等于已确认不支持 |
| Erebus (`erebus`) | 状态适配 | `Erebus-1.0.32-patched.jar` | 待逐结构审计；不等于已确认不支持 |
| The Betweenlands (`thebetweenlands`) | 状态适配 | `TheBetweenlands-3.9.6-universal.jar` | 已列 16 项，2 项有定位器；其余见逐结构表，继续核对缺项 |
| Tinkers Alloying Addon (`taiga`) | 状态适配 | `TAIGA-Replant-Replant(1.0.0-beta).jar` | 待逐结构审计；不等于已确认不支持 |
| Tinkers' Antique (`tconstruct`) | 状态适配 | `TinkersAntique-1.12.2-2.13.0.209.jar` | 待逐结构审计；不等于已确认不支持 |
| Waystones (`waystones`) | 状态适配 | `Waystones_1.12.2-4.1.0.jar` | 待逐结构审计；不等于已确认不支持 |
| Lycanites Mobs (`lycanitesmobs`) | 状态适配 | `lycanitesmobs-1.12.2-2.0.8.10.jar` | 待逐结构审计；不等于已确认不支持 |
| Dungeons Mod (`dungeonsmod`) | 状态适配 | `DungeonsMod-1.12.2-1.0.8.jar` | 待逐结构审计；不等于已确认不支持 |
| BetterNether RotN (`betternether`) | 状态适配 | `BetterNetherRotN-1.12.2-0.1.9.4.jar` | 待逐结构审计；不等于已确认不支持 |
| NetherEx (`netherex`) | 状态适配 | `NetherEx-1.12.2-2.2.5.jar` | 待逐结构审计；不等于已确认不支持 |
| Netherized (`netherized`) | 状态适配 | `Netherized-1.12.2-0.3.8B.jar` | 待逐结构审计；不等于已确认不支持 |
| Better End (`betterendforge`) | 状态适配 | `Better End-1.1.1-1.12.2.jar` | 待逐结构审计；不等于已确认不支持 |
| Actually Additions (`actuallyadditions`) | 村庄组件 | `ActuallyAdditions-1.12.2-r152.jar` | 待逐结构审计；不等于已确认不支持 |
| Binnie Core (`binniecore`) | 村庄组件 | `binnie-mods-1.12.2-2.5.1.213.jar` | 待逐结构审计；不等于已确认不支持 |
| Bountiful (`bountiful`) | 村庄组件 | `Bountiful-1.12.2-2.2.3.jar` | 待逐结构审计；不等于已确认不支持 |
| Cyclic (`cyclicmagic`) | 村庄组件 | `Cyclic-1.12.2-1.20.14.jar` | 待逐结构审计；不等于已确认不支持 |
| Forestry (`forestry`) | 村庄组件 | `forestry_1.12.2-5.8.2.427.jar` | 待逐结构审计；不等于已确认不支持 |
| Sakura Reforged (`sakura`) | 村庄组件 | `sakura-1.2.0.jar` | 待逐结构审计；不等于已确认不支持 |
| Biomes O' Plenty (`biomesoplenty`) | 生成环境 | `BiomesOPlenty-1.12.2-7.0.1.2844-universal-patched.jar` | 待逐结构审计；不等于已确认不支持 |
| BetterCaves (`bettercaves`) | 生成环境 | `BetterCaves-2.0.7.jar` | 待逐结构审计；不等于已确认不支持 |
| Nether API (`nether_api`) | 生成环境 | `Nether-API-v1.4.5-mc1.12.2.jar` | 独立末地地形已接入；其他生成环境待适配 |
| Extra Utilities 2 (`extrautils2`) | 生成环境 | `extrautils2-1.12-1.9.9.jar` | 待逐结构审计；不等于已确认不支持 |
| Just Enough Dimensions (`justenoughdimensions`) | 生成环境 | `justenoughdimensions-1.12.2-1.6.0-dev.20211009.214847.jar` | 待逐结构审计；不等于已确认不支持 |
| Elemental End Portal (`elementalend`) | 生成环境 | `elementalend-1.0.0.jar` | 待逐结构审计；不等于已确认不支持 |
| Aether Continuation (`aether_legacy_addon`) | 额外入口待审计 | `Aether Continuation v1.3.0.jar` | 待逐结构审计；不等于已确认不支持 |

下一步：优先审计冒险模组中可可靠预测的结构，继续交错次元的装饰和区域存储隔离，补齐 DivineRPG、Journey、End Expansion 的未列条目，再推进其他模拟器、特殊维度和村庄组件。交错次元的地下遗迹、水下遗迹、地下刷怪笼、地表小遗迹群已按用户要求排除。

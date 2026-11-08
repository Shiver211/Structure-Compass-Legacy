# 当前结构兼容表

条目与 `src/main/resources/compat/structures.json` 对应。

当前 61 项，均使用预测，必要时查询隔离地形。“已实现”表示有定位器，不等于整包验收完成。

下表仅收录已接入的结构。未实现和已停止支持的结构已从游戏目录删除。
用户已测试并反馈两类问题：龙之研究末地维度误判、极光幽境模拟位置偏差。维度判定已修复，四项有偏差的模拟已停止支持；修复包待复测。其他暂未反馈问题。

仅在来源模组已安装时注册条目，不按整合包配置删减目录。已适配条目在生成开关关闭或维度不匹配时仍显示原因。接口绑定失败的条目不会发送到客户端，详情写入加载日志。

预测读取种子、生成参数与必要的临时地形，不读取已有结构位置。Y=64 的候选中心只用于导航参考；未完整复演其他模组的生成器替换。
BOP 7.0.1.2844 patched 的 BIOMESOP 世界接入私人群系图层和原生基础地形，神庙使用 BOP 群系规则，府邸使用其原生地形桥接器。模拟不重新应用共享群系设置，不替换全局图层；完整 BOP 地表与装饰仍未适配，未知群系地表调用继续拦截。
Nether API 1.4.5 已接入独立末地生成器，用于末地城的基础地形与群系判定；模拟构造期间跳过附属结构工厂，不修改全局注册表。完整装饰、下界生成器仍未适配。混沌岛和彗星仅按规则预测，不依赖该地形工厂。
End Expansion 的哀叹群岛提供起始岛预测，待集中实测。
DivineRPG 的 12 项纯种子预测包含两种冰雪地牢和梦魇世界十种大型建筑。跳过同层大型结构共用 populate 窗口的候选，避免生成优先级与覆盖影响；可能略过更近的结构。当前待集中实测。
Unseen’s 的炽焰竞技场、黑曜石竞技场提供起始点预测，待集中实测。

## Minecraft — 1.12.2

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `minecraft:village` | 村庄 / Village | 预测 | 已实现 | — |
| `minecraft:mineshaft` | 废弃矿井 / Mineshaft | 预测 | 已实现 | — |
| `minecraft:stronghold` | 要塞 / Stronghold | 预测 | 已实现 | — |
| `minecraft:ocean_monument` | 海底神殿 / Ocean Monument | 预测 | 已实现 | — |
| `minecraft:desert_pyramid` | 沙漠神殿 / Desert Pyramid | 预测 | 已实现 | — |
| `minecraft:jungle_temple` | 丛林神庙 / Jungle Temple | 预测 | 已实现 | — |
| `minecraft:swamp_hut` | 沼泽小屋 / Swamp Hut | 预测 | 已实现 | — |
| `minecraft:igloo` | 雪屋 / Igloo | 预测 | 已实现 | — |
| `minecraft:woodland_mansion` | 林地府邸 / Woodland Mansion | 预测 | 已实现 | — |
| `minecraft:nether_fortress` | 下界要塞 / Nether Fortress | 预测 | 已实现 | — |
| `minecraft:end_city` | 末地城 / End City | 预测 | 已实现 | — |

## The Aether — 1.5.4.1

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `aether_legacy:silver_dungeon` | 白银地牢 / Silver Dungeon | 预测 | 已实现 | — |
| `aether_legacy:golden_dungeon` | 黄金地牢 / Golden Dungeon | 预测 | 已实现 | — |

## Atum 2 — 2.0.21

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `atum:pyramid` | 金字塔 / Pyramid | 预测 | 已实现 | — |
| `atum:tomb` | 陵墓 / Tomb | 预测 | 已实现 | — |
| `atum:girafi_tomb` | 吉拉菲之墓 / Girafi Tomb | 预测 | 已实现 | — |
| `atum:lighthouse` | 灯塔 / Lighthouse | 预测 | 已实现 | — |
| `atum:mineshaft` | 阿图姆矿井 / Atum Mineshaft | 预测 | 已实现 | — |

## TofuCraft Reload — 0.1.0.9

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `tofucraft:village` | 豆腐村庄 / Tofu Village | 预测 | 已实现 | — |
| `tofucraft:castle` | 豆腐城堡 / Tofu Castle | 预测 | 已实现 | — |
| `tofucraft:mineshaft` | 豆腐矿井 / Tofu Mineshaft | 预测 | 已实现 | — |

## The Twilight Forest — 3.15.1

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `twilightforest:small_hill` | 小型空心山丘 / Small Hollow Hill | 预测 | 已实现 | — |
| `twilightforest:medium_hill` | 中型空心山丘 / Medium Hollow Hill | 预测 | 已实现 | — |
| `twilightforest:large_hill` | 大型空心山丘 / Large Hollow Hill | 预测 | 已实现 | — |
| `twilightforest:hedge_maze` | 树篱迷宫 / Hedge Maze | 预测 | 已实现 | — |
| `twilightforest:naga_courtyard` | 娜迦庭院 / Naga Courtyard | 预测 | 已实现 | — |
| `twilightforest:lich_tower` | 巫妖塔 / Lich Tower | 预测 | 已实现 | — |
| `twilightforest:ice_tower` | 极光城堡 / Aurora Palace | 预测 | 已实现 | — |
| `twilightforest:quest_grove` | 谜题羊树丛 / Quest Grove | 预测 | 已实现 | — |
| `twilightforest:hydra_lair` | 九头蛇巢穴 / Hydra Lair | 预测 | 已实现 | — |
| `twilightforest:labyrinth` | 牛头人迷宫 / Labyrinth | 预测 | 已实现 | — |
| `twilightforest:dark_tower` | 黑暗高塔 / Dark Tower | 预测 | 已实现 | — |
| `twilightforest:knight_stronghold` | 哥布林骑士要塞 / Knight Stronghold | 预测 | 已实现 | — |
| `twilightforest:yeti_cave` | 雪怪洞穴 / Yeti Cave | 预测 | 已实现 | — |
| `twilightforest:troll_cave` | 巨魔洞窟 / Troll Cave | 预测 | 已实现 | — |
| `twilightforest:final_castle` | 终焉城堡 / Final Castle | 预测 | 已实现 | — |
| `twilightforest:mushroom_tower` | 蘑菇城堡 / Mushroom Tower | 预测 | 已实现 | — |

## Draconic Evolution — 2.3.28.193 tweaked-patched

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `draconicevolution:chaos_island` | 混沌岛 / Chaos Island | 预测 | 已实现 | — |
| `draconicevolution:ender_comet` | 末影彗星 / Ender Comet | 预测 | 已实现 | — |

## Thaumic Augmentation — 2.1.14

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `thaumicaugmentation:eldritch_spire` | 邪术尖塔 / Eldritch Spire | 预测 | 已实现 | — |

## The Aurorian — 1.2

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `theaurorian:moon_temple` | 月宫 / Moon Temple | 预测 | 已实现 | — |
| `theaurorian:darkstone_dungeon` | 暗石地牢 / Darkstone Dungeon | 预测 | 已实现 | 水平位置按生成规则定位；Y 为导航参考高度，实际入口高度随地形变化。 |
| `theaurorian:runestone_tower` | 符石地牢 / Runestone Tower | 预测 | 已实现 | 水平位置按生成规则定位；Y 为导航参考高度，实际入口高度随地形变化。 |

## AbyssalCraft — 1.12.2-1.11.3

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `abyssalcraft:abyssal_stronghold` | 深渊要塞 / Abyssal Stronghold | 预测 | 已实现 | 水平位置按生成规则定位；Y 为导航参考高度，实际入口高度随地形变化。 |
| `abyssalcraft:dreadlands_mineshaft` | 恐惧之地矿井 / Dreadlands Mineshaft | 预测 | 已实现 | 水平位置按生成规则定位；Y 为导航参考高度，实际入口高度随地形变化。 |
| `abyssalcraft:jzahar_temple` | 扎哈尔神殿 / J'zahar's Temple | 预测 | 已实现 | 水平位置按生成规则定位；Y 为导航参考高度，实际入口高度随地形变化。 |

## End Expansion — 1.2-fix

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `endexpansion:lamented_islands` | 哀叹群岛 / Lamented Islands | 预测 | 已实现，待集中实测 | — |

## DivineRPG — 1.0.0-patched

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `divinerpg:archer_dungeon` | 冰霜弓箭手地牢 / Frost Archer Dungeon | 预测 | 已实现，待集中实测 | — |
| `divinerpg:rollum_dungeon` | 罗鲁姆地牢 / Rollum Dungeon | 预测 | 已实现，待集中实测 | — |
| `divinerpg:crypt_1` | 梦魇墓穴 I / Vethea Crypt I | 预测 | 已实现，待集中实测 | — |
| `divinerpg:crypt_2` | 梦魇墓穴 II / Vethea Crypt II | 预测 | 已实现，待集中实测 | — |
| `divinerpg:hive` | 蜂巢 / Hive | 预测 | 已实现，待集中实测 | — |
| `divinerpg:temple_1` | 梦魇神殿 I / Vethea Temple I | 预测 | 已实现，待集中实测 | — |
| `divinerpg:temple_2` | 梦魇神殿 II / Vethea Temple II | 预测 | 已实现，待集中实测 | — |
| `divinerpg:quadrotic_post` | 夸德罗哨站 / Quadrotic Post | 预测 | 已实现，待集中实测 | — |
| `divinerpg:karos_madhouse` | 卡洛斯疯人院 / Karos's Madhouse | 预测 | 已实现，待集中实测 | — |
| `divinerpg:evergarden` | 永恒花园 / Evergarden | 预测 | 已实现，待集中实测 | — |
| `divinerpg:raglok_chamber` | 拉格洛克密室 / Raglok's Chamber | 预测 | 已实现，待集中实测 | — |
| `divinerpg:wreck_hall` | 毁灭大厅 / Wreck Hall | 预测 | 已实现，待集中实测 | — |

## Unseen's Dungeon Additions — 1.7.4.1

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `da:nether_arena` | 炽焰竞技场 / Burning Flame Arena | 预测 | 已实现，待集中实测 | — |
| `da:obsidilith_arena` | 黑曜石竞技场 / Obsidilith Arena | 预测 | 已实现，待集中实测 | — |

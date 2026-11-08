# 当前结构兼容表

由 `tools/generate_compat_docs.py` 从 `src/main/resources/compat/structures.json` 生成。

当前 55 项：预测 43 项、实验性模拟 0 项、暂不支持 12 项。

下表是当前实现批次，不是整合包全部结构清单。其余兼容工作见 `IMPLEMENTATION.md`。
用户已测试并反馈两类问题：龙之研究末地维度误判、极光幽境模拟位置偏差。维度判定已修复，四项有偏差的模拟已停止支持；修复包待复测。其他暂未反馈问题。

仅在来源模组已安装时注册条目，不按整合包配置删减目录。生成开关关闭或维度不匹配时，条目仍可见并显示原因。接口绑定失败时显示“暂不支持”，详情写入加载日志。

预测读取种子、生成参数与必要的临时地形，不读取已有结构位置。Y=64 的候选中心只用于导航参考；未完整复演其他模组的生成器替换。
Nether API 的末地地形尚无隔离适配，末地城在该环境显示不可搜索；混沌岛和彗星仅按规则预测，不依赖该地形工厂。

## Minecraft — 1.12.2

| 结构 ID | 名称 | 搜索方式 | 已知限制 |
|---|---|---|---|
| `minecraft:village` | 村庄 / Village | 预测 | — |
| `minecraft:mineshaft` | 废弃矿井 / Mineshaft | 预测 | — |
| `minecraft:stronghold` | 要塞 / Stronghold | 预测 | — |
| `minecraft:ocean_monument` | 海底神殿 / Ocean Monument | 预测 | — |
| `minecraft:desert_pyramid` | 沙漠神殿 / Desert Pyramid | 预测 | — |
| `minecraft:jungle_temple` | 丛林神庙 / Jungle Temple | 预测 | — |
| `minecraft:swamp_hut` | 沼泽小屋 / Swamp Hut | 预测 | — |
| `minecraft:igloo` | 雪屋 / Igloo | 预测 | — |
| `minecraft:woodland_mansion` | 林地府邸 / Woodland Mansion | 预测 | — |
| `minecraft:nether_fortress` | 下界要塞 / Nether Fortress | 预测 | — |
| `minecraft:end_city` | 末地城 / End City | 预测 | — |

## The Aether — 1.5.4.1

| 结构 ID | 名称 | 搜索方式 | 已知限制 |
|---|---|---|---|
| `aether_legacy:silver_dungeon` | 白银地下城 / Silver Dungeon | 预测 | — |
| `aether_legacy:golden_dungeon` | 黄金地下城 / Golden Dungeon | 预测 | — |
| `aether_legacy:bronze_dungeon` | 青铜地下城 / Bronze Dungeon | 暂不支持 | 青铜地下城依赖天境装饰流程及地下空间检查，尚未实现对应模拟适配。 |

## Atum 2 — 2.0.21

| 结构 ID | 名称 | 搜索方式 | 已知限制 |
|---|---|---|---|
| `atum:pyramid` | 金字塔 / Pyramid | 预测 | — |
| `atum:tomb` | 陵墓 / Tomb | 预测 | — |
| `atum:girafi_tomb` | 吉拉菲之墓 / Girafi Tomb | 预测 | — |
| `atum:lighthouse` | 灯塔 / Lighthouse | 预测 | — |
| `atum:mineshaft` | 阿图姆矿井 / Atum Mineshaft | 预测 | — |
| `atum:ruin` | 废墟 / Ruin | 暂不支持 | 废墟依赖邻近金字塔的生成布局排除，尚未完成局部生成顺序适配。 |
| `atum:dungeon` | 阿图姆地牢 / Atum Dungeon | 暂不支持 | 该结构在装饰阶段生成，尚未实现随机状态与前置地形的复演。 |
| `atum:fossil` | 化石 / Fossil | 暂不支持 | 该结构在装饰阶段生成，尚未实现随机状态与前置地形的复演。 |
| `atum:starting_structure` | 初始营地 / Starting Structure | 暂不支持 | 初始结构依赖出生点选择与维度初始化顺序，尚未完成适配。 |

## TofuCraft Reload — 0.1.0.9

| 结构 ID | 名称 | 搜索方式 | 已知限制 |
|---|---|---|---|
| `tofucraft:village` | 豆腐村庄 / Tofu Village | 预测 | — |
| `tofucraft:castle` | 豆腐城堡 / Tofu Castle | 预测 | — |
| `tofucraft:mineshaft` | 豆腐矿井 / Tofu Mineshaft | 预测 | — |
| `tofucraft:tofu_building` | 豆腐建筑 / Tofu Building | 暂不支持 | 该结构在装饰阶段生成，尚未实现随机状态与前置地形的复演。 |

## The Twilight Forest — 3.15.1

| 结构 ID | 名称 | 搜索方式 | 已知限制 |
|---|---|---|---|
| `twilightforest:small_hill` | 小型空心山丘 / Small Hollow Hill | 预测 | — |
| `twilightforest:medium_hill` | 中型空心山丘 / Medium Hollow Hill | 预测 | — |
| `twilightforest:large_hill` | 大型空心山丘 / Large Hollow Hill | 预测 | — |
| `twilightforest:hedge_maze` | 树篱迷宫 / Hedge Maze | 预测 | — |
| `twilightforest:naga_courtyard` | 娜迦庭院 / Naga Courtyard | 预测 | — |
| `twilightforest:lich_tower` | 巫妖塔 / Lich Tower | 预测 | — |
| `twilightforest:ice_tower` | 极光宫殿 / Aurora Palace | 预测 | — |
| `twilightforest:quest_grove` | 探险者树林 / Quest Grove | 预测 | — |
| `twilightforest:hydra_lair` | 九头蛇巢穴 / Hydra Lair | 预测 | — |
| `twilightforest:labyrinth` | 牛头人迷宫 / Labyrinth | 预测 | — |
| `twilightforest:dark_tower` | 黑暗高塔 / Dark Tower | 预测 | — |
| `twilightforest:knight_stronghold` | 哥布林骑士要塞 / Knight Stronghold | 预测 | — |
| `twilightforest:yeti_cave` | 雪怪洞穴 / Yeti Cave | 预测 | — |
| `twilightforest:troll_cave` | 巨魔洞窟 / Troll Cave | 预测 | — |
| `twilightforest:final_castle` | 最终城堡 / Final Castle | 预测 | — |
| `twilightforest:mushroom_tower` | 蘑菇城堡 / Mushroom Tower | 预测 | — |
| `twilightforest:hollow_tree` | 巨型空心树 / Hollow Tree | 暂不支持 | 巨型空心树使用独立生成流程，尚未接入布局和地形验证。 |

## Draconic Evolution — 2.3.28.193 tweaked-patched

| 结构 ID | 名称 | 搜索方式 | 已知限制 |
|---|---|---|---|
| `draconicevolution:chaos_island` | 混沌岛 / Chaos Island | 预测 | — |
| `draconicevolution:ender_comet` | 末影彗星 / Ender Comet | 预测 | — |

## Thaumic Augmentation — 2.1.14

| 结构 ID | 名称 | 搜索方式 | 已知限制 |
|---|---|---|---|
| `thaumicaugmentation:eldritch_spire` | 邪术尖塔 / Eldritch Spire | 预测 | — |
| `thaumicaugmentation:dimensional_fracture` | 维度裂隙 / Dimensional Fracture | 暂不支持 | 裂隙涉及跨维度连接与延迟生成队列，尚未完成状态隔离。 |

## The Aurorian — 1.2

| 结构 ID | 名称 | 搜索方式 | 已知限制 |
|---|---|---|---|
| `theaurorian:moon_temple` | 月亮神殿 / Moon Temple | 预测 | — |
| `theaurorian:darkstone_dungeon` | 暗石地牢 / Darkstone Dungeon | 预测 | 水平位置按生成规则定位；Y 为导航参考高度，实际入口高度随地形变化。 |
| `theaurorian:runestone_tower` | 符石高塔 / Runestone Tower | 预测 | 水平位置按生成规则定位；Y 为导航参考高度，实际入口高度随地形变化。 |
| `theaurorian:graveyard` | 墓地 / Graveyard | 暂不支持 | 生成概率使用非世界种子随机源，无法复现实际坐标；模拟存在位置偏差，已停止支持搜索。 |
| `theaurorian:umbra_tower` | 暗影高塔 / Umbra Tower | 暂不支持 | 生成概率使用非世界种子随机源，无法复现实际坐标；模拟存在位置偏差，已停止支持搜索。 |
| `theaurorian:surface_ruin` | 地表遗迹 / Surface Ruin | 暂不支持 | 生成概率使用非世界种子随机源，无法复现实际坐标；模拟存在位置偏差，已停止支持搜索。 |
| `theaurorian:underground_ruin` | 地下遗迹 / Underground Ruin | 暂不支持 | 生成概率使用非世界种子随机源，无法复现实际坐标；模拟存在位置偏差，已停止支持搜索。 |

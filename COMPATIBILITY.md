# 当前结构兼容表

由 `tools/generate_compat_docs.py` 从 `src/main/resources/compat/structures.json` 生成。

当前 129 项：预测 61 项、实验性模拟 0 项、运行时不可用 68 项。
不可用条目中：待适配 53 项、本期不支持 15 项。“已实现”表示有定位器，不等于整包验收完成。

下表是当前实现批次，不是整合包全部结构清单。其余兼容工作见 `IMPLEMENTATION.md`。
用户已测试并反馈两类问题：龙之研究末地维度误判、极光幽境模拟位置偏差。维度判定已修复，四项有偏差的模拟已停止支持；修复包待复测。其他暂未反馈问题。

仅在来源模组已安装时注册条目，不按整合包配置删减目录。没有定位器的条目默认隐藏，勾选“显示不支持”后可查看具体原因。已适配条目在生成开关关闭或维度不匹配时仍显示原因。接口绑定失败的条目也归入默认隐藏范围，详情写入加载日志。

预测读取种子、生成参数与必要的临时地形，不读取已有结构位置。Y=64 的候选中心只用于导航参考；未完整复演其他模组的生成器替换。
Nether API 1.4.5 已接入独立末地生成器，用于末地城的基础地形与群系判定；模拟构造期间跳过附属结构工厂，不修改全局注册表。完整装饰、下界生成器仍未适配。混沌岛和彗星仅按规则预测，不依赖该地形工厂。
本批核对 End Expansion 的八项主要结构及 Raids 前哨站。哀叹群岛提供起始岛预测，待集中实测。其他条目的待办或不支持原因见表格；End Expansion 的小型装饰遗迹仍待逐项审计。
冒险模组优先批次新增 DivineRPG 的 12 项纯种子预测，包含两种冰雪地牢和梦魇世界十种大型建筑。跳过同层大型结构共用 populate 窗口的候选，避免生成优先级与覆盖影响；可能略过更近的结构。当前待集中实测，其他房屋与 Journey 条目另列待办或具体不支持原因。
交错次元批次提供浮空岛和巨型树根拱预测，其他 14 类结构保留待适配原因。按用户要求不纳入地下遗迹、水下遗迹、地下刷怪笼和地表小遗迹群。新预测只使用独立群系图层，不创建区块；群系规则与真实图层不一致时停止搜索。

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
| `aether_legacy:silver_dungeon` | 白银地下城 / Silver Dungeon | 预测 | 已实现 | — |
| `aether_legacy:golden_dungeon` | 黄金地下城 / Golden Dungeon | 预测 | 已实现 | — |
| `aether_legacy:bronze_dungeon` | 青铜地下城 / Bronze Dungeon | — | 待适配 | 青铜地下城依赖天境装饰流程及地下空间检查，尚未实现对应模拟适配。 |

## Atum 2 — 2.0.21

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `atum:pyramid` | 金字塔 / Pyramid | 预测 | 已实现 | — |
| `atum:tomb` | 陵墓 / Tomb | 预测 | 已实现 | — |
| `atum:girafi_tomb` | 吉拉菲之墓 / Girafi Tomb | 预测 | 已实现 | — |
| `atum:lighthouse` | 灯塔 / Lighthouse | 预测 | 已实现 | — |
| `atum:mineshaft` | 阿图姆矿井 / Atum Mineshaft | 预测 | 已实现 | — |
| `atum:ruin` | 废墟 / Ruin | — | 待适配 | 废墟依赖邻近金字塔的生成布局排除，尚未完成局部生成顺序适配。 |
| `atum:dungeon` | 阿图姆地牢 / Atum Dungeon | — | 待适配 | 该结构在装饰阶段生成，尚未实现随机状态与前置地形的复演。 |
| `atum:fossil` | 化石 / Fossil | — | 待适配 | 该结构在装饰阶段生成，尚未实现随机状态与前置地形的复演。 |
| `atum:starting_structure` | 初始营地 / Starting Structure | — | 待适配 | 初始结构依赖出生点选择与维度初始化顺序，尚未完成适配。 |

## TofuCraft Reload — 0.1.0.9

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `tofucraft:village` | 豆腐村庄 / Tofu Village | 预测 | 已实现 | — |
| `tofucraft:castle` | 豆腐城堡 / Tofu Castle | 预测 | 已实现 | — |
| `tofucraft:mineshaft` | 豆腐矿井 / Tofu Mineshaft | 预测 | 已实现 | — |
| `tofucraft:tofu_building` | 豆腐建筑 / Tofu Building | — | 待适配 | 该结构在装饰阶段生成，尚未实现随机状态与前置地形的复演。 |

## The Twilight Forest — 3.15.1

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `twilightforest:small_hill` | 小型空心山丘 / Small Hollow Hill | 预测 | 已实现 | — |
| `twilightforest:medium_hill` | 中型空心山丘 / Medium Hollow Hill | 预测 | 已实现 | — |
| `twilightforest:large_hill` | 大型空心山丘 / Large Hollow Hill | 预测 | 已实现 | — |
| `twilightforest:hedge_maze` | 树篱迷宫 / Hedge Maze | 预测 | 已实现 | — |
| `twilightforest:naga_courtyard` | 娜迦庭院 / Naga Courtyard | 预测 | 已实现 | — |
| `twilightforest:lich_tower` | 巫妖塔 / Lich Tower | 预测 | 已实现 | — |
| `twilightforest:ice_tower` | 极光宫殿 / Aurora Palace | 预测 | 已实现 | — |
| `twilightforest:quest_grove` | 探险者树林 / Quest Grove | 预测 | 已实现 | — |
| `twilightforest:hydra_lair` | 九头蛇巢穴 / Hydra Lair | 预测 | 已实现 | — |
| `twilightforest:labyrinth` | 牛头人迷宫 / Labyrinth | 预测 | 已实现 | — |
| `twilightforest:dark_tower` | 黑暗高塔 / Dark Tower | 预测 | 已实现 | — |
| `twilightforest:knight_stronghold` | 哥布林骑士要塞 / Knight Stronghold | 预测 | 已实现 | — |
| `twilightforest:yeti_cave` | 雪怪洞穴 / Yeti Cave | 预测 | 已实现 | — |
| `twilightforest:troll_cave` | 巨魔洞窟 / Troll Cave | 预测 | 已实现 | — |
| `twilightforest:final_castle` | 最终城堡 / Final Castle | 预测 | 已实现 | — |
| `twilightforest:mushroom_tower` | 蘑菇城堡 / Mushroom Tower | 预测 | 已实现 | — |
| `twilightforest:hollow_tree` | 巨型空心树 / Hollow Tree | — | 待适配 | 巨型空心树使用独立生成流程，尚未接入布局和地形验证。 |

## Draconic Evolution — 2.3.28.193 tweaked-patched

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `draconicevolution:chaos_island` | 混沌岛 / Chaos Island | 预测 | 已实现 | — |
| `draconicevolution:ender_comet` | 末影彗星 / Ender Comet | 预测 | 已实现 | — |

## Thaumic Augmentation — 2.1.14

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `thaumicaugmentation:eldritch_spire` | 邪术尖塔 / Eldritch Spire | 预测 | 已实现 | — |
| `thaumicaugmentation:dimensional_fracture` | 维度裂隙 / Dimensional Fracture | — | 待适配 | 裂隙涉及跨维度连接与延迟生成队列，尚未完成状态隔离。 |

## The Aurorian — 1.2

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `theaurorian:moon_temple` | 月亮神殿 / Moon Temple | 预测 | 已实现 | — |
| `theaurorian:darkstone_dungeon` | 暗石地牢 / Darkstone Dungeon | 预测 | 已实现 | 水平位置按生成规则定位；Y 为导航参考高度，实际入口高度随地形变化。 |
| `theaurorian:runestone_tower` | 符石高塔 / Runestone Tower | 预测 | 已实现 | 水平位置按生成规则定位；Y 为导航参考高度，实际入口高度随地形变化。 |
| `theaurorian:graveyard` | 墓地 / Graveyard | 暂不支持 | 本期不支持 | 生成概率使用非世界种子随机源，无法复现实际坐标；模拟存在位置偏差，已停止支持搜索。 |
| `theaurorian:umbra_tower` | 暗影高塔 / Umbra Tower | 暂不支持 | 本期不支持 | 生成概率使用非世界种子随机源，无法复现实际坐标；模拟存在位置偏差，已停止支持搜索。 |
| `theaurorian:surface_ruin` | 地表遗迹 / Surface Ruin | 暂不支持 | 本期不支持 | 生成概率使用非世界种子随机源，无法复现实际坐标；模拟存在位置偏差，已停止支持搜索。 |
| `theaurorian:underground_ruin` | 地下遗迹 / Underground Ruin | 暂不支持 | 本期不支持 | 生成概率使用非世界种子随机源，无法复现实际坐标；模拟存在位置偏差，已停止支持搜索。 |

## AbyssalCraft — 1.12.2-1.11.3

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `abyssalcraft:abyssal_stronghold` | 深渊要塞 / Abyssal Stronghold | 预测 | 已实现 | 水平位置按生成规则定位；Y 为导航参考高度，实际入口高度随地形变化。 |
| `abyssalcraft:dreadlands_mineshaft` | 恐惧之地矿井 / Dreadlands Mineshaft | 预测 | 已实现 | 水平位置按生成规则定位；Y 为导航参考高度，实际入口高度随地形变化。 |
| `abyssalcraft:jzahar_temple` | 扎哈尔神殿 / J'zahar's Temple | 预测 | 已实现 | 水平位置按生成规则定位；Y 为导航参考高度，实际入口高度随地形变化。 |
| `abyssalcraft:darklands_house_1` | 黑暗之地房屋 I / Darklands House I | — | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:darklands_house_2` | 黑暗之地房屋 II / Darklands House II | — | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:darklands_scion_1` | 黑暗之地残迹 I / Darklands Scion Ruin I | — | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:darklands_scion_2` | 黑暗之地残迹 II / Darklands Scion Ruin II | — | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:circular_shrine` | 环形神龛 / Circular Shrine | — | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:circular_shrine_columns` | 柱式环形神龛 / Circular Shrine with Columns | — | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:ritual_grounds` | 仪式场地 / Ritual Grounds | — | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:ritual_grounds_columns` | 柱式仪式场地 / Ritual Grounds with Columns | — | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:elevated_shrine` | 高台神龛 / Elevated Shrine | — | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:elevated_shrine_large` | 大型高台神龛 / Large Elevated Shrine | — | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:abyssal_pillars` | 深渊荒原支柱 / Abyssal Wasteland Pillars | — | 待适配 | 生成依赖湖泊、要塞等前置流程及共享装饰随机源；尚未完成隔离复演，无法可靠定位。 |
| `abyssalcraft:abyssal_ruin` | 深渊荒原废墟 / Abyssal Wasteland Ruin | — | 待适配 | 生成依赖湖泊、要塞等前置流程及共享装饰随机源；尚未完成隔离复演，无法可靠定位。 |
| `abyssalcraft:shoggoth_lair` | 修格斯巢穴 / Shoggoth Lair | — | 待适配 | 各维度使用不同的自然生成入口，依赖矿物、湖泊或遗迹的随机消耗及跨区块地基检查；尚未适配。 |
| `abyssalcraft:omothol_city` | 奥穆索城市 / Omothol City | 暂不支持 | 本期不支持 | 生成器用内存中的已生成位置集合排除邻近建筑，且随机状态受区块生成顺序影响；不读取这些记录时无法可靠复现坐标。 |
| `abyssalcraft:omothol_temple` | 奥穆索散布神殿 / Omothol Temple | 暂不支持 | 本期不支持 | 生成器用内存中的已生成位置集合排除邻近建筑，且随机状态受区块生成顺序影响；不读取这些记录时无法可靠复现坐标。 |
| `abyssalcraft:omothol_tower` | 奥穆索高塔 / Omothol Tower | 暂不支持 | 本期不支持 | 生成器用内存中的已生成位置集合排除邻近建筑，且随机状态受区块生成顺序影响；不读取这些记录时无法可靠复现坐标。 |
| `abyssalcraft:omothol_storage` | 奥穆索仓库 / Omothol Storage | 暂不支持 | 本期不支持 | 生成器用内存中的已生成位置集合排除邻近建筑，且随机状态受区块生成顺序影响；不读取这些记录时无法可靠复现坐标。 |

## End Expansion — 1.2-fix

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `endexpansion:lamented_islands` | 哀叹群岛 / Lamented Islands | 预测 | 已实现，待集中实测 | 导航到起始岛的模板起点（Y=90），群岛布局由模组生成。当前版本待集中实测。 |
| `endexpansion:ash_tower` | 灰烬高塔 / Ash Tower | 暂不支持 | 本期不支持 | 位置取决于生成器累计调用次数和区块生成顺序，同一种子无法独立复现；本期不支持。 |
| `endexpansion:ashed_mines` | 灰烬矿井 / Ashed Mines | 暂不支持 | 本期不支持 | 位置取决于生成器累计调用次数和区块生成顺序，同一种子无法独立复现；本期不支持。 |
| `endexpansion:end_vault` | 末地地牢 / End Vault | 暂不支持 | 本期不支持 | 位置取决于生成器累计调用次数和区块生成顺序，同一种子无法独立复现；本期不支持。 |
| `endexpansion:barrend_crypt` | 荒芜墓穴 / Barrend Crypt | 暂不支持 | 本期不支持 | 位置取决于生成器累计调用次数和区块生成顺序，同一种子无法独立复现；本期不支持。 |
| `endexpansion:trader_outcamp` | 阿瓦隆商人营地 / Avalon Trader Outcamp | 暂不支持 | 本期不支持 | 位置取决于生成器累计调用次数和区块生成顺序，同一种子无法独立复现；本期不支持。 |
| `endexpansion:king_fortress` | 末地国王要塞 / End King Fortress | — | 待适配 | 网格候选可计算，但布局使用全局随机源并以组件数量判定成功；完整成功条件仍待适配。 |
| `endexpansion:better_stronghold` | 强化要塞 / Better Stronghold | — | 待适配 | 替换原版要塞生成器，独立起点与布局校验尚未适配。 |

## Raids — 1.1.5

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `raids:pillager_outpost` | 掠夺者前哨站 / Pillager Outpost | 暂不支持 | 本期不支持 | 放置条件查询最近的未探索村庄，取决于真实探索及结构状态；本期不读取这些记录，因此不支持可靠定位。 |

## DivineRPG — 1.0.0-patched

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `divinerpg:archer_dungeon` | 冰霜弓箭手地牢 / Frost Archer Dungeon | 预测 | 已实现，待集中实测 | 预测首个模板组件的位置、高度与旋转；跳过与同层大型结构共用生成区域的候选，可能略过更近的结构。待集中实测。 |
| `divinerpg:rollum_dungeon` | 罗鲁姆地牢 / Rollum Dungeon | 预测 | 已实现，待集中实测 | 预测首个模板组件的位置、高度与旋转；跳过与同层大型结构共用生成区域的候选，可能略过更近的结构。待集中实测。 |
| `divinerpg:crypt_1` | 梦魇墓穴 I / Vethea Crypt I | 预测 | 已实现，待集中实测 | 预测首个模板组件的位置、高度与旋转；跳过与同层大型结构共用生成区域的候选，可能略过更近的结构。待集中实测。 |
| `divinerpg:crypt_2` | 梦魇墓穴 II / Vethea Crypt II | 预测 | 已实现，待集中实测 | 预测首个模板组件的位置、高度与旋转；跳过与同层大型结构共用生成区域的候选，可能略过更近的结构。待集中实测。 |
| `divinerpg:hive` | 蜂巢 / Hive | 预测 | 已实现，待集中实测 | 预测首个模板组件的位置、高度与旋转；跳过与同层大型结构共用生成区域的候选，可能略过更近的结构。待集中实测。 |
| `divinerpg:temple_1` | 梦魇神殿 I / Vethea Temple I | 预测 | 已实现，待集中实测 | 预测首个模板组件的位置、高度与旋转；跳过与同层大型结构共用生成区域的候选，可能略过更近的结构。待集中实测。 |
| `divinerpg:temple_2` | 梦魇神殿 II / Vethea Temple II | 预测 | 已实现，待集中实测 | 预测首个模板组件的位置、高度与旋转；跳过与同层大型结构共用生成区域的候选，可能略过更近的结构。待集中实测。 |
| `divinerpg:quadrotic_post` | 夸德罗哨站 / Quadrotic Post | 预测 | 已实现，待集中实测 | 预测首个模板组件的位置、高度与旋转；跳过与同层大型结构共用生成区域的候选，可能略过更近的结构。待集中实测。 |
| `divinerpg:karos_madhouse` | 卡洛斯疯人院 / Karos's Madhouse | 预测 | 已实现，待集中实测 | 预测首个模板组件的位置、高度与旋转；跳过与同层大型结构共用生成区域的候选，可能略过更近的结构。待集中实测。 |
| `divinerpg:evergarden` | 永恒花园 / Evergarden | 预测 | 已实现，待集中实测 | 预测首个模板组件的位置、高度与旋转；跳过与同层大型结构共用生成区域的候选，可能略过更近的结构。待集中实测。 |
| `divinerpg:raglok_chamber` | 拉格洛克密室 / Raglok's Chamber | 预测 | 已实现，待集中实测 | 预测首个模板组件的位置、高度与旋转；跳过与同层大型结构共用生成区域的候选，可能略过更近的结构。待集中实测。 |
| `divinerpg:wreck_hall` | 毁灭大厅 / Wreck Hall | 预测 | 已实现，待集中实测 | 预测首个模板组件的位置、高度与旋转；跳过与同层大型结构共用生成区域的候选，可能略过更近的结构。待集中实测。 |
| `divinerpg:livestock_merchant_hut` | 牲畜商人小屋 / Livestock Merchant Hut | — | 待适配 | 自然概率可复现，但草地、群系与四角地基检查仍待适配。 |
| `divinerpg:workshop_house_1` | 工坊房屋 1 / Workshop House 1 | — | 待适配 | 房屋选择在地牢放置之后进行，依赖其随机消耗和地面检查；尚未完成复演。 |
| `divinerpg:workshop_house_2` | 工坊房屋 2 / Workshop House 2 | — | 待适配 | 房屋选择在地牢放置之后进行，依赖其随机消耗和地面检查；尚未完成复演。 |
| `divinerpg:workshop_house_3` | 工坊房屋 3 / Workshop House 3 | — | 待适配 | 房屋选择在地牢放置之后进行，依赖其随机消耗和地面检查；尚未完成复演。 |
| `divinerpg:workshop_house_4` | 工坊房屋 4 / Workshop House 4 | — | 待适配 | 房屋选择在地牢放置之后进行，依赖其随机消耗和地面检查；尚未完成复演。 |
| `divinerpg:workshop_house_5` | 工坊房屋 5 / Workshop House 5 | — | 待适配 | 房屋选择在地牢放置之后进行，依赖其随机消耗和地面检查；尚未完成复演。 |
| `divinerpg:workshop_house_6` | 工坊房屋 6 / Workshop House 6 | — | 待适配 | 房屋选择在地牢放置之后进行，依赖其随机消耗和地面检查；尚未完成复演。 |
| `divinerpg:hunger_house_1` | 饥饿者小屋 1 / Hunger House 1 | — | 待适配 | 候选位置可计算，三个模板的选择、旋转偏移及与同层建筑的覆盖检查仍待适配。 |
| `divinerpg:hunger_house_2` | 饥饿者小屋 2 / Hunger House 2 | — | 待适配 | 候选位置可计算，三个模板的选择、旋转偏移及与同层建筑的覆盖检查仍待适配。 |
| `divinerpg:hunger_house_3` | 饥饿者小屋 3 / Hunger House 3 | — | 待适配 | 候选位置可计算，三个模板的选择、旋转偏移及与同层建筑的覆盖检查仍待适配。 |

## Journey Into the Light — 1.0.5

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `journey:corba_village` | 科尔巴村庄 / Corba Village | 暂不支持 | 本期不支持 | 当前分支仅在区块重载时补建村庄起点，新区块生成未调用起点生成器；实际放置受加载顺序影响，不能只用网格预测。 |
| `journey:tower_dungeon` | 高塔地牢 / Tower Dungeon | — | 待适配 | 结构概率与其他装饰共享随机源，并依赖地面或空间检查；尚未完成必要的前置复演。 |
| `journey:cylindrical_tower` | 圆柱高塔地牢 / Cylindrical Tower Dungeon | — | 待适配 | 结构概率与其他装饰共享随机源，并依赖地面或空间检查；尚未完成必要的前置复演。 |
| `journey:rockite_dungeon` | 岩石地牢 / Rockite Dungeon | — | 待适配 | 结构概率与其他装饰共享随机源，并依赖地面或空间检查；尚未完成必要的前置复演。 |
| `journey:overworld_ruins` | 主世界遗迹 / Overworld Ruins | — | 待适配 | 结构概率与其他装饰共享随机源，并依赖地面或空间检查；尚未完成必要的前置复演。 |
| `journey:corba_treehouse` | 科尔巴树屋 / Corba Treehouse | — | 待适配 | 结构概率与其他装饰共享随机源，并依赖地面或空间检查；尚未完成必要的前置复演。 |
| `journey:tordo_village` | 托尔多村庄 / Tordo Village | — | 待适配 | 结构概率与其他装饰共享随机源，并依赖地面或空间检查；尚未完成必要的前置复演。 |

## The Betweenlands / 交错次元 — 3.9.6

| 结构 ID | 名称 | 搜索方式 | 开发状态 | 已知限制 |
|---|---|---|---|---|
| `thebetweenlands:floating_island` | 浮空岛 / Floating Island | 预测 | 已实现，待集中实测 | 按自然种子规则定位浮空岛中心；Y 为生成中心高度，可能位于岛体或池水中。待集中实测。 |
| `thebetweenlands:giant_root` | 巨型树根拱 / Giant Root Arch | 预测 | 已实现，待集中实测 | 定位巨型树根拱的根部起点（Y=114，通常在水下）；每个候选区块取首条自然生成的根。待集中实测。 |
| `thebetweenlands:spirit_tree` | 灵魂树 / Spirit Tree | — | 待适配 | 待适配：空地群系仅能缩小范围；仍需复演前置装饰、候选乱序、空间检查及邻近生成影响。 |
| `thebetweenlands:sludge_worm_dungeon` | 污泥地牢（泥沼谜塔） / Sludge Worm Dungeon | — | 待适配 | 待适配：需复演空地候选与自然生成顺序，并隔离区域重叠排除、共享战利品和实体状态。 |
| `thebetweenlands:cragrock_tower` | 碎岩塔 / Cragrock Tower | — | 待适配 | 待适配：需独立复演前置装饰随机顺序、地形及自然放置检查，并隔离交错次元区域存储。 |
| `thebetweenlands:wight_fortress` | 幽魂要塞（鬼魂城堡） / Wight Fortress | — | 待适配 | 待适配：需独立复演前置装饰随机顺序、地形及自然放置检查，并隔离交错次元区域存储。 |
| `thebetweenlands:chiromaw_matriarch_nest` | 翼手女王巢穴 / Chiromaw Matriarch Nest | — | 待适配 | 待适配：需独立复演前置装饰随机顺序、地形及自然放置检查，并隔离交错次元区域存储。 |
| `thebetweenlands:sunken_idol_head` | 沉没神像（峭壁岩头颅） / Sunken Idol Head | — | 待适配 | 待适配：需独立复演前置装饰随机顺序、地形及自然放置检查，并隔离交错次元区域存储。 |
| `thebetweenlands:abandoned_shack` | 泥砖废弃小屋 / Abandoned Shack | — | 待适配 | 待适配：三种布局共用装饰随机源，需复演布局选择、地形与放置检查，并隔离区域存储。 |
| `thebetweenlands:spawner_structure` | 刷怪笼建筑（小型神庙） / Spawner Structure | — | 待适配 | 待适配：需独立复演前置装饰随机顺序、地形及自然放置检查，并隔离交错次元区域存储。 |
| `thebetweenlands:tar_pool_dungeon` | 焦油池地牢 / Tar Pool Dungeon | — | 待适配 | 待适配：需独立复演前置装饰随机顺序、地形及自然放置检查，并隔离交错次元区域存储。 |
| `thebetweenlands:waystone` | 巨石柱 / Waystone | — | 待适配 | 待适配：网格候选可预测，但实际放置需要装饰后的地面、液体和空间检查。 |
| `thebetweenlands:druid_circle` | 德鲁伊祭坛 / Druid Circle | — | 待适配 | 待适配：生成于主世界；先寻找合格沼泽地面，再抽取自然概率，还需复演区域加载与空间条件。 |
| `thebetweenlands:deepman_simulacrum` | 洞穴人面雕像 / Deepman Simulacrum | — | 待适配 | 待适配：需复演前置装饰随机顺序、地面与空间条件，并隔离雕像方块实体状态。 |
| `thebetweenlands:lake_cavern_simulacrum` | 洞穴湖泊雕像 / Lake Cavern Simulacrum | — | 待适配 | 待适配：需复演前置装饰随机顺序、地面与空间条件，并隔离雕像方块实体状态。 |
| `thebetweenlands:rootman_simulacrum` | 树根人面雕像 / Rootman Simulacrum | — | 待适配 | 待适配：需复演前置装饰随机顺序、地面与空间条件，并隔离雕像方块实体状态。 |

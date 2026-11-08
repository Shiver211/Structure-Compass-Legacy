# 未进入指南针目录的结构记录

这份开发记录不随模组发布，不参与结构注册。目录清理时保留 69 项待适配、21 项本期不支持的已知原因；后续确认可用后再加入运行目录。

Erebus 和交错次元已按用户要求完全移除，不属于本记录或当前适配范围。

## The Aether — 1.5.4.1

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `aether_legacy:bronze_dungeon` | 青铜地下城 / Bronze Dungeon | 本期不支持 | 青铜地下城的非种子随机房间会改变邻近地下城的生成条件，区块加载范围也影响生成成败；当前无法可靠模拟，暂不支持。 |

## Atum 2 — 2.0.21

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `atum:ruin` | 废墟 / Ruin | 待适配 | 废墟依赖邻近金字塔的生成布局排除，尚未完成局部生成顺序适配。 |
| `atum:dungeon` | 阿图姆地牢 / Atum Dungeon | 待适配 | 该结构在装饰阶段生成，尚未实现随机状态与前置地形的复演。 |
| `atum:fossil` | 化石 / Fossil | 待适配 | 该结构在装饰阶段生成，尚未实现随机状态与前置地形的复演。 |
| `atum:starting_structure` | 初始营地 / Starting Structure | 待适配 | 初始结构依赖出生点选择与维度初始化顺序，尚未完成适配。 |

## TofuCraft Reload — 0.1.0.9

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `tofucraft:tofu_building` | 豆腐建筑 / Tofu Building | 待适配 | 该结构在装饰阶段生成，尚未实现随机状态与前置地形的复演。 |

## The Twilight Forest — 3.15.1

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `twilightforest:hollow_tree` | 巨型空心树 / Hollow Tree | 待适配 | 巨型空心树使用独立生成流程，尚未接入布局和地形验证。 |

## Thaumic Augmentation — 2.1.14

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `thaumicaugmentation:dimensional_fracture` | 维度裂隙 / Dimensional Fracture | 待适配 | 裂隙涉及跨维度连接与延迟生成队列，尚未完成状态隔离。 |

## The Aurorian — 1.2

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `theaurorian:graveyard` | 墓地 / Graveyard | 本期不支持 | 生成概率使用非世界种子随机源，无法复现实际坐标；模拟存在位置偏差，已停止支持搜索。 |
| `theaurorian:umbra_tower` | 暗影高塔 / Umbra Tower | 本期不支持 | 生成概率使用非世界种子随机源，无法复现实际坐标；模拟存在位置偏差，已停止支持搜索。 |
| `theaurorian:surface_ruin` | 地表遗迹 / Surface Ruin | 本期不支持 | 生成概率使用非世界种子随机源，无法复现实际坐标；模拟存在位置偏差，已停止支持搜索。 |
| `theaurorian:underground_ruin` | 地下遗迹 / Underground Ruin | 本期不支持 | 生成概率使用非世界种子随机源，无法复现实际坐标；模拟存在位置偏差，已停止支持搜索。 |

## AbyssalCraft — 1.12.2-1.11.3

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `abyssalcraft:darklands_house_1` | 黑暗之地房屋 I / Darklands House I | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:darklands_house_2` | 黑暗之地房屋 II / Darklands House II | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:darklands_scion_1` | 黑暗之地残迹 I / Darklands Scion Ruin I | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:darklands_scion_2` | 黑暗之地残迹 II / Darklands Scion Ruin II | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:circular_shrine` | 环形神龛 / Circular Shrine | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:circular_shrine_columns` | 柱式环形神龛 / Circular Shrine with Columns | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:ritual_grounds` | 仪式场地 / Ritual Grounds | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:ritual_grounds_columns` | 柱式仪式场地 / Ritual Grounds with Columns | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:elevated_shrine` | 高台神龛 / Elevated Shrine | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:elevated_shrine_large` | 大型高台神龛 / Large Elevated Shrine | 待适配 | 结构选择与概率检查穿插地基验证及其他遗迹生成；尚未复演前置地形和随机消耗，无法可靠定位。 |
| `abyssalcraft:abyssal_pillars` | 深渊荒原支柱 / Abyssal Wasteland Pillars | 待适配 | 生成依赖湖泊、要塞等前置流程及共享装饰随机源；尚未完成隔离复演，无法可靠定位。 |
| `abyssalcraft:abyssal_ruin` | 深渊荒原废墟 / Abyssal Wasteland Ruin | 待适配 | 生成依赖湖泊、要塞等前置流程及共享装饰随机源；尚未完成隔离复演，无法可靠定位。 |
| `abyssalcraft:shoggoth_lair` | 修格斯巢穴 / Shoggoth Lair | 待适配 | 各维度使用不同的自然生成入口，依赖矿物、湖泊或遗迹的随机消耗及跨区块地基检查；尚未适配。 |
| `abyssalcraft:omothol_city` | 奥穆索城市 / Omothol City | 本期不支持 | 生成器用内存中的已生成位置集合排除邻近建筑，且随机状态受区块生成顺序影响；不读取这些记录时无法可靠复现坐标。 |
| `abyssalcraft:omothol_temple` | 奥穆索散布神殿 / Omothol Temple | 本期不支持 | 生成器用内存中的已生成位置集合排除邻近建筑，且随机状态受区块生成顺序影响；不读取这些记录时无法可靠复现坐标。 |
| `abyssalcraft:omothol_tower` | 奥穆索高塔 / Omothol Tower | 本期不支持 | 生成器用内存中的已生成位置集合排除邻近建筑，且随机状态受区块生成顺序影响；不读取这些记录时无法可靠复现坐标。 |
| `abyssalcraft:omothol_storage` | 奥穆索仓库 / Omothol Storage | 本期不支持 | 生成器用内存中的已生成位置集合排除邻近建筑，且随机状态受区块生成顺序影响；不读取这些记录时无法可靠复现坐标。 |

## End Expansion — 1.2-fix

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `endexpansion:ash_tower` | 灰烬高塔 / Ash Tower | 本期不支持 | 位置取决于生成器累计调用次数和区块生成顺序，同一种子无法独立复现；本期不支持。 |
| `endexpansion:ashed_mines` | 灰烬矿井 / Ashed Mines | 本期不支持 | 位置取决于生成器累计调用次数和区块生成顺序，同一种子无法独立复现；本期不支持。 |
| `endexpansion:end_vault` | 末地地牢 / End Vault | 本期不支持 | 位置取决于生成器累计调用次数和区块生成顺序，同一种子无法独立复现；本期不支持。 |
| `endexpansion:barrend_crypt` | 荒芜墓穴 / Barrend Crypt | 本期不支持 | 位置取决于生成器累计调用次数和区块生成顺序，同一种子无法独立复现；本期不支持。 |
| `endexpansion:trader_outcamp` | 阿瓦隆商人营地 / Avalon Trader Outcamp | 本期不支持 | 位置取决于生成器累计调用次数和区块生成顺序，同一种子无法独立复现；本期不支持。 |
| `endexpansion:king_fortress` | 末地国王要塞 / End King Fortress | 待适配 | 网格候选可计算，但布局使用全局随机源并以组件数量判定成功；完整成功条件仍待适配。 |
| `endexpansion:better_stronghold` | 强化要塞 / Better Stronghold | 待适配 | 替换原版要塞生成器，独立起点与布局校验尚未适配。 |

## Raids — 1.1.5

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `raids:pillager_outpost` | 掠夺者前哨站 / Pillager Outpost | 本期不支持 | 放置条件查询最近的未探索村庄，取决于真实探索及结构状态；本期不读取这些记录，因此不支持可靠定位。 |

## DivineRPG — 1.0.0-patched

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `divinerpg:livestock_merchant_hut` | 牲畜商人小屋 / Livestock Merchant Hut | 待适配 | 自然概率可复现，但草地、群系与四角地基检查仍待适配。 |
| `divinerpg:workshop_house_1` | 工坊房屋 1 / Workshop House 1 | 待适配 | 房屋选择在地牢放置之后进行，依赖其随机消耗和地面检查；尚未完成复演。 |
| `divinerpg:workshop_house_2` | 工坊房屋 2 / Workshop House 2 | 待适配 | 房屋选择在地牢放置之后进行，依赖其随机消耗和地面检查；尚未完成复演。 |
| `divinerpg:workshop_house_3` | 工坊房屋 3 / Workshop House 3 | 待适配 | 房屋选择在地牢放置之后进行，依赖其随机消耗和地面检查；尚未完成复演。 |
| `divinerpg:workshop_house_4` | 工坊房屋 4 / Workshop House 4 | 待适配 | 房屋选择在地牢放置之后进行，依赖其随机消耗和地面检查；尚未完成复演。 |
| `divinerpg:workshop_house_5` | 工坊房屋 5 / Workshop House 5 | 待适配 | 房屋选择在地牢放置之后进行，依赖其随机消耗和地面检查；尚未完成复演。 |
| `divinerpg:workshop_house_6` | 工坊房屋 6 / Workshop House 6 | 待适配 | 房屋选择在地牢放置之后进行，依赖其随机消耗和地面检查；尚未完成复演。 |
| `divinerpg:hunger_house_1` | 饥饿者小屋 1 / Hunger House 1 | 待适配 | 候选位置可计算，三个模板的选择、旋转偏移及与同层建筑的覆盖检查仍待适配。 |
| `divinerpg:hunger_house_2` | 饥饿者小屋 2 / Hunger House 2 | 待适配 | 候选位置可计算，三个模板的选择、旋转偏移及与同层建筑的覆盖检查仍待适配。 |
| `divinerpg:hunger_house_3` | 饥饿者小屋 3 / Hunger House 3 | 待适配 | 候选位置可计算，三个模板的选择、旋转偏移及与同层建筑的覆盖检查仍待适配。 |

## Journey Into the Light — 1.0.5

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `journey:corba_village` | 科尔巴村庄 / Corba Village | 本期不支持 | 当前分支仅在区块重载时补建村庄起点，新区块生成未调用起点生成器；实际放置受加载顺序影响，不能只用网格预测。 |
| `journey:tower_dungeon` | 高塔地牢 / Tower Dungeon | 待适配 | 结构概率与其他装饰共享随机源，并依赖地面或空间检查；尚未完成必要的前置复演。 |
| `journey:cylindrical_tower` | 圆柱高塔地牢 / Cylindrical Tower Dungeon | 待适配 | 结构概率与其他装饰共享随机源，并依赖地面或空间检查；尚未完成必要的前置复演。 |
| `journey:rockite_dungeon` | 岩石地牢 / Rockite Dungeon | 待适配 | 结构概率与其他装饰共享随机源，并依赖地面或空间检查；尚未完成必要的前置复演。 |
| `journey:overworld_ruins` | 主世界遗迹 / Overworld Ruins | 待适配 | 结构概率与其他装饰共享随机源，并依赖地面或空间检查；尚未完成必要的前置复演。 |
| `journey:corba_treehouse` | 科尔巴树屋 / Corba Treehouse | 待适配 | 结构概率与其他装饰共享随机源，并依赖地面或空间检查；尚未完成必要的前置复演。 |
| `journey:tordo_village` | 托尔多村庄 / Tordo Village | 待适配 | 结构概率与其他装饰共享随机源，并依赖地面或空间检查；尚未完成必要的前置复演。 |

## Ice and Fire: RotN Edition — 1.9.1-1.3.3

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `iceandfire:fire_dragon_roost` | 火龙巢 / Fire Dragon Roost | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:ice_dragon_roost` | 冰龙巢 / Ice Dragon Roost | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:lightning_dragon_roost` | 电龙巢 / Lightning Dragon Roost | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:fire_dragon_cave` | 火龙穴 / Fire Dragon Cave | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:ice_dragon_cave` | 冰龙穴 / Ice Dragon Cave | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:lightning_dragon_cave` | 电龙穴 / Lightning Dragon Cave | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:gorgon_temple` | 蛇发女妖神庙 / Gorgon Temple | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:siren_island` | 塞壬岛 / Siren Island | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:cyclops_cave` | 独眼巨人洞穴 / Cyclops Cave | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:pixie_village` | 精灵村 / Pixie Village | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:snow_village` | 雪地村 / Snow Village | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:mausoleum` | 陵墓 / Mausoleum | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:hydra_cave` | 九头蛇洞穴 / Hydra Cave | 待适配 | 待适配：RotN 分支的 last* 位置参与自然间距判定，并改变后续随机消耗；禁用间距后的受限路径及局部生成仍待验证。 |
| `iceandfire:desert_myrmex_hive` | 沙漠蚁巢 / Desert Myrmex Hive | 待适配 | 待适配：蚁巢同时依赖 lastMyrmexHive 与 MyrmexWorldData 中其他蚁巢的 500 格排斥；不能用空记录替代。 |
| `iceandfire:jungle_myrmex_hive` | 丛林蚁巢 / Jungle Myrmex Hive | 待适配 | 待适配：蚁巢同时依赖 lastMyrmexHive 与 MyrmexWorldData 中其他蚁巢的 500 格排斥；不能用空记录替代。 |
| `iceandfire:animal_farm` | 村庄动物农场 / Village Animal Farm | 待适配 | 待适配：属于村庄布局组件，需要验证该建筑实际入选与放置，不能返回任意村庄。 |

## Netherized — 0.3.8B

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `netherized:bastion_remnant` | 堡垒遗迹 / Bastion Remnant | 本期不支持 | 堡垒间距使用共享生成器的调用次数计数，并在生成后清零，不是由种子和区块坐标决定的网格。 |
| `netherized:nether_fossil` | 下界化石（14 种模板） / Nether Fossil (14 templates) | 待适配 | 待适配：原生区块生成器在 populate 时不重置随机源；Nether API 环境中的前置装饰与化石自然入口也需单独复演。 |

## Lost Aether Content — 1.0.2

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `lost_aether:platinum_dungeon` | 铂金地下城 / Platinum Dungeon | 待适配 | 待适配：保留 Forge 1/2000 概率，并检查模板偏移后 Y=120 起点为空气；前置天境云、地下城与装饰尚未完整复演，不能仅凭裸地形启用。 |

## Unseen's Dungeon Additions — 1.7.4.1

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `da:blossom_cave` | 虚空之花洞穴 / Void Blossom Cave | 本期不支持 | 起始建筑高度使用无种子 Random 或 ModRand，固定世界种子与区块仍会改变坐标；不返回有已知高度偏差的预测。 |
| `da:high_city` | 高庭之城 / High Court City | 本期不支持 | 起始建筑高度使用无种子 Random 或 ModRand，固定世界种子与区块仍会改变坐标；不返回有已知高度偏差的预测。 |
| `da:rot_hold` | 腐败要塞 / Rotten Hold | 待适配 | 待适配：已有网格仅给出候选；地形高度、空间判定和原生布局成败尚需隔离验证。 |
| `da:lich_tower` | 夜巫妖高塔 / Night Lich Tower | 待适配 | 待适配：已有网格仅给出候选；地形高度、空间判定和原生布局成败尚需隔离验证。 |
| `da:frozen_castle` | 冰封城堡 / Frozen Castle | 待适配 | 待适配：已有网格仅给出候选；地形高度、空间判定和原生布局成败尚需隔离验证。 |
| `da:forgotten_temple` | 遗忘神庙 / Forgotten Temple | 待适配 | 待适配：已有网格仅给出候选；地形高度、空间判定和原生布局成败尚需隔离验证。 |
| `da:gaelon_sanctuary` | 盖伦圣所 / Gaelon Sanctuary | 待适配 | 待适配：已有网格仅给出候选；地形高度、空间判定和原生布局成败尚需隔离验证。 |
| `da:mysterious_trader` | 神秘商人据点 / Mysterious Trader Post | 待适配 | 待适配：已有网格仅给出候选；地形高度、空间判定和原生布局成败尚需隔离验证。 |
| `da:dauntless_arena` | 无畏竞技场 / Dauntless Arena | 待适配 | 待适配：已有网格仅给出候选；地形高度、空间判定和原生布局成败尚需隔离验证。 |
| `da:outpost` | 前哨站／飞空船 / Outpost / Airship | 待适配 | 待适配：地面检查失败可能改为随机高度的飞空建筑；需要区分并验证可复现的地面路径。安装版自然入口只在末地调用。 |

## Astral Sorcery — 1.10.27

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `astralsorcery:ancient_shrine` | 大型古代神庙 / Ancient Shrine | 待适配 | 待适配：启用理想间距时概率依赖已有神庙记录；关闭该规则时仍需复演独立随机源、前置装饰与本结构的地基／空间条件。 |
| `astralsorcery:desert_shrine` | 沙漠神庙 / Desert Shrine | 待适配 | 待适配：启用理想间距时概率依赖已有神庙记录；关闭该规则时仍需复演独立随机源、前置装饰与本结构的地基／空间条件。 |
| `astralsorcery:small_shrine` | 小型神庙 / Small Shrine | 待适配 | 待适配：启用理想间距时概率依赖已有神庙记录；关闭该规则时仍需复演独立随机源、前置装饰与本结构的地基／空间条件。 |
| `astralsorcery:treasure_shrine` | 宝藏神庙 / Treasure Shrine | 待适配 | 待适配：启用理想间距时概率依赖已有神庙记录；关闭该规则时仍需复演独立随机源、前置装饰与本结构的地基／空间条件。 |
| `astralsorcery:small_ruin` | 小型遗迹 / Small Ruin | 待适配 | 待适配：启用理想间距时概率依赖已有神庙记录；关闭该规则时仍需复演独立随机源、前置装饰与本结构的地基／空间条件。 |

## Battle Towers / Battle Towers Fixes — 1.12.2 / 1.2.2

| 结构 ID | 名称 | 开发状态 | 原因 |
|---|---|---|---|
| `AS_BattleTowers:surface_tower` | 地上战斗高塔（含材质变体） / Surface Battle Tower (material variants) | 本期不支持 | 自然间距依赖已生成高塔位置，地上／地下选择使用 world.rand；BTFixes 另按服务器启动时间跳过生成。空记录与固定时间不能可靠复演该流程。 |
| `AS_BattleTowers:underground_tower` | 地下战斗高塔（含材质变体） / Underground Battle Tower (material variants) | 本期不支持 | 自然间距依赖已生成高塔位置，地上／地下选择使用 world.rand；BTFixes 另按服务器启动时间跳过生成。空记录与固定时间不能可靠复演该流程。 |

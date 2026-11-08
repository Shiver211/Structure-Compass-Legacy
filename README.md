# Structure Compass Legacy

Minecraft 1.12.2 / Cleanroom 的结构指南针。当前版本为 `0.1.0`，正在分批实现整合包模组兼容。

右键打开列表，按中文、英文或模组名搜索。找到目标后，指针和 HUD 提供方向、水平距离与坐标。
只搜索当前维度，使用生成规则预测，必要时验证临时地形。当前没有启用模拟搜索条目。
无法可靠复现坐标的结构保留“不支持”条目和具体原因，不再提供仅概率相似的模拟结果。
不读取已有结构坐标，不记录探索位置，不保存搜索历史。物品 NBT 仅保存当前导航目标。

```text
石砖  铁锭   石砖
铁锭  指南针 铁锭
石砖  铁锭   石砖
```

安装 `build/libs/structurecompasslegacy-0.1.0.jar`，客户端和服务端使用同一版本。
不要安装 `-dev.jar` 或 `-sources.jar`。首次运行自动生成 `config/structurecompasslegacy.cfg`。

- [逐结构兼容表](COMPATIBILITY.md)：当前 75 个条目及具体限制。
- [本批检查与测试项](reports/compat-batch-2026-11-08.md)：Nether API 末地城和深渊国度适配。
- [实现说明](IMPLEMENTATION.md)：模块入口、隔离边界和剩余兼容工作。

## 构建

沿用 Cleanroom TemplateDevEnv：Java 25 运行 Gradle 9.7.0 / RFG 2.0.3，输出 Java 8 字节码。
目标验收环境为 Cleanroom `0.6.13-alpha`。

```powershell
.\gradlew.bat build
python tools/generate_compat_docs.py --check
```

可选模组通过注册时绑定的公开接口接入，默认构建无需它们的 JAR。
需要直接引用模组 API 时，可使用本地 `compileOnly` 依赖：

```powershell
.\gradlew.bat build '-PcompatModsDir=D:/path/to/mods' '-PcompatModJars=Atum-1.12.2-2.0.21.jar'
```

只核查已安装分支的字节码时，无需添加编译依赖：

```powershell
.\gradlew.bat test '-PcompatModsDir=D:/path/to/mods'
```

指定目录后会核查 Nether API 1.4.5 与 AbyssalCraft 1.11.3；普通构建跳过这两项版本核查。

第三方模组不打包进指南针。个人目录不写入构建配置。

候选计算与局部验证思路参考 [Simple Structure Scanner](https://github.com/Aedial/Simple-Structure-Scanner)。
目前没有复制该项目的源代码。开发模板来自 [CleanroomMC/TemplateDevEnv](https://github.com/CleanroomMC/TemplateDevEnv)，保留其 MIT 许可。

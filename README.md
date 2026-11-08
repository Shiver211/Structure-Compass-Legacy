# Structure Compass Legacy

Minecraft 1.12.2 / Cleanroom 的结构指南针。当前版本为 `0.1.0`。受控环境的底层验证已完成，正在逐结构推进模组兼容。

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
- [工作进度](PROGRESS.md)：底层里程碑、完成证据与下一步。
- [底层验证](reports/foundation-2026-11-08.md)：自动检查结果与待做的游戏验收。
- [实现说明](IMPLEMENTATION.md)：模块入口、隔离边界和剩余兼容工作。

## 构建

沿用 Cleanroom TemplateDevEnv：Java 25 运行 Gradle 9.7.0 / RFG 2.0.3，输出 Java 8 字节码。
目标验收环境为 Cleanroom `0.6.13-alpha`。

```powershell
.\gradlew.bat build
.\gradlew.bat verifyFoundationRuntime
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

指定目录后会核查 Nether API 1.4.5、AbyssalCraft 1.11.3 及 RC / IvToolkit 验证入口的 API；普通构建跳过这些版本核查。

底层回归检查位于 `src/test`，正式 JAR 不包含验证命令和诊断审计。
`verifyFoundationRuntime` 在 `build/foundation-runtime` 中创建独立测试世界：
检查真实 Mixin 与模拟隔离，再用正常 `WorldServer` 生成、保存并重新读取刷怪笼目标。
它验证受控原版自然流程；RC 的完整前置生成、目标整包、多种子和多人验收仍属后续工作。
HMCL 已通过的 9 项基础检查及失败坐标保留在[底层验证记录](reports/foundation-2026-11-08.md)，不算作整包定位通过。

第三方模组不打包进指南针。个人目录不写入构建配置。

候选计算与局部验证思路参考 [Simple Structure Scanner](https://github.com/Aedial/Simple-Structure-Scanner)。
目前没有复制该项目的源代码。开发模板来自 [CleanroomMC/TemplateDevEnv](https://github.com/CleanroomMC/TemplateDevEnv)，保留其 MIT 许可。

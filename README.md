# AetherTV · 以太桌面

面向家庭影音的 Android TV 桌面，基于 [pengchujin/aerial-tv](https://github.com/pengchujin/aerial-tv) 开发，保留 MIT 许可与上游版权。支持 Android 5.0+，优先适配当贝 F3。

**中文** · [English](README.en.md)

## 0.3.1 测试版

- 首页应用网格、文件夹、壁纸、屏保和手机扫码管理沿用 Aerial 的基础能力。
- AetherTV / 以太桌面品牌与五语言界面。
- Auto / Full / Lite 渲染模式，手机控制台“更多”可切换；后台释放可重建图片缓存。
- Lite静态保留缓存玻璃，航拍使用透明底栏和方向光；视频缓冲、图片及屏外磁贴绘制缓存收紧。
- 手机配置 M3U 与 XMLTV/XMLTV.gz；前台直播支持 HLS/TS、频道表、EPG 与失败切源，网络失效可读取缓存。
- 桌面/菜单分别设置液态或磨砂玻璃，聚焦时 Dock 伸展；支持 4–8 列、应用菜单名称及独立系统控件。
- 多人和 Agent 按工作包推进，构建固定 Java 17，双 flavor 保留包名和清单约束。

前台直播与桌面定制已做 Redmi K40S 手机验证；首页静音预览、NAS 海报墙和文件挂载见后续 [Roadmap](ROADMAP.md)。验收范围见 [0.3.0 工作包](docs/work/AERIAL-20261027-liquid-controls.md)，电视专属能力仍需目标机验证。

0.3.1 在 Redmi K40S / Android 14 的 Lite 对照中，静态、航拍、菜单 PSS 中位数约为 211、251、239 MiB，相比0.3.0约降低17%、15%、22%。这是单轮手机样本，不能替代F3结果；条件与限制见[性能工作包](docs/work/AERIAL-20261027-lite-performance.md)。

## 安装与开发

请使用 [0.3.1 预发布包](https://github.com/blackstone2333/aethertv/releases/tag/v0.3.1) 和 [安装与升级指南](docs/INSTALL.md)，也可从源码构建：

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleStandardRelease assembleGuardRelease
```

APK 位于 `app/build/outputs/apk/<flavor>/release/`。应用只从 [AetherTV Releases](https://github.com/blackstone2333/aethertv/releases) 获取后续版本；不要将上游 Aerial 的 APK 当作本定制版更新。

包名固定 `com.dangbei.tvlauncher`，源码 namespace 固定 `app.orchard.tv`。当贝原厂桌面 `com.dangbei.leard.leradlauncher` 必须保持启用。手机验证不能替代 F3 的 HOME、黑屏、遥控器、HDMI、待机和硬解码验收；这些设备行为仍为未验证。

## 协作入口

先读 [AGENTS.md](AGENTS.md)、[项目地图](docs/PROJECT.md) 和 [工作规则](docs/AGENT_WORKFLOW.md)，再读取目标 [工作包](docs/work/) 与 [交接](docs/handoffs/)。工作单元沿用 `AERIAL-YYYYMMDD-<slug>` 命名。

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./tools/check-project-rules.sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testStandardReleaseUnitTest testGuardReleaseUnitTest
```

真实源地址、设备信息、令牌、账号和签名密钥只保留本机，不提交 `local/`。具体架构见 [ARCHITECTURE.md](docs/ARCHITECTURE.md)；其中历史设备记录来自上游，不能作为本定制版的实机结论。

[问题反馈](https://github.com/blackstone2333/aethertv/issues) · [MIT License](LICENSE)

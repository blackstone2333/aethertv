# AERIAL-20261026-roadmap-phase2

Status: complete
Owner: main model / integration owner

## Spec

- 目标用户/系统：当贝 F3（Android 6/API 23）上的 AetherTV 家庭影音桌面。
- 必须可观察的结果：项目有可执行的 Goldilocks 协作规则；第一版有稳定的品牌、中文化、包名和 Lite 架构基线；IPTV 已有独立配置/解析/缓存边界，NAS/打包矩阵有独立验收入口。
- 范围：协作规则初始化、品牌/Lite 基线、IPTV 第一版契约和手机配置入口、模块接口与风险约束。
- 非目标：本单元不直接接入真实 IPTV/NAS，不重写首页，不停用原厂桌面，不更换 `applicationId` 或 namespace，不宣称未上真机功能完成。
- 约束：`com.dangbei.tvlauncher`、`app.orchard.tv`、Java 17、minSdk 21/Android 5/6 兼容、五语言资源、保留当贝原厂桌面和现有无障碍服务。
- 验收条件：规则预检可运行；工作包/交接模板可被后续 Agent 使用；Plan 明确 Phase 2→IPTV→NAS→矩阵的依赖和最小验证；当前源码未提交改动不被覆盖。
- 测试策略：普通逻辑和手机 Web 先用 L0/L1 验证；电视接管、MStar 黑屏、遥控器、HDMI、待机复活和硬解码保留 L3 当贝 F3 验收。

## Plan

1. 建立项目地图、Agent 工作规则、工作包和交接模板 — integration owner — `tools/check-project-rules.sh`、文档链接和 Git diff。
2. 收敛 Phase 2 品牌基线：品牌 SVG 导出、应用名/关于页五语言、包名和版本元数据 — phase2-brand owner — Java 17 双 flavor release、单测、aapt2 清单检查；F3 真机单列未验证/已验证。
3. 提取 Lite 渲染策略和内存回收接口，不重写首页 — lite owner — Compose 单元/基准 probe、`onTrimMemory` 行为证据、双 flavor release。
4. 定义并实现 IPTV 契约：M3U/XMLTV 解析、缓存、源优先级、Media3 播放会话 — iptv owner — parser/failover 单测、离线缓存、全屏释放；预览最后接入且 F3 未验证前标记未验证。
5. 定义 NAS/MoonTV/Selene/OrionTV `MediaCardRepository` 与首页 Dock — nas owner — 超时/离线降级、可禁用 provider、无网络时首屏可用。
6. 建立 flavor 矩阵：当贝先行，其他品牌只保留能力检测和独立覆盖 — packaging owner — 每个 flavor 的 APK 元数据和清单检查。
7. 集成验收并更新当前工作包；真实所有权转移时创建 handoff — integration owner — 一次 combined acceptance，未验证设备行为保持明确。

## Architecture decisions

- IPTV 不进入 `SystemProbe` 的实体电视输入模型；使用 `live/model|repo|player|ui|config` 独立模块。
- 首页只消费独立的只读 `LiveState`/`MediaCard`，网络加载失败不得阻塞 `HomeViewModel.reload()`。
- Media3 预览、全屏和屏保播放器分开管理；焦点停留后延迟启动预览，离焦、返回和 Home 立即释放 Surface/播放器。
- NAS 凭据不硬编码；手机 Web 配置沿用 NanoHTTPD token，敏感信息使用受保护存储或最小必要凭据。
- Android 5/6 保持当前 Compose 1.9/Media3 1.8.1 兼容线；局域网明文只做精确配置，不全局开放 cleartext。

## Status

- 已完成：读取外部当贝交接、AetherTV 规则和 Roadmap；完成品牌中文化、Lite 渲染/内存回收、IPTV 解析/缓存/手机配置、双 flavor 构建和 APK 静态检查。
- 当前动作：第一版进入交接，等待设备证据和下一轮 NAS/播放器需求。
- 阻塞：Redmi K40S 已配对但 MIUI 拒绝 ADB 安装，需要设备端打开“通过 ADB 安装/安装 via USB”；没有新的当贝 F3 真机证据。
- 精确下一步：完成手机端安装授权后安装 standard release，保存一个源并验证刷新/离线回退；随后再拆 Media3 全屏和 NAS Dock。
- 不重复：不重复已通过的上游兼容修复和无关设备矩阵；不把上游 v1.49 整体 merge 到当前工作树。

## Evidence

- `ROADMAP.md`、`AGENTS.md`、`PROJECT.md`、`BRANDING.md`、`docs/HANDOFF.md`、`docs/ARCHITECTURE.md` 已读取。
- Hermes 外部交接 `codex_aerial_handover.md` 已读取，原文件不进入公开仓库。
- 当前基线：`main` 已提交并同步公开仓库；实现提交为 `3a811f4`，直播契约测试提交为 `dfc6758`，工作树清洁。
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./tools/check-project-rules.sh`：通过。
- `ANDROID_HOME=/Users/black/Library/Android/sdk ANDROID_SDK_ROOT=/Users/black/Library/Android/sdk JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testStandardReleaseUnitTest testGuardReleaseUnitTest assembleStandardRelease assembleGuardRelease --no-daemon`：通过（测试和双 Release 分开执行，均成功）。
- `aapt2 dump badging/xmltree`：标准版和守护版均为 `com.dangbei.tvlauncher`，版本 `101 / 1.0.1-dangbei`，标准版包含 `CATEGORY_HOME`，配置 action 为 `com.dangbei.tvlauncher.CONFIG`；守护版未包含 HOME 过滤器。
- `node --check /tmp/aethertv-web.js`、`git diff --check`：通过。
- 真机：Redmi K40S 无线调试配对成功，Android 14 / `munch`；standard APK 被 MIUI 以 `INSTALL_FAILED_USER_RESTRICTED` 拒绝，未启动；当贝 F3 专属接管/黑屏/遥控器/HDMI/硬解码均未验证。

## Deferred

- 真实局域网源地址、EPG 数据和 NAS 凭据配置。
- 当贝 F3 上的直播预览、全屏播放、黑屏回归和多任务中心实测。
- 其他品牌 flavor 的设备专属行为，直到当贝 flavor 稳定并有独立设备证据。

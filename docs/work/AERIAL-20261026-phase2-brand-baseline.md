# AERIAL-20261026-phase2-brand-baseline

Status: complete
Owner: main model / integration owner

## Spec

- 目标：把 `AetherTV / 以太桌面` 品牌接入 Android 资源和关于页，并保持 `com.dangbei.tvlauncher`、`app.orchard.tv` 不变。
- 范围：`app/src/main/res/`、品牌 SVG 导出、应用名/关于页、五语言资源、版本显示；允许读取现有首页但不重写首页状态流。
- 非目标：不改包名、不替换签名、不接入 IPTV/NAS、不停用当贝原厂桌面。
- 验收：五套字符串键一致；品牌图标由 SVG 导出；Java 17 下双 flavor release；APK 元数据正确；真机结果单列。

## Plan

1. 建立资源键和五语言文案。
2. 从 `design/brand/` 与 `assets/branding/` 生成 Android 图标/横幅资源。
3. 更新关于页和可见应用名，保留上游行为。
4. 运行规则预检、受影响单测、双 flavor release 和 APK 检查。

## Status

- 已完成：Android 应用名、守护/更新/屏保入口和 Web 核心文案切换为 AetherTV/以太桌面；五语言资源键保持一致；手机反馈链接指向公开仓库；包名和版本元数据保持白名单基线。
- 当前动作：发布验收完成。
- 阻塞：无；视觉体系重做仍延期到 Phase 4。
- Exact next action：后续发布时补充品牌资源的视觉回归截图。

## Evidence

- `app/src/main/res/values*/strings.xml`、`app/src/main/assets/web/index.html`、`docs/brand/`。
- `assembleStandardRelease assembleGuardRelease` 和 `aapt2` 包名/版本检查：通过。
- 真机：Redmi K40S 已配对但 APK 安装被 MIUI 拒绝；当贝 F3 品牌显示未验证。

## Deferred

- 视觉体系重做和 AI 生图资产，放到 Phase 4。

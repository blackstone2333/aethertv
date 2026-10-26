# AERIAL-20261026-phase2-brand-baseline

Status: complete
Owner: main model / integration owner

## Spec

- 目标：把 `AetherTV / 以太桌面` 品牌接入 Android 资源和关于页，并保持 `com.dangbei.tvlauncher`、`app.orchard.tv` 不变。
- 范围：`app/src/main/res/`、应用名/关于页、五语言资源、版本显示和本仓库更新入口；允许读取现有首页但不重写首页状态流。
- 非目标：不改包名、不替换签名、不接入 IPTV/NAS、不停用当贝原厂桌面。
- 验收：五套字符串键一致；保留现有图标资产，视觉重做延期；Java 17 下双 flavor release；APK 元数据正确；真机结果单列。

## Plan

1. 建立资源键和五语言文案。
2. 保留图标资源基线；专属 SVG 图标/横幅重做并入 Phase 4（deferred）。
3. 更新关于页和可见应用名，保留上游行为。
4. 运行规则预检、受影响单测、双 flavor release 和 APK 检查。

## Status

- 已完成：AetherTV/以太桌面可见文案基线，控制中心显示“以太桌面 1.0.1”；手机反馈与更新/安装入口指向本仓库；五语言文案同步，包名不变。
- 视觉体系重做仍留在 Phase 4；本包完成范围为显示名、可见品牌/版本和资源基线。

## Evidence

- 双 flavor Release、APK 包名/版本检查、Web 五语言检查通过。
- Redmi K40S L1：控制中心品牌和版本显示、手机页标题已验证；F3 品牌显示未验证。详见 [手机验收](AERIAL-20261026-phone-acceptance.md)。

## Exact next action

根据试用反馈调整视觉；正式发行前验收同签名 APK 更新。

## Deferred

- 视觉体系重做和 AI 生图资产，放到 Phase 4。

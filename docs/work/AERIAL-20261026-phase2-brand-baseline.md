# AERIAL-20261026-phase2-brand-baseline

Status: pending
Owner: phase2-brand owner

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

- 已完成：工作范围和约束定义。
- 当前动作：等待 Owner 接管。
- 阻塞：未决定最终版本号和品牌资源导出尺寸。
- Exact next action：先检查现有 strings 键和 `aapt2` 可用性，再提交资源变更提案。

## Evidence

- 事实依据：`BRANDING.md`、`PROJECT.md`、`ROADMAP.md`。
- 真机：未验证。

## Deferred

- 视觉体系重做和 AI 生图资产，放到 Phase 4。

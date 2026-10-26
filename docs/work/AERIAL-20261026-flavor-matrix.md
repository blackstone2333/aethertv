# AERIAL-20261026-flavor-matrix

Status: complete
Owner: main model / integration owner

## Spec

- 目标：在当贝 flavor 稳定后建立多机型打包矩阵，隔离包名、清单、权限、设备资源和安装脚本。
- 范围：Gradle flavor/build profile、manifest/resource overlay、APK 元数据检查和安装脚本；当贝先行。
- 非目标：不复制共享业务代码，不改变当贝白名单包名，不在无设备证据时宣称小米/TCL 已兼容。
- 验收：每个 flavor 的 applicationId、namespace、HOME/guard 过滤器、版本和签名配置可独立检查；共享代码仍用 Java 17 构建。

## Plan

1. 记录当贝 flavor 的固定契约和元数据检查。
2. 抽取设备能力检测接口，禁止机型字符串散落业务层。
3. 增加第二个 flavor 的最小 overlay 和安装/报告入口。
4. 每个 flavor 只在对应设备有 release 证据后开放。

## Status

- 已完成：保留当贝白名单 `applicationId`、`app.orchard.tv` namespace、standard/guard 两个 flavor；标准版含 HOME，守护版不含 HOME；双 Release APK 的版本、包名和 CONFIG action 已静态核验。
- 当前动作：发布验收完成。
- 阻塞：其他品牌设备能力差异仍需独立设备资料。
- Exact next action：新增品牌 flavor 前先建独立工作包和 APK 元数据检查。

## Evidence

- `app/build.gradle.kts`、`app/src/standard/AndroidManifest.xml`、外部当贝交接；`aapt2 dump badging/xmltree`：双 APK 通过。
- 真机：Redmi K40S 已连接但 MIUI 拒绝 APK 安装；当贝 F3 未完成安装证据，设备专属行为未验证。

## Deferred

- 小米/TCL/极米/创维的品牌专属行为和签名分发。

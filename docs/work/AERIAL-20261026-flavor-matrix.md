# AERIAL-20261026-flavor-matrix

Status: pending
Owner: packaging owner

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

- 已完成：矩阵边界和当贝优先顺序定义。
- 当前动作：等待 Owner 接管。
- 阻塞：暂无第二品牌的授权设备和白名单资料。
- Exact next action：先为当贝 flavor 写 APK metadata probe，不增加新品牌代码。

## Evidence

- 事实依据：`app/build.gradle.kts`、`app/src/standard/AndroidManifest.xml`、外部当贝交接。
- 真机：除已有交接记录外，AetherTV 当前工作树未新增 F3 证据。

## Deferred

- 小米/TCL/极米/创维的品牌专属行为和签名分发。

# AERIAL-20261026-flavor-matrix

Status: complete
Owner: main model / integration owner

## Spec

- 目标：完成当贝包名与 standard/guard 元数据基线，为后续多机型打包矩阵保留边界。
- 范围：Gradle flavor/build profile、manifest/resource overlay、APK 元数据检查和安装脚本；当贝先行。
- 非目标：不复制共享业务代码，不改变当贝白名单包名，不在无设备证据时宣称小米/TCL 已兼容。
- 验收：每个 flavor 的 applicationId、namespace、HOME/guard 过滤器、版本和签名配置可独立检查；共享代码仍用 Java 17 构建。

## Plan

1. 记录当贝 flavor 的固定契约和元数据检查。
2. 新品牌能力检测抽取 — deferred，待有目标设备。
3. 新增品牌 overlay 与设备入口 — deferred；当前保留 standard/guard 两个 HOME flavor。
4. 当前 standard 只有手机 L1；电视设备结论继续 deferred。

## Status

- v0.1 已完成：当贝白名单包名、namespace、standard/guard Release 的 HOME 和 CONFIG 契约及静态核验。
- 多品牌矩阵、自动发行和设备专属适配留待后续；当前完整安装/还原脚本仍需电视实测。

## Evidence

- `aapt2`：双 APK 包名和版本正确，standard 含 HOME，guard 不含 HOME。
- Redmi K40S Android 14 standard Release 安装与启动通过；F3 专属行为未验证。详见 [手机验收](AERIAL-20261026-phone-acceptance.md)。

## Exact next action

新增品牌前建立独立工作包；F3 稳定后再开放其他品牌。

## Deferred

- 小米/TCL/极米/创维的品牌专属行为和签名分发。

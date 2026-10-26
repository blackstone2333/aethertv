# AERIAL-20261026-lite-mode

Status: complete
Owner: main model / integration owner

## Spec

- 目标：为低内存 Android 5/6 电视提供 Full/Lite 渲染开关和内存回收，不改变 HomeGuard 或包名机制。
- 范围：`ui/theme`、`ui/home`、`apps` 缓存和 `MainActivity` 生命周期；Lite 关闭高负载 blur/shadow/parallax，`onTrimMemory` 释放可重建图片缓存。
- 非目标：不一次性重写首页，不引入要求 API 23 以上的新依赖，不在后台持续动画。
- 验收：同一布局可切换两种策略；缓存释放可观测且重新进入可恢复；Java 17 双 flavor release；代表性模拟器/真机结果分别标记。

## Plan

1. 定义 `RenderMode` 和设置来源，默认 Auto，按设备能力选择。
2. 将 LiquidGlass、TileArt 阴影/视差和壁纸缓存改为策略读取。
3. 在 Activity/Application 生命周期接入 trim 回收并记录诊断。
4. 接手机配置页并完成 L0/L1；性能量化与 F3 L3 留待后续。

## Status

- 已完成：Auto/Full/Lite 持久化和手机可见开关，Lite 跳过玻璃与磁贴高负载效果；Home 隐藏时释放图片，回前台先读本地烘焙图；系统屏保持有图片期间保留共享缓存。
- Redmi K40S L1：三种策略状态、Lite 设置保留、后台释放日志与回前台相同画面均通过；系统屏保/F3 仍未验证。

## Evidence

- Java 17 双 Release、预检通过，详见 [手机验收](AERIAL-20261026-phone-acceptance.md)。
- 手机未测统一冷启动基准；没有将 0.4 秒或帧率目标写成已达成。

## Exact next action

收集视觉反馈；在 F3 记录低内存、屏保与播放共存时的内存和恢复行为。

## Deferred

- 0.4 秒冷启动目标，除非有统一基准设备和测量脚本，否则不作为完成条件。

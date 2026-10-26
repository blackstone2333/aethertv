# AERIAL-20261026-lite-mode

Status: pending
Owner: lite owner

## Spec

- 目标：为低内存 Android 5/6 电视提供 Full/Lite 渲染开关和内存回收，不改变 HomeGuard 或包名机制。
- 范围：`ui/theme`、`ui/home`、`apps` 缓存和 `MainActivity` 生命周期；Lite 关闭高负载 blur/shadow/parallax，`onTrimMemory` 释放可重建图片缓存。
- 非目标：不一次性重写首页，不引入要求 API 23 以上的新依赖，不在后台持续动画。
- 验收：同一布局可切换两种策略；缓存释放可观测且重新进入可恢复；Java 17 双 flavor release；代表性模拟器/真机结果分别标记。

## Plan

1. 定义 `RenderMode` 和设置来源，默认沿用 Full。
2. 将 LiquidGlass、TileArt 阴影/视差和壁纸缓存改为策略读取。
3. 在 Activity/Application 生命周期接入 trim 回收并记录诊断。
4. 用受影响单测和性能 probe 验收，最后再接手机配置页。

## Status

- 已完成：范围和 Android 5/6 约束定义。
- 当前动作：等待 Owner 接管。
- 阻塞：暂无；真实性能数字须设备证据。
- Exact next action：盘点现有可回收缓存和 `LiquidGlass` 调用点，提出最小接口。

## Evidence

- 事实依据：`docs/ARCHITECTURE.md`、`app/src/main/java/app/orchard/tv/ui/components/LiquidGlass.kt`。
- 真机：未验证。

## Deferred

- 0.4 秒冷启动目标，除非有统一基准设备和测量脚本，否则不作为完成条件。

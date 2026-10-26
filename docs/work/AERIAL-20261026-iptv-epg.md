# AERIAL-20261026-iptv-epg

Status: pending
Owner: iptv owner

## Spec

- 目标：把 M3U/EPG、本地/公网/IPv6 回退和 Media3 直播播放做成独立模块，供首页 Dock 和全屏入口消费。
- 范围：新增 `live/model|repo|player|ui|config`；扩展手机 Web API；只读接入现有 `HomeViewModel`。
- 非目标：不把网络频道混入 `SystemProbe`/实体 HDMI 输入，不硬编码 NAS 凭据，不先做首页实时预览。
- 约束：Media3 1.8.1 和 Android 5/6；局域网明文精确放行；焦点/离焦必须创建和释放播放器；单路硬解码器互斥。
- 验收：解析器/回退纯 Kotlin 单测；离线缓存可浏览；全屏播放和返回释放；真实源连通性和 F3 预览单列为设备证据。

## Plan

1. 先定义 `Channel`、`EpgProgram`、`LiveSourceConfig`、`LivePlaybackState`。
2. 实现 M3U/XMLTV.gz 解析、大小限制、缓存和优先级回退。
3. 实现全屏播放器和焦点导航，最后实现延迟预览。
4. 加入手机端 `api/live/*` 配置、测试和脱敏状态。

## Status

- 已完成：模块边界和播放器互斥约束定义。
- 当前动作：等待 Owner 接管。
- 阻塞：真实源地址和 EPG 质量需运行时确认。
- Exact next action：先写 parser fixture 和 failover contract，不接 Home UI。

## Evidence

- 事实依据：`app/src/main/java/app/orchard/tv/wallpaper/AerialStream.kt`、`PhoneServer.kt`、`docs/ARCHITECTURE.md`。
- 真机：未验证。

## Deferred

- 真实源默认值、自动发现和首页画中画，待配置/全屏稳定后。

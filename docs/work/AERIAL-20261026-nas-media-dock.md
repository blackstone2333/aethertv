# AERIAL-20261026-nas-media-dock

Status: pending
Owner: nas owner

## Spec

- 目标：以统一媒体卡片接口接入 MoonTV/Selene/OrionTV 推荐、历史和最近入库提示，并保持网络失败时首页可用。
- 范围：`media/model|repo`、首页 Dock 适配和可配置 HTTP provider；WebDAV/SMB/NFS 先做独立媒体库边界。
- 非目标：不在本单元实现文件系统挂载内核、不把账号密码写入源码、不阻塞 `HomeViewModel.reload()`。
- 验收：provider 可独立启停；超时/离线有缓存或空态；统一卡片可映射标题、海报、播放入口和更新时间；五语言文案同步。

## Plan

1. 定义 `MediaCard`、`MediaCardRepository` 和 provider health 状态。
2. 实现 HTTP provider 超时、缓存和播放入口；先接 MoonTV/Selene/OrionTV 可配置 URL。
3. 实现首页静态 Dock，再接历史/最近入库。
4. 将 WebDAV/SMB/NFS 放入独立媒体库工作包，避免侵入首页状态。

## Status

- 已完成：接口方向和离线原则定义。
- 当前动作：等待 Owner 接管。
- 阻塞：上游各服务 API/认证协议尚未确认。
- Exact next action：收集可公开的只读 API 响应样例，确定 provider adapter 接口。

## Evidence

- 事实依据：`ROADMAP.md`、`app/src/main/java/app/orchard/tv/HomeViewModel.kt`。
- 真机：未验证。

## Deferred

- AVS 入库事件推送、自动发现和原生网络挂载。

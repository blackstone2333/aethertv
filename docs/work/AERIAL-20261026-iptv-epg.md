# AERIAL-20261026-iptv-epg

Status: complete
Owner: main model / integration owner

## Spec

- 目标：把 M3U/EPG、离线缓存和源优先级规划做成独立数据模块，为后续 Media3/首页入口提供契约。
- 范围：新增 `live/model|parser|repo`；扩展令牌保护的手机 Web API 和配置页；不接入现有 `HomeViewModel`。
- 非目标：不把网络频道混入 `SystemProbe`/实体 HDMI 输入，不硬编码 NAS 凭据，不先做首页实时预览。
- 约束：Media3 1.8.1 和 Android 5/6；局域网明文精确放行；焦点/离焦必须创建和释放播放器；单路硬解码器互斥。
- 验收：解析器/回退/缓存/配置纯 Kotlin 单测；手机页可保存、刷新、删除源；真实源连通性和 F3 预览单列为设备证据。

## Plan

1. 先定义 `Channel`、`EpgProgram`、`LiveSourceConfig`、`LivePlaybackState`。
2. 实现 M3U/XMLTV.gz 解析、大小限制、缓存和优先级回退。
3. 接入手机端 `api/live/*` 配置、刷新、删除和状态。
4. 保持播放器/首页预览为后续工作包，先用离线契约验收。

## Status

- v0.1 已完成：模型、M3U、XMLTV/XMLTV.gz、边界限制、磁盘 TTL/容量缓存、陈旧回退、源优先级规划和手机源管理。
- HTTP 源例外限定在直播传输，不修改全局网络策略；HTTPS 使用平台证书校验。
- 播放器、首页预览和无感播放切源仍为后续工作，本包完成仅指配置/解析/缓存基线。

## Evidence

- 每个 flavor 41 项共享/回归测试通过，其中直播覆盖解析、源配置、缓存、回退与 HTTP 传输。
- Redmi K40S L1：网页保存/刷新、M3U 1 个频道、EPG.gz 1 个节目、503 离线回退、重新启动后的磁盘缓存及删除临时源均通过。
- 构建、清单、Web JS/五语言证据见 [手机验收](AERIAL-20261026-phone-acceptance.md)。真实家庭源与 F3 播放未验证。

## Exact next action

在独立工作包接入 Media3 全屏播放器，明确会话释放和解码器互斥后再连接首页。

## Deferred

- 真实源默认值、自动发现、Media3 全屏/焦点释放和首页画中画，待配置稳定且有设备证据后。

# AERIAL-20261026-roadmap-phase2

Status: complete
Owner: main model / integration owner

## Spec

- 目标：交付 Roadmap v0.1 开发验收版，建立可持续协作的家庭影音桌面基线。
- 范围：Goldilocks 风格工作/交接规则、品牌可见文案、Auto/Full/Lite 与内存回收、手机控制台、M3U/XMLTV/XMLTV.gz 配置/解析/缓存与优先级数据契约。
- 验收：规则可执行；Java 17 双 Release 与受影响测试通过；包名/清单正确；Redmi K40S L1 安装、启动、手机源管理、离线缓存和 Lite 回前台通过。
- 固定约束：`com.dangbei.tvlauncher` / `app.orchard.tv`、minSdk 21、五语言、保留当贝原厂桌面和现有无障碍服务；不提交本机地址/凭据/令牌。
- 非目标：v0.1 不交付直播全屏播放器、首页预览、NAS 海报墙/网络挂载、其他品牌打包矩阵或 F3 L3 结论。Roadmap 的这些项目仍需后续独立工作包。

## Plan

1. 项目地图、角色/文件所有权、工作包/交接模板和预检 — 已完成。
2. 品牌中文化与定制版更新边界 — 已完成。
3. Lite 渲染策略、可见选择、后台缓存释放/恢复 — 已完成。
4. IPTV 模型/解析/缓存/源优先级规划和手机配置 — 已完成。
5. Java 17 构建、双 flavor 清单与 Redmi L1 — 已完成，详见 [手机验收](AERIAL-20261026-phone-acceptance.md)。
6. NAS/Media3/多品牌矩阵与 F3 L3 — 从 v0.1 分离，保留在后续包。

## Architecture decisions

- IPTV 独立于 `SystemProbe` 和 `HomeViewModel.reload()`，网络失败不得阻塞首页。
- HTTP 只用于显式配置的直播请求，不全局允许 cleartext；HTTPS 继续校验证书，跨 origin 重定向清除配置头，拒绝降级。
- 后续 Media3 预览、全屏和屏保需管理解码器互斥、焦点延迟与离焦释放。
- NAS 凭据不得硬编码；手机配置沿用随机 token，配置保留应用私有存储。

## Status

v0.1 范围已完成并在手机验证。公开仓库为 `blackstone2333/aethertv`，本地 `main` 与远程同步开发。F3 专属行为仍为“未验证”。

## Evidence

- 实现基线提交 `3a811f4`，直播契约测试 `dfc6758`；后续手机修复及设备证据见 [phone-acceptance](AERIAL-20261026-phone-acceptance.md)。
- Java 17 双 flavor Release、每个 flavor 41 项测试、预检、Web 五语言/JS、APK 元数据：通过。
- Redmi K40S Android 14：USB 安装/启动/编译优化成功，无线 ADB 启动成功；网页管理、M3U/EPG.gz 在线数量、离线与重新启动后的磁盘缓存、Lite 选择和后台恢复通过。
- 当贝 F3：HOME、原厂桌面心跳、黑屏、HomeGuard、遥控器、HDMI、音量、多任务、待机和硬解码均未验证。

## Exact next action

收集用户对 v0.1 的试用反馈；按独立工作包接 Media3 全屏播放和真实源，再推进 NAS Dock 与 F3 L3。

## Deferred

完整播放器/预览/NAS/多品牌 flavor、真实源质量和电视验收；禁止把数据层备用 URL 规划写成已实现无感播放切源。

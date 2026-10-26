# AERIAL-20261026-iptv-epg

Status: complete
Owner: main model / integration owner

## Spec

- 目标：把 M3U/EPG、本地/公网/IPv6 回退和 Media3 直播播放做成独立模块，供首页 Dock 和全屏入口消费。
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

- 已完成：模型、M3U `EXTINF`、XMLTV/XMLTV.gz、输入/输出上限、磁盘 TTL/容量缓存、离线陈旧回退、源优先级/备用 URL 规划、手机端源管理和刷新状态。
- 当前动作：进入发布验收；播放器和首页预览保持 deferred。
- 阻塞：真实源地址和 EPG 质量需运行时确认；Redmi 已连接但 MIUI 拒绝 ADB 安装，需要打开设备端“通过 ADB 安装/安装 via USB”授权。
- Exact next action：完成手机端安装授权后安装 standard release，打开手机页保存一个局域网源并执行刷新；再决定是否接入全屏 Media3。

## Evidence

- `app/src/main/java/app/orchard/tv/live/`：模型、解析器、缓存、Feed Repository、Failover Planner、配置存储。
- `app/src/main/java/app/orchard/tv/wallpaper/PhoneServer.kt` 与 `app/src/main/assets/web/index.html`：`/api/live/state`、`/api/live/source`、`/api/live/source/delete`、`/api/live/refresh`。
- `ANDROID_HOME=/Users/black/Library/Android/sdk JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testStandardReleaseUnitTest testGuardReleaseUnitTest --no-daemon`：通过；标准/守护共享测试成功。
- `node --check /tmp/aethertv-web.js`、`git diff --check` 和 `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./tools/check-project-rules.sh`：通过。
- 双 flavor Release 构建：`assembleStandardRelease assembleGuardRelease` 通过；APK 包名、namespace、版本和 HOME/CONFIG 清单已用 `aapt2` 核验。
- 真机：Redmi K40S 已配对并连接，Android 14 / `munch`；APK 安装被 MIUI 以 `INSTALL_FAILED_USER_RESTRICTED` 拒绝，手机页/播放未验证；当贝 F3 专属行为未验证。

## Deferred

- 真实源默认值、自动发现、Media3 全屏/焦点释放和首页画中画，待配置稳定且有设备证据后。

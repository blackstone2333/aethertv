# NAS 独立播放器选型与桌面接口

Owner: parent desktop conversation
Status: review
Checked: 2026-10-27

用户目标是独立影音应用负责视频库与播放，以太桌面展示推荐、继续观看和一键播放入口。当前范围为源码选型与接口设计；尚未接入用户NAS或实现NAS播放器。

## 选型

| 项目 | 当前源码依据 | 适用方向 | 结论 |
| --- | --- | --- | --- |
| [Jellyfin Android TV](https://github.com/jellyfin/jellyfin-androidtv) + [Jellyfin服务端](https://github.com/jellyfin/jellyfin) | TV的[版本目录](https://github.com/jellyfin/jellyfin-androidtv/blob/master/gradle/libs.versions.toml)为minSdk23、JDK21；GPL-2.0体系 | 影片海报库、账号、继续观看、字幕和服务端转码 | 优先评估Java17兼容历史基线。Android6符合F3系统版本；真实硬解码和性能仍需F3验证。桌面继续minSdk21，NAS应用单独声明兼容范围 |
| [NOVA Video Player](https://github.com/nova-video-player/aos-AVP) | [aos-Video构建文件](https://github.com/nova-video-player/aos-Video/blob/nova/build.gradle)为minSdk23；主仓Apache-2.0，含多种原生子模块 | 直接播放NAS共享文件、文件浏览与本地媒体库 | 适合作为不部署影音服务端的第二路线；原生构建、FFmpeg及其他依赖须分别核验，不能只按主仓许可证推断发行条件 |
| [Findroid](https://github.com/jarnedemeulemeester/findroid) | [Versions.kt](https://github.com/jarnedemeulemeester/findroid/blob/main/buildSrc/src/main/kotlin/Versions.kt)的MIN_SDK为29；GPL-3.0。已有TV模块，README仍列TV为计划功能 | 较新Android上的Jellyfin体验 | 当前不选作F3基线；不能把Android10应用直接当Android6候选 |
| [Kodi](https://github.com/xbmc/xbmc) | [Android构建说明](https://github.com/xbmc/xbmc/blob/master/docs/README.Android.md)要求NDK28c，包含大量原生与插件依赖 | 完整媒体中心、广泛协议和插件 | 暂不作为轻量第一版基线；本轮未确认其当前minSdk，也未测量四个项目的实际内存占用 |

Jellyfin是与Emby相近的开源服务端及客户端组合；当前Jellyfin TV不能直接作为兼容现有Emby服务器的承诺。如果需要保留Emby，后续必须单列认证、目录、播放和续播API适配，而非把服务器URL替换后即视为支持。

## 首版建议

优先评估Jellyfin TV的Java17兼容历史基线，作为独立NAS客户端原型，修改远程焦点、海报墙、设置与播放器UI，统一以太桌面的组件视觉。账号、令牌、扫描、刮削、字幕和解码都由该应用持有；桌面不引入Jellyfin SDK、网络文件系统客户端或视频数据库。

当前上游版本目录明确要求JDK21，不能直接照搬进本项目。下一包先固定并验证可维护的Java17兼容历史基线及Android6依赖，再做最小原型；若没有合适基线，就只借鉴其服务端API并另做轻量Java17客户端。本项目不静默升级JDK。完整开源发行应保留所采用项目的许可证、源码及依赖归属。

## 待实现的桌面契约

- 版本化只读ContentProvider快照：最多24张卡片，包含opaque provider/item ID、标题、受控缩略图URI、已观看进度与更新时间。图片按桌面卡片尺寸提供，桌面缓存有字节上限。
- `PLAY_ITEM v1`通过显式目标包传递item ID和可选续播位置；播放器从自己的媒体库解析内容。桌面不接收密码、访问令牌或任意原始流URL。
- 同签名应用使用signature权限及只读URI授权。第三方客户端需要显式授权适配器，不能为了接入任意App而公开私有影片库。
- 播放器在内容或进度改变后发送有界失效通知；桌面只在前台或恢复时读取小快照，不在后台轮询、联网扫库或启动第二解码器。
- Android5/6使用普通ContentProvider与显式Intent；Android TV推荐频道API26+只能做附加能力。
- 未安装播放器、离线、快照过期或无授权时，桌面保持可导航，展示已有缓存或空状态；不影响HomeViewModel.reload或HomeGuard。

此NAS契约尚未编码，也不属于当前AetherLive直播v1接口。两者共享独立播放器的边界原则，不复用尚未约定的字段。

## 下一工作包的验收

1. 固定NAS客户端源码基线，Java17构建、包名/清单和依赖兼容检查通过。
2. 测试服务端覆盖登录、目录、续播、字幕、离线/过期令牌及显式播放；不会把凭据写入桌面或公开日志。
3. 手机完成海报推送→桌面→指定影片播放→返回与进度更新的端到端验证，测量桌面与播放器分别占用。
4. F3另验安装限制、遥控器、MStar解码、长时播放和资源释放；未验证项明确保留。

本轮证据来自公开源码与构建声明，原始下载存于local；未编译或安装上述四个NAS候选，未验证用户的真实媒体库。

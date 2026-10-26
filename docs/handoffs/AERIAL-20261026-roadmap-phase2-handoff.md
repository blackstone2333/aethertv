# AERIAL-20261026-roadmap-phase2 handoff

Status: review
From: main model / integration owner
To: next AetherTV implementation session
Repository: `/Users/black/Library/CloudStorage/SynologyDrive-air/NIE/vibecoding/AetherTV`
Work unit: `AERIAL-20261026-roadmap-phase2`

## Current state

- 已完成：AetherTV 品牌中文化基线、Lite Full/Auto/Lite 渲染策略、后台内存回收、IPTV M3U/XMLTV/XMLTV.gz 解析、磁盘 TTL/容量缓存、离线陈旧回退、源优先级规划和手机源配置页。
- 未完成：Media3 IPTV 全屏播放、首页画中画预览、NAS/MoonTV/Selene/OrionTV Dock、真实局域网源质量和当贝 F3 电视专属验收。
- 明确非目标：不更换 `com.dangbei.tvlauncher` 或 `app.orchard.tv`，不卸载/停用 `com.dangbei.leard.leradlauncher`，不覆盖无障碍服务，不把网络请求加入 `HomeViewModel.reload()`。

## Changed surfaces

- 文件范围：`app/src/main/java/app/orchard/tv/live/**`、`app/src/test/java/app/orchard/tv/live/**`、`PhoneServer.kt`、`assets/web/index.html`、Lite 生命周期和渲染组件、五语言品牌资源、项目工作包文档。
- 共享接口：`LiveSources`、`LiveFeedRepository`、`M3uParser`、`XmlTvParser`、`FailoverPlanner`；手机 API 为 `api/live/state`、`api/live/source`、`api/live/source/delete`、`api/live/refresh`。
- 依赖方向：手机 Web → `PhoneServer` → `LiveSources` → 解析/缓存；首页暂不依赖直播网络状态。

## Evidence

- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./tools/check-project-rules.sh`：通过。
- `ANDROID_HOME=/Users/black/Library/Android/sdk ANDROID_SDK_ROOT=/Users/black/Library/Android/sdk JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testStandardReleaseUnitTest testGuardReleaseUnitTest --no-daemon`：通过。
- `assembleStandardRelease assembleGuardRelease`：通过；`aapt2 dump badging/xmltree` 确认包名 `com.dangbei.tvlauncher`、namespace `app.orchard.tv`、versionCode 101、standard 含 HOME、guard 不含 HOME、两者含 CONFIG action。
- `node --check /tmp/aethertv-web.js`、`git diff --check`：通过。
- 公开仓库：`https://github.com/blackstone2333/aethertv`；`main` 已同步至 `806ed74`，包含实现与 6 个 IPTV 契约测试文件。
- 真机：Redmi K40S 无线调试配对成功，识别为 Android 14 / `munch` / 1080×2400；standard APK 安装被 MIUI 拒绝为 `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`，尚未启动应用；当贝 F3 所有专属行为均为“未验证”。

## Risks and authority

- 始终保持 `applicationId = com.dangbei.tvlauncher`、namespace `app.orchard.tv`、Java 17、minSdk 21/Android 5/6 兼容线。
- 当贝 F3 的原厂桌面必须保持启用；禁止使用无障碍监听替代已有系统链路。
- IPTV URL 由手机页配置并存入应用私有存储；不要把真实 URL、令牌、凭据或设备 IP 写入仓库文档。
- 回滚路径：按本工作包提交回退 live/PhoneServer/Lite/品牌资源；不回滚上游 Aerial 兼容修复或 `local/`。

## Exact next action

在 Redmi K40S 开启“无线调试”并完成配对，取得当前 adbd 端口后执行：

```text
adb connect <手机地址>:<当前端口>
adb install -r -g app/build/outputs/apk/standard/release/app-standard-release.apk
adb shell monkey -p com.dangbei.tvlauncher -c android.intent.category.LAUNCHER 1
```

然后扫描 AetherTV 手机页二维码，保存一个局域网 M3U/EPG 源，刷新一次并断网确认陈旧缓存仍可浏览；记录设备型号、APK flavor、版本和结果，再决定是否拆 Media3 全屏播放工作包。

## Completion condition

下一次接管完成后，手机 L1 证据应包含安装、启动、手机页源保存/刷新/离线回退；若进入电视 L3，还需单列 HOME 接管、原厂桌面心跳、黑屏、遥控器、HDMI、硬解码和待机复活证据。

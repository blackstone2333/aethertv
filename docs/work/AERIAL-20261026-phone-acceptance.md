# AERIAL-20261026-phone-acceptance

Status: complete
Owner: main model / integration owner

## Spec

- 目标：完成 v0.1 Redmi K40S L1 安装、启动、手机控制台、M3U/XMLTV.gz 和离线回退验收。
- 验收：standard Release 可运行；手机页保存/刷新/删除源；在线数量正确；离线及重新启动后返回磁盘缓存；Auto/Full/Lite 可选择且持久化；后台释放图片后回前台恢复壁纸。
- 范围：`live/repo/**`、对应单测、`MainActivity.kt`、`Wallpapers.kt`/系统屏保共享缓存、渲染设置入口、更新与品牌文案、五语言资源、安装脚本的定制版身份修正和文档。
- 约束：Java 17；`com.dangbei.tvlauncher` / `app.orchard.tv`；Android 5/6 兼容；HTTP 例外限定在配置的直播请求，不全局放行；保留手机默认桌面和现有无障碍配置。
- 用户将测试连接改为 USB，安装验收以 USB 为准；已配对无线连接另作启动检查。
- 非目标：直播全屏播放、首页预览、NAS Dock；F3 HOME/黑屏/HDMI/遥控器/待机/硬解码仍未验证。
- 依赖：[v0.1 总包](AERIAL-20261026-roadmap-phase2.md)、`LiveSources` 和令牌保护的 `PhoneServer`。

## Plan

1. USB 安装与启动 standard Release — 已完成。
2. 临时 HTTP M3U/XMLTV.gz 服务验收，修复传输并添加回归测试 — 已完成。
3. Lite 选择、后台释放/回前台恢复、定制版品牌与更新入口 — 已完成。
4. Java 17 双 Release、单测、APK 元数据、网页和手机复验 — 已完成。
5. 清理临时源/端口/服务/私密二维码，更新交接并同步仓库 — 已完成。

## Status

v0.1 L0/L1 通过，可在 Redmi K40S 上试用。真实家庭源质量和 F3 L3 验收保留为后续独立工作。手机测试不证明电视兼容性或 0.4 秒启动指标。

## Evidence

### L0：构建和回归

- 环境：`JAVA_HOME=/opt/homebrew/opt/openjdk@17`，Android SDK 使用本机已配置路径。
- `./gradlew testStandardReleaseUnitTest testGuardReleaseUnitTest assembleStandardRelease assembleGuardRelease --no-daemon`：成功；每个 flavor 41 项测试、0 失败。后续仅生命周期修复，再构建双 Release 成功；直播逻辑及测试未改动，复用已通过的单测证据。
- 新传输测试覆盖 fixed-length/chunked、体积与截断拒绝、跨 origin 清除凭据、同 origin 保留头和查询路径、请求头注入拒绝。
- `aapt2 dump badging/xmltree`：包名均为 `com.dangbei.tvlauncher`，完整 Activity 为 `app.orchard.tv.MainActivity`；standard `101 / 1.0.1-dangbei` 有 HOME，guard `101 / 1.0.1-dangbei-guard` 无 HOME，两者有白名单 CONFIG action。
- `check-project-rules.sh`、Web JS 语法、五语言表长度/占位符、`bash -n tools/install.sh tools/restore.sh`、`git diff --check`：通过。

### L1：Redmi K40S

- 设备：`22021211RC / munch`，Android 14；standard Release，versionCode 101。
- `adb -s <USB serial> install -r -g .../app-standard-release.apk`：Success；`am start -W -n com.dangbei.tvlauncher/app.orchard.tv.MainActivity`：Status ok；`cmd package compile -m speed -f com.dangbei.tvlauncher`：Success。
- 已配对的无线 ADB transport 执行同一启动命令：Status ok。未再要求无线安装。
- 控制中心显示“以太桌面 1.0.1”，没有上游版本升级提示。更新 manifest/APK/安装说明均改为本仓库；尚无公开 Release，实际 OTA 下载与安装未验证。
- 浏览器通过本机桥接访问手机实际 `PhoneServer`：网页保存临时源、刷新、选择 Lite 均成功；可见“当前使用轻量模式”与“1 个频道 · 1 个节目 · 离线缓存”。浏览器为桌面内置浏览器的手机宽度视图，未单独验证手机 Chrome/Safari。
- HTTP fixture：在线 M3U 1 个频道、gzip EPG 1 条节目，`fromCache=false/stale=false`；fixture 返回 503 后相同数量仍可用，`fromCache=true/stale=true`，无刷新错误。
- 重新覆盖安装并启动后，Lite 设置仍在；源离线刷新仍获得上述磁盘缓存。数据来自可控 fixture，不能替代真实家庭 IPTV 源质量结论。
- Auto/Full/Lite 三种 API 选择及实际状态正确；非法模式返回 400，缺少二维码令牌的状态请求返回 404。
- Lite 下打开系统设置：日志出现 `onStop`、`Home hidden: released wallpaper and UI caches`；回到桌面 Status ok，前后截图逐字节一致，壁纸恢复。Android 拒绝向该前台服务进程注入后台 trim，本次用真实隐藏/回前台验证释放路径，未将手动 trim 注入写成通过。
- 临时源已按创建 ID 删除，源数量恢复为 0；渲染恢复原来的 Auto；临时服务器/桥接、单独创建的 ADB 转发与反向端口、设备截图与本机私密二维码/令牌文件已清理。仅保留不含令牌的本机验收截图，未上传仓库。

## Exact next action

用户在手机上试用并提出布局与视觉调整；下一开发单元先实现独立 Media3 全屏直播，再接真实家庭源和 NAS 卡片；在目标 F3 上逐项补 L3。

## Deferred

- F3 HOME/原厂桌面心跳/黑屏、系统屏保共享缓存、遥控器、HDMI、音量、多任务、待机和 MStar 硬解码的 L3。
- 真实家庭源、IPv6 网络端到端、播放器切源与解码性能；当前只有数据层优先级规划。
- NAS/MoonTV/Selene/OrionTV Dock、首页预览、统一冷启动性能基准和公开 OTA 发行。
- 安装/还原脚本已修正 package/namespace 和下载归属，只有语法与静态检查，电视接管/还原流程未实测。

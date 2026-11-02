# AERIAL-20261027-independent-media

Status: review
Owner: independent IPTV conversation (player); desktop conversation (desktop contract consumer)

## Spec

- 用户确定：直播成为独立播放器，基于 lizongying/my-tv-0 调整 UI/设置；以太桌面保留一键直播与小型接口，减少职责和占用。
- 保持上一个模块化控件工作包的结果，先做其手机验收，再进行播放拆分。Java17、Android5/6兼容；桌面白名单包名/namespace、HOME、守护和用户数据边界不变。
- 独立播放器需要独立 APK/进程、遥控器 UI、源配置、频道/EPG、恢复上次频道、有界失败回退；启动/定频道接口有版本号。桌面不后台预览或轮询频道库，不自动安装/登录第三方服务。
- NAS 范围为候选调研与接口设计：优先开源、Android TV、Android6、Emby/Jellyfin服务器兼容和桌面内容推送。不在缺少NAS实例/账号时声称私有服务播放已验证。
- my-tv-0 读取固定commit、保留MIT归属；按最小依赖取舍兼容代码，避免引入桌面守护、全局代理和未审核二进制解码器。
- 可观察验收：独立APK可单独启动和配置，桌面一键跳入；离开释放会话；已有源可经用户明确导入迁移而不丢失；协议测试/双桌面Release/独立播放器Release，Redmi端到端验证。F3与实际家庭源未验证。

- 用户补充UI验收：遵循Apple设计原则，实现液态玻璃材质并优化视觉与操作逻辑。播放器使用同帧视频纹理折射，静态设置使用同源缓存材质，禁止额外解码器/逐帧CPU截图；频道、源列表、编辑与返回层级清晰。

## Plan

1. 查阅my-tv-0许可证、源码和依赖；筛选Jellyfin等NAS客户端，记录选型证据。
2. 定义独立播放器与桌面契约，抽离播放/配置所有权，保留既有数据恢复路径。
3. 构建和验证，更新Roadmap、项目地图和可交接证据。

## Status

2026-10-28：独立播放器实现与L0通过，Redmi K40S / Android14完成本轮手机验证，待父对话集成与F3验收。独立包app.aethertv.live 0.1.0/1；未提交、未推送、未发布。本机debug key签署的Release仅为测试包。

- 桌面旧live代码/测试迁移到players/live；共享契约v1、只读签名导出与桌面启动桥保留。播放器不依赖MainActivity、桌面材质或设置实现。
- TV表单支持新增/命名/编辑M3U或TXT及EPG地址、启停、二次确认移除。保存/取消固定在滚动区外；Back取消草稿并恢复来源焦点；显式导入仅合并缺失ID，整份校验失败不会部分写入，保留已有headers/priority/timeout。
- Apple风格材质：播放浮层折射同一OES视频纹理，方向性边缘高光、层次与焦点弹簧；设置页使用静态缓存背景与折射镜片。无逐帧CPU截图、额外解码器或空闲持续动效；GL初始化失败回退SurfaceView。频道序号/分组/当前播放勾选与焦点分开表达。
- PLAY/SETTINGS检查v1；可选channel_id仅匹配已有opaque key，任意URL不作为播放入口。先解析请求频道再分配会话；Android singleTask将SETTINGS交给现有播放器根Activity时内部转发设置。离开同步释放一个前台Media3会话，返回设置后恢复最新频道。
- my-tv-0仅选择性适配固定commit的TXT分组/逗号多线路/重复合并，不宣称完整fork；MIT归属位于third-party与APK assets。

## Evidence

- 上游源码读取于local/my-tv-0，不提交该Git目录。采用路径/改动见players/live/README.md。
- L0：`JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :liveplayer:testReleaseUnitTest :liveplayer:assembleRelease`，exit 0，独立Release构建成功，43项单测通过（含TXT分组多线路、迁移幂等/不覆盖/原子拒绝、契约ID及Android6/9入口版本校验、玻璃光学中心/边缘对称性、请求频道优先选择与SETTINGS转发）。本地日志local/liveplayer-build.log，报告players/live/build/reports/tests/testReleaseUnitTest/。
- L0：aapt2 badging确认app.aethertv.live / 0.1.0 / 1 / min21 / target34；apksigner verify成功，当前本地debug签名（仅测试包）。产物players/live/build/outputs/apk/release/liveplayer-release.apk。
- Android5运行时未验证：Robolectric4.17对SDK21报UnknownSdk，测试矩阵使用其支持的SDK23/28；该runner限制不作为产品失败或Android5通过证据。APK清单min21及依赖兼容声明通过。
- 首次构建因环境未指定Android SDK失败；指定ANDROID_HOME后补齐脚手架遗漏的图标资源与GlyphPaths依赖，后续构建通过。
- 桌面双Release由父对话集成验证，播放器不修改父对话UI所有权范围。

- L1：Redmi K40S / Android14，独立Release 0.1.0/1安装并执行speed编译成功。设置/编辑/保存固定按钮、取消草稿与来源焦点、导入2个自建源、重复导入不覆盖编辑内容通过；无签名权限的shell读取旧provider被拒绝。
- L1：自建临时信号验证HLS主线路503后备线路首帧、EPG标题/进度、同帧OES玻璃；最终APK验证TXT解析与chunked TS首帧、SETTINGS离开释放与返回恢复、请求已知频道启动、未来协议版本拒绝、不可用频道有界结束且提供重试/频道入口。桌面天线按钮启动独立LiveActivity通过。未将手机注入方向键等同于电视遥控器验收。
- 本地证据：`local/liveplayer-acceptance/`内direct-channel-proof、nonoverwrite-final-proof、txt-settings-resume-proof、offline-final-proof、desktop-launch-final-proof、version-rejection-final-proof与截图。设备连接、配置和原始截图不入版本库。
- 最终APK SHA-256：`3e0ef5581dc0afb1eebbd6c524bfc084b6c82e2a241c6b980c2cd2b3dc495a2a`；元数据与签名复核通过。

- 清理完成：仅删除两条记录的播放器/旧桌面测试源，旧配置与测试前基线一致，用户原有源保留；恢复父对话桌面快照并speed编译、回到桌面。自建服务与端口映射已停止/移除，设备锁已释放。证据player-cleanup-proof与legacy-cleanup-proof。

## Exact next action

2026-11-02起播放器代码与43项独立测试在`codex/aerial/aerial-20261102-live-independent`维护；桌面分支仅消费v1契约并进行自身双Release验证，不包含或等待播放器实现。桌面隔离状态见`AERIAL-20261102-desktop-independent`。

在当贝F3开机后补做独立包安装白名单、真实遥控器/焦点/返回、MStar硬解码、GL材质与退出恢复验收。手机通过不代表电视通过。

## Deferred

- NAS真实账号/服务器连接及内容播放；F3硬解码/遥控器、白名单与安装限制。

# AetherCinema 独立Android TV影音应用交接

Status: active
Owner: AetherCinema · Jellyfin / Emby 影音 (`01a17e1d-7997-7bb0-9971-34e978823f0e`, local)

## 用户授权与目标

用户于2026-10-28明确要求再开独立对话，基于Jellyfin做新UI的Android TV应用，适配Emby后端，最好支持MoonTV和飞牛视频。你负责持续实现独立应用；父对话保留桌面集成，已有AetherLive对话继续负责IPTV。

权威工作包：`docs/work/AERIAL-20261028-nas-cinema.md`。先读AGENTS、PROJECT、AGENT_WORKFLOW、HANDOFF必须遵守部分、ARCHITECTURE、DESIGN_SYSTEM和NAS_PLAYER_PLAN。UI实施时读Apple Design技能，并查阅Apple公开tvOS焦点、遥控器和材质规范；不虚构官方版本或文档内容。

## 已知事实

- 路径为当前AetherTV项目；共享分支`codex/aerial/aerial-20261027-lite-performance`，桌面检查点`1c50d16`。
- 根目录当前有大量IPTV未提交差异。`players/live/**`、`contracts/**`、app里的MediaBridge/旧配置导出、manifest、PhoneServer、web、五语言资源与根构建脚本由IPTV Owner持有，不能覆盖。
- NAS仅完成选型记录，尚无NAS工程。当前Jellyfin TV要求JDK21、minSdk23；必须先找到并验证Java17兼容基线，保留真实上游源码来源、GPL与依赖许可。
- 暂名AetherCinema，独立applicationId`app.aethertv.cinema`，namespace`app.orchard.tv`，0.1.0/1；Android6目标。桌面仍为com.dangbei.tvlauncher / app.orchard.tv / 0.3.1 / 106，min21，不擅自改动。
- 构建JDK `/opt/homebrew/opt/openjdk@17`，SDK `/Users/black/Library/Android/sdk`。不要从HOME、系统根或CloudStorage根递归搜索。

## 所有权与实施

- 你独占`players/nas/**`和NAS工作包/本交接。先把NAS工程作为该子目录内独立Gradle根构建，不改共享settings.gradle.kts/build.gradle.kts或现有播放器模块。
- 基于真实Jellyfin源码做TV UI和Jellyfin/Emby适配；不要做只有静态海报的demo、WebView壳，或只改服务器品牌判断。具体provider/capability设计由你核查源码后落地。
- 按工作包验收持续实现到可安装测试APK与实际可测播放闭环。缺少真实服务器时可写可复用测试fixture，及时记录需要用户提供的服务版本/地址信息，继续不依赖账号的工作。
- MoonTV有多个同名或衍生项目；确认具体协议，飞牛视频同样按实际版本验证，不把不同后端伪装成Jellyfin/Emby。核心Jellyfin/Emby闭环优先。
- 先在`players/nas/docs/DESKTOP_CONTRACT.md`提出只读快照、受控图片URI、opaque item ID及PLAY_ITEM v1契约。最多24张小图；账号和解码留在NAS应用，桌面不加后台扫描/轮询/解码。
- 不修改桌面UI、共享contracts、IPTV源码或根构建配置；需要共享改动写精确提案给父对话集成。不要提交/推送，父对话统一集成；不发布正式Release。

## 设备与验证

- Redmi K40S是L1手机环境，连接方式及私有记录只放local，设备说明见`docs/devices/redmi-k40s.md`。F3当前无在线测试证据。
- 先推进协议测试、独立构建、模拟器UI与fixture。需要手机时先读`local/coordination/`：任何ui-device-busy或live-device-busy存在时不运行该手机ADB命令、不安装、不改偏好；不要删其他Owner的锁。
- 手机闲置时先建立自己的nas-device-busy并检查其他Owner是否有设备请求，完成后立即移除并记录恢复状态；只在实际测试期间占用，不在实现/构建时持锁。其他Owner可能尚未识别NAS锁，开始手机测试前先在local/coordination/nas-device-requested声明需求，由父对话协调共享设备规则，避免抢用。
- 使用Release并对自己的独立包执行speed编译；记录包名/版本/命令。保留桌面及用户安装应用、偏好、媒体和账号；仅清理本轮记录ID的测试数据。
- 不把编译、mock或手机验证写成F3/真实后端通过；保留真实失败与缺口。无需为无关桌面UI重复跑所有旧检查。

## 当前执行状态、恢复和完成条件

本交接只创建了NAS工作包和说明，尚未创建源码、APK或修改后端。父对话已读取规则、git状态和现有NAS选型资料；这些不是NAS功能完成证据。

接管后检查git状态，记录选定Jellyfin commit及Java17构建结果；随后实施TV应用和Emby adapter，并将验证与Exact next action持续写入本工作包。若遇到兼容障碍，保存可运行检查点和明确诊断，不重置共享工作树或擅自升级JDK。完成条件以工作包Acceptance为准，保留GPL归属与所有未验证边界。

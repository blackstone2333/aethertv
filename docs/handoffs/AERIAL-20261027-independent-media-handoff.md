# 独立IPTV播放器交接

Status: review
Owner: independent IPTV conversation (isolated workspace)

## 当前交接：2026-11-02开发隔离

- 工作区：`/Users/black/.codex/worktrees/aetherlive-independent/AetherTV`。
- 分支：`codex/aerial/aerial-20261102-live-independent`；基线`5edcc5bd2b57a9061a4478a682f4903d1369790b`。
- 本轮仅隔离与检查点，不继续开发功能、不操作设备、不发布Release。原共享目录按内容读取复制，未搬走、删除、清理、切换或重置任何文件。phone原始证据留在原目录local。
- 后续唯一可写范围是新工作区`players/live/**`及本工作包/交接。`contracts/**`为当前v1固定副本，不继续修改；协议变动先提案交桌面Owner。原目录app/根构建/桌面docs/NAS不得修改。
- 独立入口：`JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk players/live/gradlew -p players/live --no-daemon testReleaseUnitTest assembleRelease`。也可进入players/live后执行其wrapper。该Gradle根只加载AetherLive，不加载:app或NAS。
- wrapper及插件版本沿用基线Gradle8.14.3/AGP8.12.0/Kotlin2.2.10，min21和版本0.1.0/1不变。仅新增自己的settings/gradle.properties/wrapper；不改仓库根构建。local签名策略保持，密钥和测试数据没有复制。
- 独立构建exit0，43测试0失败，projects仅Root project AetherLive且No sub-projects；APK 0.1.0/1/min21/target34、v1/v2签名与许可证核验通过。产物players/live/build/outputs/apk/release/AetherLive-release.apk；本轮未安装设备。
- APK SHA-256为428d22294582e8d3fba026455e0f4546fd875679b2190356b6d8ecd14c252892。契约源码SHA-256为22957e2cdfcc41d579a954a4bb6048544f37e0c10ea30bea1dd2041882f00f7e。
- 检查点标题`AERIAL-20261102-live-independent: isolate AetherLive build and handoff`，目标远端分支origin/codex/aerial/aerial-20261102-live-independent。完整证据见工作包“2026-11-02隔离证据”。完成检查点即停止，桌面可以独立继续。

以下保留历史实现与验收记录，旧共享范围权限已由上方隔离范围替代。

## 目标与边界

用户授权独立AetherLive播放器，选择性适配lizongying/my-tv-0，桌面保留一键直播和v1接口；随后要求Apple风格液态玻璃与合理操作。NAS由父对话及独立NAS对话负责。本包不修改其实现。

Java17/min21；独立applicationId app.aethertv.live、namespace app.orchard.tv、版本0.1.0/1。桌面包名不变。不得reset/clean或全量git add；历史实现未发布，本轮允许在独立分支提交/推送播放器检查点。共享目录中其他Owner的差异保持原样。

## 当前状态与变更

独立Release可构建，43项单测通过；Redmi K40S / Android14通过本轮手机验证，F3未验证。完整证据见docs/work/AERIAL-20261027-independent-media.md。

- players/live/**：独立模块、播放/源/EPG/解析/缓存/有界回退、TV表单与五语言、同帧视频玻璃和静态设置材质。GL失败回退SurfaceView；只有一个前台解码器，onPause同步释放。
- contracts/**：PLAY/SETTINGS v1；channel_id是mergeStations生成的opaque key（如id:…或name:…），只查已有频道，不接受任意URL播放。
- app/media/**与manifest/Gradle：签名保护只读LegacyMediaProvider、MediaBridge和可见性查询；原live代码/测试搬到独立模块，旧配置保留。
- PhoneServer/web：旧直播源管理已移出桌面。源设置/迁移由独立播放器负责。
- 独立设置：名称/地址/EPG/启停一起保存，Back取消草稿并恢复来源焦点；保存/取消固定在滚动区外，移除须二次确认。显式迁移整份校验、仅补缺失ID，不覆盖现有配置。
- Android singleTask可能把外部SETTINGS发给LiveActivity根；已验证v1后内部转发LiveSettingsActivity。先选择请求频道再分配解码器，避免默认频道短暂播放；返回设置后使用最新频道。
- 上游my-tv-0 commit19ab99c4c2f819fdfd08b09f64219bb7cd93eaa1，MainViewModel.kt TXT分组/逗号多线路/重复合并语义选择性适配。MIT在third-party与APK assets，不是完整fork；无FFmpeg AAR、全局代理、自动升级。

## 历史文件所有权（本轮后不再授权修改共享文件）

本包已改范围：players/live/**、contracts/**、app/media/**、app manifest/build与settings Gradle、PhoneServer/web、原live源码/测试路径、相关五语言资源、本工作包与交接。

父对话独占HomeScreen/HomeChrome/MenuScene/LiquidGlass/ControlPanel/ControlCenter/WallpaperSheet/AerialSettings/ControlCards/ControlLayout，以及模块化控件与设计系统文档。本播放器未改其UI文件；已有MediaBridge调用由父对话维护。

## 验证与恢复

- Java17 / Android SDK显式设置后运行 `./gradlew :liveplayer:testReleaseUnitTest :liveplayer:assembleRelease`，exit0，43测试0失败。最终APK为players/live/build/outputs/apk/release/liveplayer-release.apk；稳定测试副本local/releases/aetherlive-0.1.0-test/aetherlive.apk。
- APK包名/0.1.0/1/min21/target34及签名核验通过；本机debug key签署Release，仅测试。SHA-256：3e0ef5581dc0afb1eebbd6c524bfc084b6c82e2a241c6b980c2cd2b3dc495a2a。
- 手机：安装+speed编译，源导入/重复不覆盖、草稿取消、固定底部动作、签名provider拒绝shell，HLS503回退/EPG/玻璃、TXT与chunked TS首帧、设置离开释放及返回恢复、请求频道、未来版本拒绝、有限失败及重试/频道入口、桌面天线启动独立Activity。原始证据仅local/liveplayer-acceptance。
- Android5运行时未验证；Robolectric4.17不支持SDK21，SDK23/28测试通过，min21构建通过不代表Android5运行通过。
- 清理完成：只移除两条记录的测试源（播放器与旧桌面），保留所有非本轮源；旧配置与基线一致。恢复父对话desktop-current.apk并speed编译、桌面前台；自建服务/映射已移除，live-device-busy已删除、live-device-released已写。不得再运行seed。
- 无需回滚独立模块；继续在此差异上集成。不要清除播放器数据或卸载来恢复，用户已存有自己的源。

## Exact next action

本轮只交付隔离后的播放器源代码检查点。待用户重新安排后，仅在新工作区players/live及自有工作包/交接继续；不得操作原共享目录、桌面app/根构建/contracts或NAS，不要求桌面Owner等待此分支。F3完整播放/真实遥控器/硬解码仍未验证，后续设备验收需另行安排。

## Deferred / 完成条件

F3 Release、Android5/6真实设备、真实家庭流长期稳定性与GL降级路径待验；NAS实际服务不属于本包。代码与手机证据已供审阅，父对话完成集成及目标电视验收后才关闭产品验收。设备连接信息、户主配置、截图和凭据禁止入库。

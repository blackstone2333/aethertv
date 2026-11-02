# 独立IPTV播放器交接

Status: review
Owner: independent IPTV conversation → parent integration

## 目标与边界

用户授权独立AetherLive播放器，选择性适配lizongying/my-tv-0，桌面保留一键直播和v1接口；随后要求Apple风格液态玻璃与合理操作。NAS由父对话及独立NAS对话负责。本包不修改其实现。

Java17/min21；独立applicationId app.aethertv.live、namespace app.orchard.tv、版本0.1.0/1。桌面包名不变。不得reset/clean或全量git add；未提交、推送、发布。共享目录中其他Owner的差异保持原样。

## 当前状态与变更

独立Release可构建，43项单测通过；Redmi K40S / Android14通过本轮手机验证，F3未验证。完整证据见docs/work/AERIAL-20261027-independent-media.md。

- players/live/**：独立模块、播放/源/EPG/解析/缓存/有界回退、TV表单与五语言、同帧视频玻璃和静态设置材质。GL失败回退SurfaceView；只有一个前台解码器，onPause同步释放。
- contracts/**：PLAY/SETTINGS v1；channel_id是mergeStations生成的opaque key（如id:…或name:…），只查已有频道，不接受任意URL播放。
- app/media/**与manifest/Gradle：签名保护只读LegacyMediaProvider、MediaBridge和可见性查询；原live代码/测试搬到独立模块，旧配置保留。
- PhoneServer/web：旧直播源管理已移出桌面。源设置/迁移由独立播放器负责。
- 独立设置：名称/地址/EPG/启停一起保存，Back取消草稿并恢复来源焦点；保存/取消固定在滚动区外，移除须二次确认。显式迁移整份校验、仅补缺失ID，不覆盖现有配置。
- Android singleTask可能把外部SETTINGS发给LiveActivity根；已验证v1后内部转发LiveSettingsActivity。先选择请求频道再分配解码器，避免默认频道短暂播放；返回设置后使用最新频道。
- 上游my-tv-0 commit19ab99c4c2f819fdfd08b09f64219bb7cd93eaa1，MainViewModel.kt TXT分组/逗号多线路/重复合并语义选择性适配。MIT在third-party与APK assets，不是完整fork；无FFmpeg AAR、全局代理、自动升级。

## 文件所有权

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

父对话审阅并集成桌面双Release，更新PROJECT/ARCHITECTURE中旧内置live描述。F3开机后验收独立包安装白名单、遥控器焦点/返回、MStar硬解码、玻璃性能与退出恢复。电视结论仍为未验证；没有正式发布授权，只交测试包或prerelease。

## Deferred / 完成条件

F3 Release、Android5/6真实设备、真实家庭流长期稳定性与GL降级路径待验；NAS实际服务不属于本包。代码与手机证据已供审阅，父对话完成集成及目标电视验收后才关闭产品验收。设备连接信息、户主配置、截图和凭据禁止入库。

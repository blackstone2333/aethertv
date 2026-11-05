# AetherTV 项目地图

## 权威入口

- `AGENTS.md`：仓库级安全、代码、构建和设备操作约束。
- `docs/AGENT_WORKFLOW.md`：Goldilocks 风格的 Owner/Worker/Reviewer、工作包和交接规则。
- `docs/work/`：每个可验收工作单元的 Spec、Plan、Status 和证据。
- `docs/handoffs/`：只有发生真实所有权或会话转移时才创建的交接包。
- `docs/HANDOFF.md`、`docs/ARCHITECTURE.md`、`docs/devices/`：上游 Aerial 的行为和设备资料。
- 外部设备约束：Hermes 交接目录中的 `codex_aerial_handover.md`（不复制到公开仓库）。
- 产品目标：`ROADMAP.md`；视觉身份：`BRANDING.md`。

发生冲突时，安全边界和设备禁忌以 `AGENTS.md` 及外部当贝交接为准；当前源码和构建产物决定事实状态，旧文档不能覆盖新证据。

## 固定边界

- Android `applicationId` 必须是 `com.dangbei.tvlauncher`。
- Kotlin namespace 必须保持 `app.orchard.tv`；外部组件使用白名单包名加完整源码类名。
- 桌面构建和测试必须使用 Java 17；Compose/Media3 依赖保持 Android 5/6 兼容线。AetherCinema 的 Java21/min23 例外仅适用于独立播放器，不传播到桌面。
- 当贝原厂桌面 `com.dangbei.leard.leradlauncher` 保持启用，不得卸载或停用。
- 不覆盖现有无障碍服务，不修改 `local/`、签名密钥、IP、凭据或户主数据。
- 没有当贝 F3 真机证据的行为只能标记为“未验证”。

## 当前结构

```text
app/src/main/java/app/orchard/tv/
├── MainActivity.kt                 生命周期、HOME 入口和覆盖层
├── HomeViewModel.kt                应用目录、布局和首页状态
├── apps/                           应用发现、磁贴、布局和文件夹
├── ui/home/                        首页、焦点、长按菜单和屏保入口
├── ui/control/                     控制中心、系统输入和音量亮度
├── wallpaper/                      壁纸、屏保和手机 Web 服务
├── media/                          独立播放器启动桥与签名保护的只读旧配置导出
├── guard/                          开机复活、桌面守护和同步适配器
├── system/                         厂商能力检测和信号源适配
└── update/                         应用内升级
```

Roadmap 的新功能必须沿现有依赖方向接入：入口/状态 → 功能模块 → 系统适配。IPTV、NAS 和 Lite 先做独立边界，再由首页消费稳定的只读状态接口，不能把网络失败带入 `HomeViewModel.reload()` 或 HomeGuard 链路。

桌面根Gradle只加载`:app`，不需要任何`players/`目录或播放器构建产物；`contracts/src/main/java`仅包含无运行时依赖的v1协议常量。`media/MediaBridge`显式启动独立AetherLive，未安装时显示本地化提示；`LegacyMediaProvider`仅向相同签名的应用只读导出旧配置，桌面不再刷新频道库、管理直播源或分配直播解码器。旧偏好和缓存保留，迁移由播放器中的用户操作触发。

桌面、直播、NAS各用独立worktree/分支/构建入口。桌面Owner维护`app/**`、根构建和`contracts/**`；播放器Owner分别维护自己的目录、工作包和交接。桌面分支`codex/aerial/aerial-20261102-desktop-independent`；直播分支`codex/aerial/aerial-20261102-live-independent`（43项测试与独立Release通过，检查点3ff74e9）；NAS分支与最新证据由对应交接记录。本机路径仅在`local/`。接口改变先审阅，播放器工作树或后端状态不阻塞桌面，设备测试仍须共享协调。源协作目录保持原样，不作为新的桌面开发入口。

## 桌面定制与本地工具

- `AERIAL-20261026-desktop-personalization`：每排4–8个入口、航拍播放策略、素材分类偏好和受限局域网互传。
- `AERIAL-20261026-liquid-glass`：共享设计规范、同帧GPU视频玻璃和控制中心内的电视管家；详细证据与未验证边界见对应工作包。
- `system/SystemMonitor.kt` 只读系统状态；清理只移除应用可重建缓存。
- `system/TransferStore.kt` 使用应用私有 `files/exchange/`，单文件100MB、总量512MB、64项；文件访问通过随机ID，系统选择器负责本机导入/另存。PhoneServer 的二维码令牌保护全部网络入口。
- `docs/UPSTREAM_SYNC.md` 追踪选择性上游合入；每日维护避开活动开发，不整体覆盖定制。

## 版本规则

产品基线为 **0.3.0**。系统级更新、功能增改、普通优化分别递增第一、第二、第三位，高位递增后低位归零；构建次数不影响产品版本。Android 安装序号 `versionCode` 独立保持递增。早期 1.x 开发编号由该基线替代；当前优化测试版为 0.3.1 / 106，保留覆盖升级能力。

## 当前工作前沿

- `AERIAL-20261105-home-render-budget`：选择性适配Home航拍不请求影片帧率、磁贴阴影/焦点框阈值避免逐帧整块重组、名称淡化时保留阴影；逐项登记32个上游新增提交。Java17双Release/API与APK/签名检查通过，版本仍0.3.1/106。无对应现成单测，未重复无关用例；实际刷新率、焦点外观和F3性能未验证，未操作设备。见[工作包](work/AERIAL-20261105-home-render-budget.md)。

- `AERIAL-20261104-legacy-update-diagnostics`：为Android5/6更新连接补充公开ISRG Root X1，保留系统信任/hostname校验和本仓库渠道；只读诊断兼容无cmd并修正组件/相邻IPv4脱敏。每flavor13项受影响测试、13项离线报告测试、Java17双Release与APK检查通过；真实旧系统/F3连接未验证，版本仍0.3.1/106。见[工作包](work/AERIAL-20261104-legacy-update-diagnostics.md)。

- `AERIAL-20261103-upstream-install`：逐项登记上游e8c251e..b09b4ed共58个提交，选择性适配安装结果/下载校验/构建失败、Android6/7通知监听器和T-Cast修复。43项离线测试通过，Java17双Release与APK契约通过；APK与0da7ad7检查点逐字节一致，设备行为未验证。见[工作包](work/AERIAL-20261103-upstream-install.md)。

- `AERIAL-20261102-desktop-independent`：桌面worktree与构建隔离；仅保留独立播放器v1接口，选择性适配T-Cast异常长度及Android5 ADB兼容修复。证据与未验证范围见[工作包](work/AERIAL-20261102-desktop-independent.md)。

- `AERIAL-20261028-f3-three-apps`：F3 / Android6上已安装并启动桌面standard 0.3.1/106、AetherLive 0.1.0/1、AetherCinema 0.1.0/1，保留现有配置与系统默认项。仅安装/启动范围通过，真实遥控器、播放/解码与性能仍未验证；见[设备记录](devices/dangbei-f3.md)。
- `AERIAL-20261028-nas-cinema`：已授权并启动独立NAS应用对话AetherCinema；基于固定Jellyfin源码重做Android TV UI并适配Emby/MoonTV；独立应用已获Java21授权，min23/JVM17。播放器Owner独占自身工作区的players/nas及NAS工作包；实际协议、手机与后端验收以其最新交接为准，桌面快照接口尚未集成。
- `AERIAL-20261027-modular-controls`：独立玻璃控件、遥控器编辑顺序/尺寸/显隐、网络/设置内页；个性化适度压暗、宽选项和手机卡片对齐，控制中心仅右侧渐变虚化。Java17双Release与Redmi L1通过；首版每flavor92测试，直播拆分工作树桌面各70测试，F3未验证。
- NAS独立播放器候选及只读桌面接口见[选型方案](NAS_PLAYER_PLAN.md)：优先Jellyfin的Java17兼容基线，Android6和实际媒体库尚待原型验证；当前上游JDK21不能直接采用。

- `AERIAL-20261027-lite-performance`：0.3.1 / 106，Lite缓冲与解码减负、静态玻璃/视频边缘光、图片字节限额及视口附近磁贴绘制缓存。双 Release、每 flavor 88 项单测和 Redmi L1 对照；静态/航拍/菜单 PSS 中位数约下降17%/15%/22%，F3降幅和流畅度未验证。
- `AERIAL-20261027-liquid-controls`：0.3.0 / 105，定向玻璃边缘、焦点伸展、独立菜单材质/明暗、溢出渐隐、应用菜单名称、四个可选独立控件及前台直播；双 Release、单测和 Redmi L1 验收，F3 L3 未验证。
- `AERIAL-20261027-upstream-network`：1.2.1 / 104 选择性适配上游二维码LAN/VPN选址修复；48项L0网络场景、双Release与APK契约通过，未安装设备，真实网络/手机/F3均未验证。其他上游条目继续由 `docs/UPSTREAM_SYNC.md` 逐项追踪。
- `AERIAL-20261026-liquid-glass`：1.2.0 / 103 动态玻璃和控制中心内的电视管家，双 Release、受影响单测及 Redmi K40S L1 已验收；当贝 F3 L3 仍未验证，详细证据见对应工作包。
- `AERIAL-20261026-desktop-personalization`：1.1.0 / 102 桌面定制与系统工具，双 Release 和 Redmi K40S L1 已验收；当前交付与限制见对应工作包。
- `AERIAL-20261025-dangbei-home`：迁移和首屏基线已在工作树中，真机仍未验证。
- `AERIAL-20261026-roadmap-phase2`：v0.1 品牌、Lite、手机配置和直播数据基线总包。
- `AERIAL-20261026-phone-acceptance`：Redmi K40S L1 验收与运行时修复，最新设备证据入口。
- 后续按独立工作包推进直播首页预览、NAS Dock 和 F3 L3；历史设备记录不能作为当前验收证据。

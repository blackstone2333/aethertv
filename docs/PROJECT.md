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
- 构建和测试必须使用 Java 17；Compose/Media3 依赖保持 Android 5/6 兼容线。
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
├── live/                           IPTV 数据、源配置、前台播放器、EPG 和播放回退
├── guard/                          开机复活、桌面守护和同步适配器
├── system/                         厂商能力检测和信号源适配
└── update/                         应用内升级
```

Roadmap 的新功能必须沿现有依赖方向接入：入口/状态 → 功能模块 → 系统适配。IPTV、NAS 和 Lite 先做独立边界，再由首页消费稳定的只读状态接口，不能把网络失败带入 `HomeViewModel.reload()` 或 HomeGuard 链路。

IPTV 由 `LiveSources` 保存源配置、`LiveFeedRepository` 负责网络/缓存回退，解析器保持纯数据边界。`LiveActivity` 是首页天线按钮与控制中心的前台直播入口，独占一个 Media3 会话，支持频道表、EPG、上下换台和有界切源。离开同步释放解码器，返回恢复航拍。首页静音预览和 NAS Dock 仍待后续工作包。

## 桌面定制与本地工具

- `AERIAL-20261026-desktop-personalization`：每排4–8个入口、航拍播放策略、素材分类偏好和受限局域网互传。
- `AERIAL-20261026-liquid-glass`：共享设计规范、同帧GPU视频玻璃和控制中心内的电视管家；详细证据与未验证边界见对应工作包。
- `system/SystemMonitor.kt` 只读系统状态；清理只移除应用可重建缓存。
- `system/TransferStore.kt` 使用应用私有 `files/exchange/`，单文件100MB、总量512MB、64项；文件访问通过随机ID，系统选择器负责本机导入/另存。PhoneServer 的二维码令牌保护全部网络入口。
- `docs/UPSTREAM_SYNC.md` 追踪选择性上游合入；每日维护避开活动开发，不整体覆盖定制。

## 版本规则

产品基线为 **0.3.0**。系统级更新、功能增改、普通优化分别递增第一、第二、第三位，高位递增后低位归零；构建次数不影响产品版本。Android 安装序号 `versionCode` 独立保持递增。早期 1.x 开发编号由该基线替代；当前 0.3.0 的安装序号为 105，保留覆盖升级能力。

## 当前工作前沿

- `AERIAL-20261027-liquid-controls`：0.3.0 / 105，定向玻璃边缘、焦点伸展、独立菜单材质/明暗、溢出渐隐、应用菜单名称、四个可选独立控件及前台直播；双 Release、单测和 Redmi L1 验收，F3 L3 未验证。
- `AERIAL-20261027-upstream-network`：1.2.1 / 104 选择性适配上游二维码LAN/VPN选址修复；48项L0网络场景、双Release与APK契约通过，未安装设备，真实网络/手机/F3均未验证。其他上游条目继续由 `docs/UPSTREAM_SYNC.md` 逐项追踪。
- `AERIAL-20261026-liquid-glass`：1.2.0 / 103 动态玻璃和控制中心内的电视管家，双 Release、受影响单测及 Redmi K40S L1 已验收；当贝 F3 L3 仍未验证，详细证据见对应工作包。
- `AERIAL-20261026-desktop-personalization`：1.1.0 / 102 桌面定制与系统工具，双 Release 和 Redmi K40S L1 已验收；当前交付与限制见对应工作包。
- `AERIAL-20261025-dangbei-home`：迁移和首屏基线已在工作树中，真机仍未验证。
- `AERIAL-20261026-roadmap-phase2`：v0.1 品牌、Lite、手机配置和直播数据基线总包。
- `AERIAL-20261026-phone-acceptance`：Redmi K40S L1 验收与运行时修复，最新设备证据入口。
- 后续按独立工作包推进直播首页预览、NAS Dock 和 F3 L3；历史设备记录不能作为当前验收证据。

# AERIAL-20261027-upstream-network

## Spec

- Owner：主模型，无委派；基线 `82ab060`。允许修改 `wallpaper/PhoneServer.kt`、其网络回归测试、版本元数据及 `docs/UPSTREAM_SYNC.md`、`docs/PROJECT.md`、`docs/ARCHITECTURE.md` 和本工作包。
- 选择性审查上游 `2d6d468..8356785`，继续追踪旧的部分提取/待合入条目。仅适配 `5142fc5` 的手机二维码局域网选址修复，避免 VPN/代理地址遮蔽 Wi-Fi 或以太网地址。
- 验收：物理局域网优先，多个物理网络中默认网络优先；拒绝回环、链路本地和代理测试网段，已知 VPN 不能作为兜底。无有效 IPv4 时返回不可用；不改变服务访问令牌、生命周期、权限或任何用户设置。
- Java17、白名单包名 `com.dangbei.tvlauncher`、namespace `app.orchard.tv`、minSdk21/target34、双 Release 与本仓库更新渠道保持。无需新增界面文字；五语言资源不变。
- 不安装/唤醒设备，不修改默认桌面、守护或无障碍，不发布正式 Release。F3 与手机上的真实 VPN 场景均须单独标记未验证。

## Plan

1. 核验干净工作树及已关闭工作包，fetch 并按行为评估上游。
2. 适配局域网选址，增加实际网络场景回归。
3. Java17 受影响测试、双 Release 构建、APK 和项目规则核验。
4. 记录选入/略过理由与未验证范围，提交并同步 GitHub。

## Status

complete：1.2.1-dangbei / 104 的选址修复代码已适配，L0回归与双 Release 通过；没有设备安装或真机验收。主模型集中实施，无委派。

## Evidence

- 已 fetch 至上游 `8356785`；本轮审查范围不代表整体已合入。
- 当前 `PhoneServer.lanAddress` 仅读取默认网络；`SystemProbe.network` 的 VPN 类型回退没有修复二维码选址，因此该修复尚缺失。
- 选入 `5142fc5f7578ba32f8630cdb29be83d6914b1fa8` 的网络地址修复；额外拒绝私网 VPN 兜底、未指定/组播地址。没有更改请求接口、服务监听生命周期、随机令牌或 VPN 配置。
- Java17：`./gradlew testStandardReleaseUnitTest --tests app.orchard.tv.wallpaper.PhoneServerLanAddressTest testGuardReleaseUnitTest --tests app.orchard.tv.wallpaper.PhoneServerLanAddressTest assembleStandardRelease assembleGuardRelease` 成功，最终运行14秒。两个 flavor 各24项通过（12个场景 × API23/34），共48项；包含 Wi-Fi/以太网覆盖默认 VPN、多物理网络优先级、缺少默认网络、能力缺失回退、IPv6/回环/链路本地/代理网段、私网 VPN 和混合 VPN/Wi-Fi 能力。
- 测试工具限制：本仓库 Robolectric4.17 不提供 API21，首次配置21的用例未能运行，随后改为23/34；构造组播 LinkAddress 会被 Android 框架拒绝，该不可创建的测试夹具已移除。产品代码不调用高于API21的新网络接口，APK最低版本保持21，但 Android5真机仍未验证。
- `aapt2` 核验两个 APK：白名单包名、版本104、1.2.1-dangbei（guard后缀）、minSdk21、target34、standard有HOME/guard无HOME、CONFIG使用白名单包名均正确。Java17项目规则及 diff 检查通过；无新增界面文字、依赖或权限。
- 本轮仅审查并同步代码；未连接、安装、唤醒或改变任何设备，不发布正式Release。真实网络/VPN可达性、手机和F3均未验证，103的旧手机证据不能转用到104。

## Exact next action

本工作包无待实施项。下一轮继续跟踪 `docs/UPSTREAM_SYNC.md` 的待办；在设备可用的独立验收中复核104的二维码LAN/VPN可达性，不复用103的真机结论。

## Deferred

- 其他上游条目及理由见 `docs/UPSTREAM_SYNC.md`；不能以最新审查 SHA 跳过待合入项。
- 真机 LAN/VPN、二维码可达性及 F3 专属行为未验证。

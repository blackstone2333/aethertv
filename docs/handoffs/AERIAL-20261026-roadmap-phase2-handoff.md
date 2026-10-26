# AERIAL-20261026-roadmap-phase2 handoff

Status: review
From: main model / integration owner
To: next AetherTV implementation session
Work unit: `AERIAL-20261026-roadmap-phase2`

## Current state

- v0.1 完成：可见品牌文案、Auto/Full/Lite、后台图片回收/恢复、手机控制台、IPTV 源配置/解析/缓存和数据层优先级规划。
- 最新证据以 [手机验收工作包](../work/AERIAL-20261026-phone-acceptance.md) 为准。Redmi 安装限制已解决，当前 USB 安装及无线启动均通过，不要再按旧安装阻塞排查。
- 未交付：Media3 IPTV 全屏播放、首页预览、NAS/MoonTV/Selene/OrionTV Dock、SMB/WebDAV/NFS、真实源质量与 F3 L3。

## Changed surfaces

- `live/model|parser|repo` 与单测，`PhoneServer`、Web 五语言、Lite 渲染/生命周期、品牌资源/控制中心、定制版更新来源、项目规则和安装说明。
- API：`api/live/state|source|source/delete|refresh`；`api/render-mode` 接受 `auto/full/lite`，状态返回 `renderMode/liteMode`。
- `LiveHttpFetcher` 的 HTTP 例外只用于配置的直播源；全局 cleartext 仍受限，HTTPS 证书检查保留，跨 origin 清除自定义头，禁止降级重定向。
- Home 和系统时钟屏保共用壁纸缓存；清理缓存时必须考虑 `AerialDream.isAttached`，不能在屏保用图时清空。
- 更新 manifest 为本仓库 `updates/version.txt`，下载本仓库 Releases 的 `aethertv-<flavor>.apk`；目前无公开 Release，manifest 不可提前提高。

## Evidence

- Java 17 双 flavor Release：成功；每个 flavor 41 项测试、0 失败。
- `aapt2`：`com.dangbei.tvlauncher` / `app.orchard.tv`，101，standard 有 HOME、guard 无 HOME，CONFIG 正确。
- 预检、Web JS/五语言表、安装/还原脚本语法及 diff 检查通过。
- Redmi K40S Android 14 standard：安装、运行、手机网页源保存/刷新、HTTP M3U/EPG.gz、离线及重新启动后的磁盘缓存、Lite 切换/持久化、后台恢复通过。
- 临时源、服务和转发已清理，设置恢复 Auto；截图与令牌未进仓库。
- F3 所有专属行为和脚本电视接管/还原流程仍未验证，见工作包 Deferred。

## Risks and authority

- 必须保持 `com.dangbei.tvlauncher`、`app.orchard.tv`、Java 17、minSdk 21。
- 不停用或卸载 `com.dangbei.leard.leradlauncher`；F3 不使用无障碍接管，不覆盖其他服务，不删除用户数据。
- 所有真实 URL、凭据、IP、令牌和签名密钥保留本机；搜索只针对具体项目目录。
- 安装指南使用显式包名/完整类名；旧上游文档中的相对组件写法不能直接套用。
- 回滚：逐提交回退本工作单元，保留用户私有配置、签名、包名与当贝安全边界；不通过卸载回滚。

## Exact next action

接管先读 AGENTS、PROJECT、目标工作包并检查 `git status --short --branch`。从用户试用反馈确定下一单元；优先独立 Media3 全屏直播和真实家庭源，然后 NAS 卡片。不要把 v0.1 当作 Roadmap 全部完成。

## Completion condition

当前 v0.1 的 L0/L1 已完成。后续单元各自定义可观察验收；F3 HOME/黑屏/遥控器/HDMI/音量/多任务/待机/硬解码只凭目标 Release 实机证据关闭。

# AERIAL-20261028-nas-cinema

Status: active
Owner: AetherCinema · Jellyfin / Emby 影音 (`01a17e1d-7997-7bb0-9971-34e978823f0e`, local)

## Spec

- 用户明确要求另开对话，开始基于Jellyfin制作新UI的独立Android TV应用，并适配Emby后端；优先级较后的目标是MoonTV与飞牛视频后端。
- 暂用产品名AetherCinema，独立APK/进程，applicationId暂定`app.aethertv.cinema`，namespace沿用`app.orchard.tv`，初始版本`0.1.0 / 1`。桌面的`com.dangbei.tvlauncher`、namespace、版本、HOME与守护保持不变。
- 以Jellyfin Android TV为真实源码基线：记录仓库、固定commit、采用路径和修改，保留GPL及第三方归属。当前最新版要求JDK21，先选择并验证可维护的Java17兼容历史基线；不能只做一个空壳或API草图就声称已基于Jellyfin完成。
- Java17构建；目标兼容Android6 / minSdk23，桌面自身继续minSdk21。不要通过抬高桌面最低版本或升级到Java21规避问题。兼容基线若有具体障碍，先保留可运行成果和明确诊断，再提出取舍。
- TV优先：遥控器方向导航、稳定焦点、Back返回来源、海报墙、继续观看、影片详情、搜索、账号/服务端管理、播放器字幕/音轨和续播。参照Apple公开tvOS设计原则与本项目DESIGN_SYSTEM，不使用Apple受限资源，不把触屏或鼠标当主要入口。
- Jellyfin与Emby分别实现后端适配，覆盖认证、目录、详情、图片鉴权、播放协商、续播与进度上报；不能只换URL或移除版本检查就宣称Emby兼容。
- MoonTV名称可能对应多个项目或分支，先确认目标仓库/协议版本；飞牛视频先核查实际服务版本和可用接口。采用独立provider/capability边界，不假设它们使用Emby协议。协议调研可与核心Jellyfin/Emby实现并行推进；无法核实时保留明确待接入状态，不拖延核心首版。
- 账号、凭据、图片鉴权、库扫描和解码归独立应用；桌面仅消费有界只读内容快照与版本化播放入口。NAS不向桌面交付密码、令牌或任意原始播放URL。
- 性能：海报懒加载与有界图片缓存，空闲无持续动画，低成本玻璃/Lite表现，仅播放页持有解码器，离开及时释放；分别记录浏览、播放和后台PSS，不能将手机数据推算成F3。
- 不更改默认HOME、系统应用、无障碍或用户媒体；不把真实NAS地址、凭据、截图令牌提交公开仓库。未收到正式发布要求不创建正式Release。

## Ownership

- NAS Owner可修改：`players/nas/**`、本工作包、对应NAS交接包。工程先作为`players/nas`中的独立Gradle根目录构建，避免改共享构建脚本；此目录内新文案按项目五语言规则维护。
- NAS桌面契约先写入`players/nas/docs/DESKTOP_CONTRACT.md`，应用端可按提案实现自己的Provider/Intent入口；父对话批准并集成后才修改桌面与共享contracts。
- 不修改：`app/**`、`players/live/**`、现有`contracts/**`、仓库根Gradle/版本目录/设置、其他Owner工作包或用户数据。确需共享改动，先在本包记录精确提案，父对话处理。
- 不reset/clean/checkout覆盖当前脏树；不全量git add、不更换共享分支、不提交/推送其他Owner内容。初期由父对话统一集成提交，不把构建成功等同于全项目可发布。

## Plan

1. 阅读交接和源码，核查Jellyfin历史基线、许可证、Java17与API23依赖，确定独立构建方式；记录来源和基线取舍。
2. 搭出真实可运行Android TV应用，完成新的海报墙/详情/设置/播放器UI；将Jellyfin与Emby协议差异放入独立provider实现。
3. 实现并测试登录→库→详情→播放→进度/续播闭环；并行核实MoonTV与飞牛视频协议，逐个提供真实适配或清晰待接入结论。
4. 定义受控海报/继续观看快照与PLAY_ITEM接口；先完成独立应用端和契约提案，桌面代码由父对话集成。
5. Java17 Release、协议测试、APK检查、模拟器/手机验证和内存测量；更新证据与下一步，不提前发布。

## Status

独立对话已创建并启动，按本包接管和实施。创建时尚无NAS源码、APK或任何后端验证证据；后续状态由该Owner维护。

## Evidence

- 选型依据见`docs/NAS_PLAYER_PLAN.md`，已确认当前Jellyfin TV版本目录minSdk23、JDK21；Java17兼容历史基线尚未选定。
- 共享仓库位于`codex/aerial/aerial-20261027-lite-performance`，桌面检查点`1c50d16`已同步GitHub；大量未提交差异属于独立IPTV拆分，不能覆盖。
- 无用户Jellyfin/Emby/MoonTV/飞牛视频服务器的本轮测试证据。可以先用协议fixture及临时测试后端验证，实际服务验收需真实版本和用户配置；不得伪造连接、鉴权或播放结论。

## Acceptance

- 独立Release APK可由Android TV启动入口打开，无HOME、自启、无障碍接管或桌面运行依赖；Java17、包名、minSdk与签名检查通过。
- Jellyfin及Emby各有可重复认证、目录、详情、播放、字幕/音轨、续播与进度上报验证；失败、过期令牌和断网不会阻塞UI。测试后端与用户真实服务的证据分开记录。
- TV焦点有清晰反馈，进入/返回海报行保留位置，设置与播放器共用视觉语言；测试视频播放后退出释放解码器。
- MoonTV/飞牛视频分别记录目标项目/服务版本、可用接口、已实现能力、真实验证范围与缺口；仅UI入口或mock不算后端接入完成。
- 桌面联动先验独立应用端受控快照与指定条目启动；桌面实际推送联动由父对话集成后验收。
- F3安装限制、真实遥控器、MStar硬解码、长时播放与性能没有Release真机证据时仍写“未验证”。

## Exact next action

由新NAS对话读取交接、检查脏树，选择Java17兼容Jellyfin源码基线，开始独立工程与Jellyfin/Emby真实闭环实现。不要停在调研或计划。

## Deferred

- 真实服务地址/账号缺失时，家庭媒体库端到端验收待用户配置；继续推进源码、fixture和UI。
- 未核实的MoonTV分支、飞牛私有协议能力；F3 Release真机验收；正式发布与桌面共享文件集成。

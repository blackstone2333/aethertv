# AERIAL-20261028-nas-cinema

Status: review
Owner: AetherCinema · Jellyfin / Emby 影音 (`01a17e1d-7997-7bb0-9971-34e978823f0e`, local)

## 2026-11-02 当前任务：开发环境隔离

- 当前唯一执行范围是独立工作区、构建入口和可交接检查点；不继续扩展后端/UI，不连接真实服务、不操作设备、不发布正式Release。
- 从 `5edcc5bd2b57a9061a4478a682f4903d1369790b` 建立managed worktree：`/Users/black/.codex/worktrees/aethercinema-independent/AetherTV`，分支 `codex/aerial/aerial-20261102-nas-independent`。后续NAS修改只在该工作区进行。
- 只迁入56个公开NAS源码文件及本工作包/交接；忽略的构建目录、缓存、运行时、local配置、凭据和生成测试数据不复制。保留许可证及归属，不携带FileProvider纯mode差异；原协作目录内容/索引/分支不变。
- 允许修改并单独提交推送：`players/nas/**`、本工作包、NAS交接。旧文中“父对话统一提交”属于原共享目录阶段，已由本轮独立分支授权取代；不改桌面、Live、shared contracts、其他docs或根构建。
- Java21仅本独立应用获准；继续minSdk23/JVM17，桌面Java17/minSdk21不变。桌面有独立工作区，不加载NAS构建、不等待本分支。
- 共享接口仅在 `players/nas/docs` 提案，应用端既有快照/PLAY_ITEM保留，桌面尚未集成部分继续待办。
- 开发环境隔离验证：complete。新工作区下载并校验专用Temurin21.0.13+9；没有从原目录复制运行时或私有配置。`env -u AETHER_MOON_SERVER_FILE -u AETHER_MOON_AUTH_FILE JAVA_HOME=<本工作区专用JDK21> ANDROID_HOME=<本机SDK> players/nas/tools/build.sh --offline -PcinemaProbe :probe:assembleRelease`退出0（1m47s，105项任务），只加载独立app/probe模块；21项测试通过、1项真实服务测试跳过，Release/R8/lint及probe编译通过。
- 新构建APK包名/版本/min23/target34、v1/v2签名、JVM17字节码及GPL/第三方资产核验通过；大小2,231,210字节，SHA256 `0625ccf1f1caea54f69290c95a09ed482b0eac6c248874f92b3b8d59ca5793e0`。它是本轮重建产物，未安装，不使用历史APK哈希代替当前产物。
- 原协作目录分支、Git状态、原NAS源码及两份文档字节复核未变，原索引无暂存修改；迁移的实现/构建/许可证内容保持原样，仅新增开发边界文档和更新自己的交接记录。没有后端/UI功能变化。
- 历史L0/L1证据保留原日期。本基线的 `docs/devices/dangbei-f3.md` 另有2026-10-28首次安装/静态启动记录；该记录不证明真实播放、遥控器、MStar或性能，本轮没有设备操作。

## Spec

- 用户明确要求另开对话，开始基于Jellyfin制作新UI的独立Android TV应用，并适配Emby后端；优先级较后的目标是MoonTV与飞牛视频后端。
- 暂用产品名AetherCinema，独立APK/进程，applicationId暂定`app.aethertv.cinema`，namespace沿用`app.orchard.tv`，初始版本`0.1.0 / 1`。桌面的`com.dangbei.tvlauncher`、namespace、版本、HOME与守护保持不变。
- 以Jellyfin Android TV为真实源码基线：固定v0.19.10 / `984181a3d6ab14e9a6d2dcc850c582e1c138bd95`，保留采用路径、修改、GPL及第三方归属。用户已授权独立应用使用Java21；历史Java17候选仅作选型记录。实际移植播放协商、设备配置、会话上报和时间格式化逻辑，不把空壳或API草图当成采用上游完成。
- Java21构建（用户2026-10-28授权独立NAS应用）；目标兼容Android6 / minSdk23，桌面自身继续minSdk21。不要通过抬高桌面最低版本或升级到Java21规避问题。兼容基线若有具体障碍，先保留可运行成果和明确诊断，再提出取舍。
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
- 不reset/clean/checkout覆盖原共享脏树；不全量git add、不更换原共享分支、不提交/推送其他Owner内容。本轮只从自己的新工作区精确暂存并提交推送NAS文件，不把构建成功等同于全项目可发布。

## Plan

1. 已核查并固定Jellyfin源码与许可证，独立Java21构建、API23兼容逻辑测试通过；桌面构建保持原样。
2. 已实现独立TV界面及Jellyfin、Emby各自的provider；焦点/返回、海报、详情、搜索、账号、轨道选择和续播进入手机验证。
3. Jellyfin/Emby协议fixture闭环和真实MoonTV短播放/回写/冷启动续播已有L1证据。飞牛按用户要求跳过。
4. 独立应用端快照和PLAY_ITEM实现及权限probe已验证；父对话负责桌面消费端集成。
5. 已生成、签验并安装Release测试包，记录内存、清理自建测试数据并交付review；F3和实际家庭Jellyfin/Emby完整播放保留待验收。

## Status

首版独立测试包于2026-10-28生成并在Redmi K40S / Android14完成L1验证，版本`0.1.0 / 1`，包名`app.aethertv.cinema`。2026-11-02仅完成开发环境隔离和独立重建，当前产品验收仍为review，不是正式发布或F3完整验收声明。账号加密、网络/海报/页面/快照有界，仅前台播放器持有解码器；离开释放。修改范围保持`players/nas`与本工作包/交接包，无其他Owner代码。

## Evidence

- 来源、精确采用路径和依赖归属：`players/nas/docs/UPSTREAM.md`、`THIRD_PARTY_NOTICES.md`；GPL和102项运行时依赖归属随包提供。采用上游v0.19.10，JDK21构建，JVM17字节码，minSdk23。
- `players/nas/tools/build.sh -PcinemaProbe :probe:assembleRelease`退出0：Release/R8/资源收缩/lint通过。正常最终测试21通过、1项真实服务测试按默认跳过；此前启用真实MoonTV配置时22项API23测试全部通过。五语言47项资源键一致。
- APK元数据与v1/v2签名验证退出0；手机安装后SHA256与最终产物一致：`c26576568676926c7f6d1ecab0e1f5caa656315e3ea428d1062580880aa6fbf5`。APK为2,231,198字节，使用开发测试签名。
- Redmi K40S Release L1：Jellyfin和Emby自建fixture分别登录、浏览、详情、原生首帧、暂停/返回及非零续播通过；Emby生成HLS路由只证明该播放路径，不能代替真实服务转码。用户自行录入的Emby仅验证真实浏览，保留账号，不声称已测真实播放。
- 真实MoonTV：登录、搜索来源区分、剧集、详情、实际HLS首帧、26秒进度回写、冷启动继续卡及非零续播通过。测试记录创建前为空，完成后只按记录key删除该项并确认不存在；其他观看记录不改。
- 同签名probe可读取2张fixture快照及受控图、从10秒指定播放；无权限的查询和播放入口均拒绝。桌面消费端仍归父Owner。
- 最终单次PSS：MoonTV播放118.3MiB、fixture详情浏览148.6MiB、即时后台65.5MiB；有播放器Release日志。非中位数、非受控性能对照，不外推F3。
- 详细证据与边界：`players/nas/docs/ACCEPTANCE.md`；私有服务、账号、设备和截图只存忽略的`local/`。共享脏树属于其他Owner的修改保持原样。

## Acceptance

- 独立Release APK可由Android TV启动入口打开，无HOME、自启、无障碍接管或桌面运行依赖；Java21、包名、minSdk与签名检查通过。
- Jellyfin及Emby各有可重复认证、目录、详情、播放、字幕/音轨、续播与进度上报验证；失败、过期令牌和断网不会阻塞UI。测试后端与用户真实服务的证据分开记录。
- TV焦点有清晰反馈，进入/返回海报行保留位置，设置与播放器共用视觉语言；测试视频播放后退出释放解码器。
- MoonTV/飞牛视频分别记录目标项目/服务版本、可用接口、已实现能力、真实验证范围与缺口；仅UI入口或mock不算后端接入完成。
- 桌面联动先验独立应用端受控快照与指定条目启动；桌面实际推送联动由父对话集成后验收。
- F3安装限制、真实遥控器、MStar硬解码、长时播放与性能没有Release真机证据时仍写“未验证”。

## Exact next action

本轮隔离和构建验证已完成，以 `codex/aerial/aerial-20261102-nas-independent` 的独立检查点交接，随后停止。未来获得新任务后先读`players/nas/AGENTS.md`、`players/nas/docs/DEVELOPMENT.md`和验收记录；不自动启动真实服务/电视验收或桌面集成。桌面不等待本分支。

## Deferred

- 实际家庭Jellyfin播放及Emby完整播放/转码/版本范围；Android6设备UI/解码、F3安装策略/真实遥控器/MStar/长时播放/性能：未验证。
- 用户指定MoonTV仓库当前404；使用授权部署观察到的协议和前端指纹，不替换分支，不承诺全部来源可用。
- 飞牛服务端0.8.41按用户明确要求延后；桌面共享文件集成、正式发布均不在本轮交付范围。

## 2026-10-28 用户约束更新

用户明确允许独立 AetherCinema 使用 Java 21。此授权仅覆盖 `players/nas`，桌面保持 Java 17，Android 6 / minSdk23 和其他所有权边界不变。较新 Jellyfin 基线将按实际依赖兼容性核查并固定提交；历史 Java17 基线 v0.16.11 已核实但不再构成必须采用的限制。

## 2026-10-28 用户后端补充

- MoonTV 目标仓库为 `mooncloudy/moontv`；公开仓库当前返回404。用户提供已部署局域网实例和账号，私有资料仅保存在 `players/nas/local/`。部署的登录、搜索、详情、收藏和观看记录接口已授权核查；Java21下22项测试（含真实MoonTV只读测试）通过。
- 飞牛影视客户端0.9.8、服务端0.8.41来自用户截图；用户明确要求本轮先跳过，后续另给服务，当前不实现或宣称接入。

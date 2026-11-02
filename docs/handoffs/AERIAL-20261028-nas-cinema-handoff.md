# AetherCinema 独立影音应用交接

Status: review
Owner: AetherCinema · Jellyfin / Emby 影音 (`01a17e1d-7997-7bb0-9971-34e978823f0e`, local)
Updated: 2026-11-02

## 当前执行环境与权限（2026-11-02）

本轮只隔离开发环境和提交可交接检查点，不扩展后端/UI、不连接真实后端或设备、不清理原数据、不发布正式Release。以下2026-10-28的安装/服务证据是历史记录，不授权本轮重跑。NAS独立应用继续JDK21/minSdk23/JVM17；桌面继续Java17/minSdk21。

- 基线：`5edcc5bd2b57a9061a4478a682f4903d1369790b`。
- managed worktree：`/Users/black/.codex/worktrees/aethercinema-independent/AetherTV`。
- 分支：`codex/aerial/aerial-20261102-nas-independent`；桌面已迁到自己的工作区/分支，双方构建和提交互不阻塞。
- 迁入56个公开NAS源码文件及本NAS工作包/交接。没有复制桌面/Live实现、shared contracts、根构建差异、build、缓存、local、账号、地址、签名密钥或生成测试数据。复制按内容写入，文本0644，仅实际命令脚本可执行。
- 原协作目录保持原样。今后NAS只在本工作区修改 `players/nas/**`、本NAS工作包及本交接；本分支允许精确暂存、提交和推送这些文件。旧共享目录阶段的禁止提交约束不适用于本次已授权检查点。
- 新环境说明：`players/nas/AGENTS.md`、`players/nas/docs/DEVELOPMENT.md`。共享接口变更只在NAS docs提案，由桌面Owner接入；桌面消费端尚未集成内容仍为待办。
- 本轮独立构建：专用Temurin21.0.13+9，显式取消两项MoonTV测试环境变量，提供SDK36位置，从本工作区执行 `players/nas/tools/build.sh --offline -PcinemaProbe :probe:assembleRelease`；退出0，1m47s，105项任务只涉及独立app/probe，21项通过、1项真实服务测试跳过，Release/R8/lint和probe编译通过。
- 本轮新APK元数据、v1/v2签名、JVM17类版本61、GPL/第三方资产检查通过；大小2,231,210字节，SHA256 `0625ccf1f1caea54f69290c95a09ed482b0eac6c248874f92b3b8d59ca5793e0`。未安装、未验证新包设备行为，不混用下方历史哈希。
- 已复核原目录分支/Git状态、原NAS源码和两份文档字节保持不变，原索引无暂存修改；新分支相对基线仅有NAS文件与自己的工作包/交接。实现/构建/许可证内容未改，文本权限规范化，无FileProvider纯mode改动。
- 交接检查点为 `origin/codex/aerial/aerial-20261102-nas-independent` 分支；只提交此Owner范围，不发布正式Release。获取该分支后查看 `git log -1` 和本交接即可恢复。
- 基线已有 `docs/devices/dangbei-f3.md` 中2026-10-28的AetherCinema首次安装/静态启动证据；它不扩大真实播放/遥控器/MStar/性能验收，本轮未复测。

## 历史交付（2026-10-28）

独立Release测试包 `players/nas/app/build/outputs/apk/release/app-release.apk`，`app.aethertv.cinema`，`0.1.0 / 1`，minSdk23 / target34。Java21由用户明确授权，仅覆盖独立应用；桌面仍按Java17约束。使用开发测试签名，未正式发布、提交或推送。

SHA256：`c26576568676926c7f6d1ecab0e1f5caa656315e3ea428d1062580880aa6fbf5`，大小2,231,198字节。Redmi K40S中安装的APK已核对同一哈希。完整验收见 `players/nas/docs/ACCEPTANCE.md`；权威工作包为 `docs/work/AERIAL-20261028-nas-cinema.md`。

## 已实现与已验范围

- 固定Jellyfin Android TV v0.19.10 / `984181a3d6ab14e9a6d2dcc850c582e1c138bd95`，实际移植播放协商、保守设备配置、会话上报和时间格式逻辑；原始采用文件及贡献者归属在 `upstream/reference/`，GPL与102项运行时依赖归属随APK提供。并非编译整套上游原UI；具体修改映射见 `docs/UPSTREAM.md`。
- 独立Compose海报墙、继续观看、详情、搜索、账号和播放器；960dp画布，五语言文案，Pressable焦点与Back来源恢复。账号令牌采用Android Keystore AES-GCM，不保存密码。
- Jellyfin和Emby各自provider实现认证、目录、图片鉴权、PlaybackInfo、直接播放/重封装/转码路径及进度/停止上报；非UUID ID保留原值，协商URL受服务源约束。API23协议测试覆盖失败/过期/大小限制/来源拒绝等。
- 网络超时、页面、海报缓存与快照有界；单个前台Media3解码器，播放器在暂停/离开释放，后台丢弃界面和海报引用。
- Redmi K40S / Android14，Release L1：Jellyfin和Emby的自建fixture登录→浏览→详情→真实AVC/AAC首帧→返回→非零续播通过，Jellyfin外部字幕选择与Emby生成HLS路径有证据。生成HLS不证明真实服务转码。
- 用户自行填入的Emby已观察到真实视图、海报与继续观看，仅浏览验证；未借此宣称真实播放通过，账号保留。
- 用户指定MoonTV仓库 `https://github.com/mooncloudy/moontv` 当前404，未擅自替换。对其授权运行实例完成独立Cookie API适配。实际L1验证登录、搜索来源标记、剧集、详情、HLS首帧、26秒记录回写、冷启动继续卡及非零续播；音轨来自真实流。协议和前端指纹见 `docs/PROVIDERS.md`，不承诺全部来源或服务升级后持续兼容。
- 应用端只读快照/受控图片和受保护PLAY_ITEM已实现。同签名临时probe读出2张fixture卡和有界图，从10秒发起播放并推进到30秒；无权限shell查询/直接调用均被拒绝。桌面消费端代码与返回焦点由父Owner负责。
- 最终单次PSS：真实MoonTV播放118.3MiB、fixture详情浏览148.6MiB、即时后台65.5MiB。日志有Player Release。不是中位数或受控对照，不能推算F3。

## 命令与结果

在仓库根执行：

```sh
players/nas/tools/build.sh
players/nas/tools/build.sh -PcinemaProbe :probe:assembleRelease
```

最终完整构建退出0，Release/R8/资源收缩/lint通过；正常运行21项测试通过、1项真实服务测试默认跳过。此前给定本地授权配置的显式运行22项全部通过。`tools/build.sh`检测JDK21；不修改根工程JAVA_HOME或Gradle设置。SDK与可选下载的JDK都在忽略路径。

真实MoonTV只读测试可显式启用：

```sh
AETHER_MOON_SERVER_FILE="$PWD/players/nas/local/moontv-server.txt" \
AETHER_MOON_AUTH_FILE="$PWD/players/nas/local/moontv-auth.json" \
players/nas/tools/build.sh
```

最终APK `aapt2 dump badging`核验包名、版本、minSdk23和target34；`apksigner verify --verbose`退出0，v1/v2有效。手机覆盖安装与 `cmd package compile -m speed -f app.aethertv.cinema` 成功。日志/截图/服务信息都在忽略的 `players/nas/local/acceptance/`，不得写入公开文档。

## 所有权与操作限制

NAS只修改 `players/nas/**`、本NAS工作包和本交接。未动桌面 `app/**`、`players/live/**`、共享 `contracts/**` 或根构建文件。原共享仓库大量脏树属于其他Owner，不reset/clean/换分支/全量add；2026-11-02的提交推送只在独立NAS工作区执行，不修改原目录。

`docs/DESKTOP_CONTRACT.md`是独立应用端契约提案：signature权限 `app.aethertv.cinema.permission.READ_SNAPSHOT`，cards最多24、图片192×288，播放仅传账号/条目opaque ID与位置。父Owner批准并集成前，不修改桌面或共享契约。没有HOME、自启、无障碍或守护接管。

设备为共享手机，操作前检查 `local/coordination/*-device-*`；只在独占NAS锁内测试。使用明确USB序列号，地址/序号在本地设备记录中。只清理自建fixture账号、probe、反向端口和已记下的测试记录，不碰用户Emby、原厂桌面或其他应用。

## 清理与恢复

MoonTV测试条目在创建前确认无既有记录，只按记录key删除，DELETE200并重读确认不存在。移除自建fixture账号及此次MoonTV本地测试检查点后，重新登录MoonTV保留可用连接。用户Emby保持原样。临时probe/反向端口移除，桌面恢复前台，NAS独占锁释放；详细最终本地设备说明见 `local/coordination/nas-device-released`。

重建不需要删数据；测试安装用同签名覆盖，不卸载正式应用。若真实流失败，回到来源或账号页，保留已有账号，不清空用户媒体或全部观看记录。

## Exact next action

本轮以独立分支检查点交接后停止，桌面不等待NAS。后续先读 `players/nas/AGENTS.md`、`players/nas/docs/DEVELOPMENT.md`、`players/nas/docs/ACCEPTANCE.md` 和 `players/nas/docs/DESKTOP_CONTRACT.md`；只有新增明确任务后才继续功能开发、真实后端/设备验收或桌面集成，不把应用端probe当成桌面集成已验。

## Deferred 与完成条件

- 用户明确要求飞牛先跳过，稍后另给服务。截图仅确认客户端0.9.8/服务端0.8.41；当前未接入，不复用MoonTV凭据。
- Android6物理/模拟器UI及解码、当贝F3安装策略、真实遥控器、MStar、长时播放和性能：未验证。API23 Robolectric仅是L0。
- 实际家庭Jellyfin播放、Emby完整播放/转码与版本范围：未验证。不同MoonTV来源有403/404，不保证全部可播。
- 电视与实际服务验证、桌面消费端集成及正式发布另行验收。当前交付止于可安装首版测试包和明确L0/L1证据，不能标记全部后端/F3完成。

# AERIAL-20261115-upstream-review

Status: complete
Owner: desktop conversation (main model)
Branch: `codex/aerial/aerial-20261102-desktop-independent`

## Spec

- Fetch and assess every commit after the 2026-11-12 ledger boundary (`31deac69`) through upstream `50ba53c` (v1.95–v1.98).
- Select only changes that fit the desktop worktree, Java 17, Android 5/6 compatibility, fixed package/namespace, local update channel, free layout, glass/control cards, system tools, aerial preferences and independent-player contract.
- Preserve AetherTV behavior and user settings when an upstream patch conflicts. Do not import upstream release numbers, Lite/player modules, device claims, HOME/accessibility takeover changes or site documents.
- No shared-device operations, factory-app changes, user-data changes or formal Release.

## Result

The fetch advanced `upstream/main` from `31deac69edc56de9d773c208787b939e2c54a76d` to `50ba53c98731fcc40711037225132eecce1a37cb`, adding 62 commits and tags v1.95–v1.98. Four bounded fixes were adapted:

- `84ea832`: update downloads now resume a partial `.part` file up to three times when the source supports Range, and safely restart when it does not.
- `5d8c59f`: settings and network candidates are filtered for exported/enabled access and held permissions before selection.
- `ae97b24`: opening the local Inputs page refreshes source state so stale cable/CEC state is not shown.
- `09c5c7b`: weather diagnostics expose result/HTTP failure kind and in-flight state without location data.

The upstream confirmation-page fallback (`86fb3d6`) was not copied because the local updater intentionally removed PackageInstaller sessions and uses ACTION_VIEW after package/signature checks. Guard, HOME, vendor-input, vendor-settings, brightness, Lite, 4K canvas and release/documentation changes remain deferred or skipped for the boundaries recorded below. Product version remains `0.3.1 / 106`; no player source or root module changed.

## Verification

- `git diff --check`: pass.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 tools/check-project-rules.sh`: pass.
- Standard debug focused unit tests (`InputRoutingTest`, `XgimiSettingsTest`, `ControlLayoutTest`): pass.
- Standard and Guard release focused unit tests (`InputRoutingTest`, `XgimiSettingsTest`): pass.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease`: pass; both release lint gates ran.
- APK contract/signature check: pass. Both APKs keep `com.dangbei.tvlauncher`, `app.orchard.tv.MainActivity`, minSdk 21, targetSdk 34, versionCode 106, standard HOME versus guard no-HOME, matching signing certificates and no embedded player classes. Evidence is in ignored `local/upstream-review-20261115/`.
- No F3 or shared device was connected. HOME, accessibility, factory apps, volume, user data and player worktrees were not touched. Device behavior remains 未验证.

## Per-commit decisions

| 上游提交 | 主题 | 本轮处理 |
|---|---|---|
| `59bfc38` | Guard: a profile's filterHomeAlways takes Home with Aerial the default too (#1462) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `86fb3d6` | Update: a confirmation page that dies at once falls back to the installer (#1482) | 暂缓：本地更新器走签名校验后的 ACTION_VIEW，不引入上游 PackageInstaller session 语义；需另开更新链工作包。 |
| `84ea832` | Update: a download broken off part way resumes from there (#1480) | 选入：更新下载保留 `.part` 并使用 HTTP Range 断点续传；保留本地更新渠道、包名/签名/版本校验。 |
| `8d33008` | Inputs: JMGO H931's MediaTek ports through its TV app (#1481) | 暂缓：厂商信源路由需能力检测与对应电视回归；不以芯片或上游报告推断 F3。 |
| `01695bf` | open_source and selftest: as the tile does on XGIMI DOHKO (#1483) | 暂缓：厂商信源路由需能力检测与对应电视回归；不以芯片或上游报告推断 F3。 |
| `d803066` | UhdCanvas: Hisense's vendor.display-size counts for the panel (#1461) | 略过：本地定制没有该上游 4K 画布模块，不能孤立加入；目标电视证据另行处理。 |
| `06339f1` | Inputs key: Tianci firmware answers it with its own source page (#1468) | 略过：背景性或合并提交；按其包含的实际行为在对应专项工作包中继续追踪。 |
| `0ec4bc6` | Docs: model rows and notes from the 2026-11-12 pass | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `addc408` | agent.md: USB A-to-A for MT9655, push with the TV on its home screen (#1473) | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `01664f0` | HANDOFF, ARCHITECTURE: the 2026-11-12 pass | 略过：背景性或合并提交；按其包含的实际行为在对应专项工作包中继续追踪。 |
| `e519839` | HANDOFF: the 2026-11-12 pass checked on the Xiaomi | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `2bc2c3e` | 1.95 (135) | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `a0f4b59` | HANDOFF: 1.95 released, both TVs checked, site deployed | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `4420d05` | Guard: a kept volume press shows the bar, and held it steps on (#1499 #1498) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `8e7f7d6` | Guard: Jamdeo's GlobalPanel on Android 6 VIDAA too (#1505) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `14459d2` | Guard: leave Jamdeo's panel up while its launcher was opened on purpose (#1502) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `5d8c59f` | Settings: skip candidates Aerial may not start (#1388) | 选入：设置/网络候选要求导出且权限可用，避免锁定的厂商页遮蔽可用回退；保留本地内页和候选顺序。 |
| `c94ba84` | Guard: a stock launcher opened on purpose stays allowed across a revival (#1491) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `35c8a29` | Lite: skip settings candidates it may not start (#1388) | 略过：桌面根 Gradle 只加载 `:app`，不引入上游 Lite 模块或其版本线。 |
| `b0a5fb6` | Docs: README rows (#1462 #1468 #1493 #1495), VIDAA, Tencent box and Haier notes | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `224ef96` | HANDOFF: the 2026-11-13 pass | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `39704b9` | ARCHITECTURE: kept volume presses, stock launcher visit on disk (#1498 #1499 #1491) | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `0022bc2` | 1.96 (137), Lite 1.29 (31) | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `030a77f` | HANDOFF: 1.96 released, both TVs checked, site deployed | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `904cdd3` | HANDOFF: TCL's slow Home after install.sh, what was found and what was not | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `e688701` | Guard: answer the Home chooser with Back so Aerial returns at once (#44) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `9e95dec` | Guard: remember per firmware that kept volume presses' lifts arrive (#1521) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `4aa07eb` | Guard: open vendor recent-apps activities where SystemUI has none (#1463 #1525) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `51f9d84` | Home: a Home start with no action naming Aerial is Home too (#1508) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `461ec47` | Settings: LeTV's own settings before AOSP's empty page (#1528) | 暂缓：本轮只吸收可启动性保护；其他厂商设置 URI/组件需能力检测并保留本地层级。 |
| `44285e6` | Inputs: YunOS TVSource's source list when nothing answers passthrough (#1527) | 暂缓：厂商信源路由需能力检测与对应电视回归；不以芯片或上游报告推断 F3。 |
| `22aab1f` | Devices: Redmi A Pro 75 (MiTV-ASTR0) has two HDMI ports, not four (#1517) | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `99987c6` | Diagnostics: the weather's state, without the place (#1506) | 略过：背景性或合并提交；按其包含的实际行为在对应专项工作包中继续追踪。 |
| `017453c` | README: ChangHong mst838, Konka 551E, Redmi A Pro 75 inputs, BF1 brightness (#1531 #1504 #1517 #1355) | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `f39d584` | Device notes: LeTV Max4, Dangbei DBX5 Ultra, XGIMI chooser PR, Skyworth Home, TCL T7L, Aurora 5S, VIDAA MT9653 | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `d10a75f` | ARCHITECTURE: chooser Back (PR #1523), kept volume lifts on disk, Home by component, recents order, weather in the report | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `0e6d2c9` | HANDOFF: the 2026-11-14 pass | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `594cb32` | HANDOFF: count of closed items | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `c21c8f3` | Guard: put the enabled service back 1.5 s after the firmware drops it (#1377) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `f92bef6` | Guard: the enabled-list watch is XGIMI's (H3S), not JMGO's, in its comments (PR #1534) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `4b62a47` | README: Skyworth 8S26 14U, Lite only as an app (#1529) | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `72b1b7e` | HANDOFF: PR #1534 and the 141 test package | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `cd32240` | 1.97 (142), Lite 1.30 (32) | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `1002090` | HANDOFF: 1.97 released, both TVs checked, site deployed | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `32745d0` | Guard: ADB off at a wake the revived guard did not see is the firmware's (#1537) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `4949579` | Guard: Dangbei's ChildRecentActivity is the watch history, not recents (#1525) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `996d99f` | Inputs: Mi TV 5 Pro (ANSP0) numbers its HDMI ports as the Mi TV 4 does from 22.8 (#1487) | 暂缓：厂商信源路由需能力检测与对应电视回归；不以芯片或上游报告推断 F3。 |
| `43c7784` | Guard: out of the list at Dangbei's STR_SHUTDOWN, before the firmware stops Aerial (#1497) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `de09050` | Settings: Huawei smart screens open theirs by tvsetting:// link (#1394) | 暂缓：本轮只吸收可启动性保护；其他厂商设置 URI/组件需能力检测并保留本地层级。 |
| `594bbb5` | Inputs: Dangbei's Huanglong projectors switch through their TV app's SourceName (#1477) | 暂缓：厂商信源路由需能力检测与对应电视回归；不以芯片或上游报告推断 F3。 |
| `6a939d1` | Inputs: XGIMI on Android 14 opens the HDMI port asked for, not the picker (#1483) | 暂缓：厂商信源路由需能力检测与对应电视回归；不以芯片或上游报告推断 F3。 |
| `ae97b24` | Control Center: read the inputs again as it opens (#1483) | 选入：信源页进入时重新读取输入状态；只更新本地 HomeViewModel 与 InputsPage，不改变信源路由。 |
| `09c5c7b` | Weather: say why a reading failed in the diagnostics (#1538 #1548) | 选入：天气诊断记录 HTTP 状态/异常类型和在途状态，不输出城市或坐标。 |
| `4e8f25e` | Guard: back over the installer after an update Control Center installed (#1544) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `c5e2bc0` | Brightness: find TCL Google TV by its picture library, not com.tcl.settings | 暂缓：厂商能力检测和真实面板读回尚需设备证据，不能外推到 F3。 |
| `d012cb8` | Keep apps in front during activity pause and resume transitions | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `ff8da31` | README: TCL 65T7L, Hisense HZ55A65; XGIMI H3S notes from the 1.97 retest (#1547 #1545 #1474) | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `79c55ac` | Device notes: Dangbei DBB3, DBF6, DBF3 Air; XGIMI DOHKO inputs (#1546 #1477 #1544 #1542 #1497 #1483) | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `6ca7251` | HANDOFF, ARCHITECTURE: the 2026-11-15 pass | 略过：背景性或合并提交；按其包含的实际行为在对应专项工作包中继续追踪。 |
| `9064493` | Guard: a dialog's event no longer holds back the locked-boot start (PR #1449) | 暂缓：改变 HOME/守护/前台或按键语义，需 Android 5/6 与 F3 专项回归，不能在维护中扩大接管。 |
| `0568c76` | 1.98 (144) | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |
| `50ba53c` | HANDOFF: 1.98 released, both TVs checked, site deployed | 略过：交接、发布、README 或机型记录，不改变本地源码、版本、渠道或验证事实。 |

## Deferred

- The selected source fixes are desktop-only and keep all existing whitelist, update, layout, glass and independent-player boundaries.
- Revisit deferred vendor routes, guard lifecycle, wallpaper/4K behavior or PackageInstaller sessions only in separate work packages with their own Android 5/6 and target-device evidence.

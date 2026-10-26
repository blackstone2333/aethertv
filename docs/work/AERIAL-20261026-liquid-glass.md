# AERIAL-20261026-liquid-glass

## Spec

- Owner：主模型；允许修改：共享材质/主题、首页航拍渲染、控制中心/系统工具、手机页及相关资源、测试与设计文档；含手机复现的守护重绑定误重置菜单之最小修复。
- 依据 Apple HIG Materials、WWDC25 Meet Liquid Glass 与 WWDC26 平台设计更新建立共享设计规则。保持内容与导航层级、同心几何、克制明暗边缘与焦点反馈；不引入 Apple 专有资源或声称原生 Apple 渲染。
- 航拍运动期间底栏持续显示，折射采样当前视频帧；Full 使用单解码器 GPU 合成，不用定时截图伪造动态材质。Lite 保留低成本材质和操作暂停。
- 静态与动态底栏使用相同透镜参数；透明/磨砂仍可选。静态不运行持续循环。
- 电视管家成为原控制中心的内页，移除独立首页系统按钮；控制中心先呈现内存/存储状态，管家首屏用图形指标呈现状态，清理是默认焦点。网络复用原设置入口；详细参数、后台管理与文件互传放到二级页。遥控器 Back 按层级退出并恢复来源焦点，不增加专门返回按钮。
- 清理只释放可重建的桌面缓存；后台任务通过设备开放的系统管理能力操作，不伪报全机进程、CPU总占用或清理量。保留文件选择器与受限中转箱。
- 共享材质、圆角、排版规范覆盖现有面板；手机控制台只在导航使用透明材质，内容保留普通分组底色。
- Java17、com.dangbei.tvlauncher、app.orchard.tv、minSdk21、双Release、五语言；不改默认桌面/守护/原厂应用或用户布局。

## Plan

1. 阅读官方资料，记录设计依据和验收标准。
2. 重做共享光学与动态视频合成，保持底栏。
3. 统一面板和系统工具，调整手机导航层；按最新用户要求把系统工具改为控制中心中的电视管家，补足遥控器焦点和返回恢复。
4. Java17构建/单测/APK元数据；Redmi L1视觉、动态时序和功能回归。
5. 恢复测试偏好、更新证据并同步GitHub。

## Status

complete：1.2.0-dangbei / 103；基线 dab219d；主模型集中实施，无委派。完成本工作包的 L0 和 Redmi K40S L1 验收；不代表当贝 F3 L3 已通过。

## Evidence

- 依据：已读取 Apple 官方 Materials、WWDC25 219、WWDC26 102、27设计资源发布说明，以及 Designing for tvOS、Focus and selection、Remotes。约束和链接保存在 `docs/DESIGN_SYSTEM.md`；`AGENTS.md` 要求所有 UI 工作先读该规范。
- 已修复的旧行为：底栏透明采样无双线性过滤、偏白、动态航拍只复用旧静态画面，播放时隐藏底栏；系统工具为独立860dp居中面板，与300dp控制中心脱节。现在动态底栏在同一个 GPU pass 中采样视频当前帧，静态/动态使用共享光学参数；管家、详情、互传统一使用原控制中心外壳及导航。
- L0：Java17 下执行 `./gradlew testStandardReleaseUnitTest testGuardReleaseUnitTest --tests app.orchard.tv.ui.components.GlassOpticsTest`，两个 flavor 各2项通过。随后执行 `./gradlew testStandardReleaseUnitTest testGuardReleaseUnitTest --tests app.orchard.tv.guard.HomeGuardControlCenterTest assembleStandardRelease assembleGuardRelease`，两个 flavor 各3项通过，双 Release 构建成功。没有因文档收尾重复运行未受影响的旧测试。
- L0：包元数据核验为 `com.dangbei.tvlauncher` / 103 / 1.2.0-dangbei / minSdk21 / target34，namespace 保持 `app.orchard.tv`；standard 有 HOME、guard 无 HOME，CONFIG 使用白名单包名。项目规则、五语言 key/占位符与 Web JavaScript 语法检查通过。
- L1：Redmi K40S（Android14），USB 安装 standard Release 103 并执行 `cmd package compile -m speed -f com.dangbei.tvlauncher`。手机真实编译 OES shader；航拍运动时底栏持续显示并随当前帧折射。已目视日间金门桥、机场高频细节及夜景，四列透明与八列磨砂。
- L1：Full→Lite→Full 双向切换通过，修复旧 AndroidView 释放时清掉新输出导致的黑屏；Lite 操作暂停、闲置恢复通过。暂停读取得到未装饰原始帧，不把底栏和遮光烧入壁纸。保存8.03秒/244帧的实际运行录屏，首末帧均有底栏，背景位置持续变化；审阅截图/视频保存在本机，未提交用户应用布局、天气或二维码。
- L1：控制中心概览→管家，默认聚焦清理；实际清理曾释放3.3MB可重建缓存。详情和互传按 Back 回管家并恢复原入口，管家回主菜单恢复概览焦点。网络卡片复用系统网络设置，后台入口打开 MIUI 应用管理；从外部设置返回保留管家和来源焦点。互传二维码完整且可解码，受令牌保护的 HTTP API 正常。
- L1 发现并修复：手机系统设置返回后，原先已启用的守护重新绑定，把未知/旧前台误认成原厂桌面并发送 MAIN，关闭控制中心内页。最小修复优先使用同进程 Activity resumed 状态；无窗口重绑定、旧窗口与 resumed 冲突、paused 后真实原桌面三个回归均通过。没有修改守护权限或默认桌面，电视上的守护行为仍须 L3。
- 测试收尾：恢复测试前列数、玻璃、航拍分类/延迟/暂停、每日主题和渲染模式；所选壁纸与完整布局（忽略缓存重建产生的 stamp）逐项核对一致。移除测试创建的设备录屏/XML、ADB 转发及本机桥接，删除临时配对令牌/二维码/原始 state。没有修改用户应用顺序或原厂应用。
- 能力边界：内存/存储为系统状态，CPU 明确标注为桌面进程；直接清理仅移除本应用可重建缓存。普通 Android 应用不能承诺批量结束全机后台，入口交由系统管理，不显示虚构释放量。

## Exact next action

本包无待实施项。下一次有当贝 F3 时新建 L3 验收工作包，验证本版本的遥控器焦点、HOME/守护、硬解、HDMI、音量与性能；不要把手机证据改写成电视验收。

## Deferred

- 当贝F3 HOME/守护/硬解/HDMI/音量/性能仍未验证，需L3。
- Apple 原生 Liquid Glass 不是 Android 组件；本项目是依据公开设计原则的自有实现，低性能设备保留 Lite，GPU 初始化失败回退普通 SurfaceView。

# AERIAL-20261027-modular-controls

Status: review
Owner: main model

## Spec

- 控制中心由独立材质控件组成；遥控器可调整控件顺序、尺寸和显隐，保存后重启保留。编辑入口始终可达，支持恢复默认。
- 网络、设置的桌面与控制中心入口先进入 AetherTV 内页，显示状态和功能分类，再由明确操作打开系统设置。Back 返回父层并恢复来源焦点。
- 个性化使用有间距的独立玻璃分组卡片和手机卡片，透明外壳、虚化背景，保持五语言、液态/磨砂与明暗偏好。
- 保留 AERIAL-20261027-lite-performance 的优化；背景使用已有小图缓存，无逐帧截图、模糊或额外解码器。
- 主模型独占 ui/control、WallpaperSheet、HomeScreen、CcPage、配置与测试/文档；无委派。Java17、白名单 com.dangbei.tvlauncher、namespace app.orchard.tv、Android5/6；不动默认HOME、守护、无障碍与用户数据。
- 验收：布局配置异常值回退、无重叠排布、隐藏全部仍能恢复；双Release和APK契约；Redmi手机验证方向导航、二级页/Back、持久化、材质与PSS。F3关机，其遥控器/性能/厂商能力均未验证。

- 视觉补充：控制中心和个性化均采用薄模糊的Home图层，保留背景色彩与图标轮廓；取消叠加黑罩，材质明暗从近原色调节。缓存场景最长边480px，禁止全屏硬件截图和逐帧模糊。
- 2026-10-27最新验收：个性化适度压暗并增强小字可读性，选项使用标签之后的全部剩余宽度；手机卡片顶边与壁纸卡片对齐。控制中心及其设置内页只虚化右侧菜单区域，左侧桌面保持清晰；个性化保留全屏薄模糊。
- 半屏虚化边界必须以64dp透明渐变从清晰过渡到模糊，打开/关闭共用同一过渡；不出现硬切边、黑色遮条或额外视频取帧。

## Plan

1. 已完成：持久化控件模型和有界网格排布，接独立玻璃卡片与遥控器编辑。
2. 已完成：网络/设置内页与根页导航、个性化多卡片、缓存虚化背景及渐变/可读性修正。
3. L0/L1已完成：受影响单测、Java17双Release、APK检查和手机验证，更新0.3.1候选和证据并同步源码。L3留待F3开机验收。

## Status

代码与Redmi L1交互、薄模糊视觉修正验收通过，F3未验证；沿用0.3.1 / 106候选。布局、尺寸、显隐和顺序独立保存，编辑入口固定。菜单使用LazyColumn按需加载。

## Evidence

- 接管时分支 codex/aerial/aerial-20261027-lite-performance，未提交差异属于前序性能优化；本包在其上实施。

- Java17双Release通过，standard/guard各92项单测、0失败；新增4项覆盖配置往返、异常回退、混合尺寸/顺序排布及全部隐藏。
- Redmi K40S/Android14/standard 0.3.1 Release：覆盖安装和speed编译通过；独立卡片、网络/设置内页、Back来源焦点、编辑尺寸/顺序、进程重启保存与恢复默认观察通过。个性化卡片和横纵渐隐观察通过。截图/二维码/地址仅保留local。
- 模块化首版同条件Lite静态/航拍/菜单PSS中位数约209.6/249.8/234.0MiB，仅为Redmi趋势；薄模糊改版另行补测，不将手机数值推算为F3降幅。
- 最新四点修正的Java17双Release通过；IPTV已在当前工作树拆到独立模块，桌面standard/guard各70项单测、0失败，直播单测由独立播放器对话负责。两APK均为com.dangbei.tvlauncher、0.3.1 / 106（guard后缀）、min21 / target34，签名验证成功，standard保留HOME、guard无HOME，未再涨版本。
- 最新standard Release已覆盖安装Redmi并执行speed编译。个性化浅色背景适度压暗、小字阴影、五项航拍分类使用剩余宽度并完整可见、手机卡片与壁纸卡片顶边对齐观察通过；控制中心、网络和设置内页的左侧清晰、右侧64dp渐变模糊通过。Lite液态与Full磨砂均观察通过；Back返回后可重新打开，未更改系统网络设置。
- 渐变修正后Java17双Release再次成功，仅重跑受影响构建与视觉检查，未重复未受影响单测。命令为`JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease`，exit0；本地日志local/thin-feather-build.log。
- 最新薄模糊个性化版本Redmi Lite冷进程、静态→航拍→菜单顺序，各3次PSS：静态/航拍/菜单中位数约207.3/239.5/241.4MiB；首样本航拍约294.6MiB，仍有分配/回收瞬态。菜单数据为个性化页；右侧渐变层未单独作性能结论，F3降幅与流畅度未验证。原始样本仅local/perf-thin-final.json，不作为同机型以外推断。
- 最终已核对恢复原壁纸、渲染模式、菜单材质及明暗偏好，移除本轮ADB forward和手机占用锁，将手机释放给独立IPTV对话继续验收。截图和网络地址仅保留local。
- 桌面UI与NAS选型文档检查点9f132e4已推送至origin/codex/aerial/aerial-20261027-lite-performance；仅提交本Owner范围，播放器拆分、MediaBridge调用等Worker差异保留在工作树，待其独立验收后集成。当前手机测试APK包含该协作工作树，未发布Release。

## Exact next action

本包L0/L1计划已结束，保持review等待F3目标机验收。独立IPTV由另一对话继续，后续按其工作包集成；F3开机后补遥控器、焦点、半屏渐变/个性化可读性及内存/航拍流畅度证据。

## Deferred

- 当贝F3 Release真机验收；全屏实时模糊与额外视频合成不在本包范围。

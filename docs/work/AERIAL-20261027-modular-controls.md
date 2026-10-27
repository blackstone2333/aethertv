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

## Plan

1. 持久化控件模型和有界网格排布，接独立玻璃卡片与遥控器编辑。
2. 网络/设置内页与根页导航、个性化多卡片及缓存虚化背景。
3. 受影响单测、Java17双Release、APK检查和手机验证；更新本次0.3.1测试包与证据，提交同步。

## Status

代码与Redmi L1交互验收通过，F3未验证；沿用0.3.1 / 106候选。布局、尺寸、显隐和顺序独立保存，编辑入口固定。菜单使用LazyColumn按需加载。

## Evidence

- 接管时分支 codex/aerial/aerial-20261027-lite-performance，未提交差异属于前序性能优化；本包在其上实施。

- Java17双Release通过，standard/guard各92项单测、0失败；新增4项覆盖配置往返、异常回退、混合尺寸/顺序排布及全部隐藏。
- Redmi K40S/Android14/standard 0.3.1 Release：覆盖安装和speed编译通过；独立卡片、网络/设置内页、Back来源焦点、编辑尺寸/顺序、进程重启保存与恢复默认观察通过。个性化卡片和横纵渐隐观察通过。截图/二维码/地址仅保留local。
- 同条件Lite菜单PSS正在补测；此前性能包菜单数据属于旧结构，不能当作新结构结果。

## Exact next action

完成同条件Lite PSS补测和最终APK检查；接AERIAL-20261027-independent-media拆分播放，F3留待开机验收。

## Deferred

- 当贝F3 Release真机验收；全屏实时模糊与额外视频合成不在本包范围。

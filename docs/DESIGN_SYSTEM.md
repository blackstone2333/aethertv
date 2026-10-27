# AetherTV 设计系统

## 官方依据（2026-10-26 查阅）

- [Apple HIG: Materials](https://developer.apple.com/design/human-interface-guidelines/materials)：玻璃用于导航和控制层；文字密集面板使用regular，媒体上方简洁控件可用clear；避免玻璃叠玻璃。
- [Meet Liquid Glass / WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/)：透镜折射、环境明暗、形状嵌套、可打断反馈；玻璃内的内容使用普通透明fill。
- [Platforms State of the Union / WWDC26 102](https://developer.apple.com/videos/play/wwdc2026/102/)：27系列优化复杂背景扩散、暗边与镜面高光、统一圆角与组件一致性。
- [iOS/iPadOS/macOS27 design kits](https://developer.apple.com/news/?id=e2lxw9l1)：确认27设计更新；本项目仅参照公开原则，自行实现Android材质，不使用仅授权Apple平台的设计包资源。

- [Apple HIG: Designing for tvOS](https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos)、[Focus and selection](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection)、[Remotes](https://developer.apple.com/design/human-interface-guidelines/remotes)：远距离可读、方向导航、单一焦点；Back 回父层，返回时保留来源焦点。

## 所有 Agent 的实施规则

- 共享 `liquidGlass`、`ControlPanel`、`MenuRow`、`Type/Palette`，禁止新增一套不相干的仪表盘风格。电视优先tvOS焦点与遥控器语义；手机优先iOS分组和触控语义；不把Mac窗体直接搬上电视。
- 首页媒体底栏是clear导航层，运动时保留。光学采样必须来自当前背景；禁止用旧截图覆盖正在播放的画面。Full动态透镜由视频纹理GPU合成，Lite使用半透明降级。
- 折射体现边缘厚度，内部低畸变；先低幅度环境暗边，再窄镜面亮边，避免整圈粗白描边、发灰罩层和固定颜色染色。
- 电视管家是控制中心内页，共用导航、网络设置和外壳。先用图形状态传达信息，详细参数按需展开；不把状态指标做成无意义焦点。主操作默认聚焦，二级页返回恢复来源控件；不为遥控器添加重复返回按钮。
- 桌面与菜单分别保存液态/磨砂材质和液态明暗；菜单默认使用较暗的折射材质保障文字可读，磨砂选项保留regular扩散。内部信息使用分组列表、分隔线和字重，不嵌套玻璃卡片。
- 外壳上下使用窄亮边、左右使用中性灰暗边；静态与视频共用折射深度和方向光。焦点用弹簧伸展底板并同步增大圆角，至少保留图标/焦点框的安全留白。静态只缓存静止和伸展两个端态，禁止逐帧CPU重烘焙。
- 横纵滚动边缘只在内容溢出时使用柔和透明渐隐；不画遮盖内容的实色条。顶栏独立控件遵守同一焦点语义，Back恢复来源，管家与信号源入口可直接抵达内页。
- 底栏内外圆角与留白共用几何；聚焦放大后底板相应伸展，图标与焦点框不得贴上底板边缘。
- 字体沿用可分发的Inter/平台字体与统一Type；无Apple私有字体依赖。高对比度焦点、Lite和减少动态效果必须保留。
- 静态材质按背景/几何缓存；动态材质不做CPU逐帧截图/模糊，不增加第二解码器。退出桌面释放视频和GL资源。

## 视觉验收

- 明亮/深暗/高频细节的静态背景，四列和八列：边缘有体积、中心不发灰、圆角无阶梯、文字清晰。
- 动态航拍：连续可见底栏，玻璃内外特征随同一画面移动；暂停不把已合成底栏烧进壁纸；切回静态、打开面板、恢复播放无残留。
- 系统页与控制中心：右上角相同锚点/宽度/圆角/入场/列表焦点；返回层级正确；屏幕外点击关闭。
- 记录真实设备与限制；截图不包含二维码令牌、IP和个人文件，F3未测不得声称通过。

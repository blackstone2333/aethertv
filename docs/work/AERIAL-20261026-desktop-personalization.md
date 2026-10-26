# AERIAL-20261026-desktop-personalization

## Spec

- Owner：主模型；范围：桌面、壁纸、设置、系统工具、PhoneServer/Web、文档和必要单测。
- 恢复上游首次排序，已保存布局不迁移、不重排；长按与手机自由排布继续可用。
- 首页每排可选 4–8 个入口，默认保持 5；电视、文件夹、手机预览和方向键使用同一列数。
- 航拍默认立即播放，支持闲置延迟和交互暂停；Lite 默认操作即停、短暂闲置恢复。
- 航拍按现有目录分类选偏好；每日精选提供真实主题筛选和手动选择，明确其来源不提供官方主题标签。
- 新增透明折射底板，保留磨砂和简化模式；等距留白、外圆角=图标圆角+留白，静止时不持续计算。
- 右上角系统信息：CPU、内存、存储、网络；清理仅应用可重建缓存，反馈实际字节，不承诺强杀别的应用。
- 手机与电视局域网双向文件传输：令牌、限额、私有收件箱、路径检查，用户文件只按明确操作删除。
- 每日选择性检查上游，冲突保留定制；记录初次审查与合入结果。
- 固定 com.dangbei.tvlauncher / app.orchard.tv / Java 17 / minSdk21；不改手机默认桌面，不改当贝原桌面或无障碍。

## Plan

1. 建立验收与定时维护，恢复默认布局并统一列数。
2. 实现播放策略、素材偏好和电视/手机设置。
3. 实现折射玻璃与统一几何。
4. 系统信息、缓存回收、带限额文件互传。
5. 上游低端设备检测等适用改进；五语言、L0、双Release与APK核验。
6. Redmi K40S L1验证、视觉检查、修复，更新证据并提交同步GitHub。

## Status

complete：1.1.0-dangbei / versionCode 102。六步 Plan 已完成，Java 17 双 Release、L0 和 Redmi K40S L1 已验收；F3 专属行为保持未验证。主模型集中实施，未启动子 Agent。

## Evidence

### L0：构建、配置与单测

- Java 17；执行 `./gradlew testStandardReleaseUnitTest testGuardReleaseUnitTest assembleStandardRelease assembleGuardRelease --no-daemon` 成功。完整套件每 flavor 47 项通过，0 失败；包含传输路径/配额/中断清理和照片主题测试。
- 后续修复照片主题词、主题轮播、二维码白边、SoC 和缓存引用，受影响的 `PhotoThemesTest` 与双 Release 构建通过。最后仅修改五语言每日图片说明，再次双 Release 构建成功（2m03s）。未重复未受影响的测试。
- 最终 `aapt2 dump badging/xmltree`：包名 `com.dangbei.tvlauncher`、versionCode 102、minSdk21、白名单 CONFIG action；standard 有 HOME，guard 无 HOME。namespace 保持 `app.orchard.tv`。
- `tools/check-project-rules.sh`、Web `node --check`、五语言 key/占位符一致性及 `git diff --check` 通过。

### L1：Redmi K40S / Android 14 / standard release 102

- USB 覆盖安装成功，`cmd package compile -m speed -f com.dangbei.tvlauncher` 成功，完整组件显式启动 `Status: ok`。手机不作为 F3 设备结论。
- 4–8 列、透明/磨砂、航拍分类和每日主题均可保存；非法配置返回 400，混合请求不会部分写入。
- 八列底板、图标留白/圆角、右上角系统面板已目视检查；最终二维码从完整截图成功解码。系统页显示 QTI SM8250、内存、存储和网络。
- 局域网直接上传/下载与 ADB 转发下双向传输通过，SHA256 一致；无令牌、错误令牌及路径越界被拒绝。
- Android 系统选择器导入与另存均通过，另存文件再下载内容一致。缓存清理实际删除 3,290,048 bytes 可重建磁贴缓存，未强杀其他应用或删除用户资料。
- 播放：关闭面板后的暖恢复 102ms 开始、103ms 画面运动；Full 方向键导航保持播放。Lite 输入后 85ms 暂停，895ms 闲置后恢复。新播放器首次请求约 123ms、首帧运动约 1.326s，包含网络与解码等待。没有固定 10 秒延迟。
- 测试后按原始状态恢复 5 列、全部分类、自动渲染等偏好；逐项核对应用顺序、文件夹和隐藏状态。只删除两个记下 ID 的测试中转文件及本轮创建的另存/临时文件；原有中转项保留。临时端口转发和浏览器桥接已关闭，手机停留在主屏幕。

### 上游维护

- 已审查上游至 `2d6d468`。`1755506` 能力检测/Lite 简化、`9e27938` VPN 底层网络识别、`c97d58b` 图标抗锯齿/颜色分离为部分提取；实际范围和待审项见 [UPSTREAM_SYNC.md](../UPSTREAM_SYNC.md)。未声称整体合入 Aerial 1.52。
- 当前会话自动化“AetherTV 上游精选同步”已启用，每天北京时间 10:00 检查。工作树脏或同文件正在开发时登记待审；冲突保留定制，无变化安静。

## Exact next action

本工作包无剩余实施步骤；交付按本分支快进同步公开仓库。后续根据用户试用反馈创建新的工作单元；上游维护先复查 [UPSTREAM_SYNC.md](../UPSTREAM_SYNC.md) 中的部分提取与待合入项，F3 验收单独推进。

## Deferred

- Media3直播播放器、NAS内容聚合不在本轮。
- 当贝F3专属HOME/守护/硬解/遥控器/HDMI/音量/性能：未验证，需L3。
- 透明玻璃为静态背景边缘透镜采样；动态航拍播放时沿用原版收起底板策略，视频实时折射未实现。Lite 使用简化材质。
- 每日一图主题按中英文说明保守推断，来源不提供官方标签；无匹配时自动每日选项回退最新图片。
- CPU 展示本进程占整机算力的比例；Android 限制下清理范围为桌面自身可重建缓存。互传为私有中转箱，单文件100MB、总512MB、64项。

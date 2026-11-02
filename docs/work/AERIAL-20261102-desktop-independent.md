# AERIAL-20261102-desktop-independent

Status: review
Owner: desktop conversation (main model)

## Spec

- 用户2026-11-02明确要求桌面独立推进，播放器由各自对话负责，不能因播放器未提交改动阻塞桌面。
- 桌面使用独立Git worktree、分支、索引与构建输出。保留已验证的0.3.1/106桌面UI、Lite、自由排布/4–8列、航拍策略与分类、玻璃、电视管家和独立直播入口；只消费稳定的v1启动/只读迁移契约，不加载播放器Gradle模块或源码。
- 独立播放器实现和设备验收不属于本包；保留原协作目录中的全部差异，不搬走或删除其他Owner文件。共享契约兼容变动须先由桌面Owner审阅。
- 首轮桌面推进：选择性适配上游负T-Cast长度拒绝及Android5保持ADB的兼容性修复，补最小决定性回归；不引入其他HOME接管链变化。
- 固定com.dangbei.tvlauncher、namespace app.orchard.tv、Java17/min21、Compose1.9/Media3 1.8.1、本仓库升级渠道。沿用0.3.1/106，不发布正式Release。
- 不连接或修改设备，不改变原厂桌面、默认HOME、无障碍、keep_adb偏好、签名或用户数据。F3/真实遥控器/硬解码均保持未验证。

## Plan

1. 已完成：从当前已提交桌面基线建立独立工作区，仅复制桌面桥接差异与contracts，保留原目录。
2. 已完成：校验桌面不依赖players，明确多工作区所有权、接口与设备协调规则，并通知现有播放器对话独立工作。
3. 已完成：实施两项桌面稳定性候选，运行受影响回归、Java17双Release及APK契约检查。
4. 记录证据并提交、同步本工作区；后续维护从桌面工作区继续。提交结果由Git提交与远端分支确认。

## Status

桌面独立工作区与构建隔离已验证，L0/双Release/APK检查通过；旧工作区保持原样。直播和NAS对话均已转入各自managed worktree。当前包review，新增修复尚无目标电视证据。播放器构建、功能和验收由原Owner负责，不作为桌面门槛。

## Evidence

- 基线HEAD 5edcc5bd2b57a9061a4478a682f4903d1369790b；复制内容差异时忽略FileProvider产生的纯mode差异，不在原目录归一化权限。
- 当前分支codex/aerial/aerial-20261102-desktop-independent。实际本机路径记录于忽略的local/，不写设备或凭据到版本库。

- Java17 L0：`JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew projects :app:testStandardReleaseUnitTest :app:testGuardReleaseUnitTest :app:assembleStandardRelease :app:assembleGuardRelease --console=plain`，exit0，4m11s；standard/guard各77项、0失败。项目图仅`:app`，本工作区不存在players目录，构建未加载播放器源码/产物。
- 7项新增（每flavor）：3项ADB回归，4项两个SDK的桌面启动契约回归。Robolectric SDK23上模拟21/22只验证SDK分支，未作为Android5真实运行证据；Android6重启路径和缺少/安装独立播放器时的显式启动均有L0覆盖。
- 离线Python：`PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools/tests -v`，12项、exit0；含负长度在有界子进程退出、分片/粘包/UTF8及安装成功/拒绝/失败回归。没有连接TCL或任何外部网络。
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 bash tools/check-project-rules.sh`，exit0；diff空白检查通过。
- aapt2 36.1.0/apksigner：双APK均com.dangbei.tvlauncher、106、min21/target34；standard版本0.3.1且含HOME，guard版本0.3.1-guard且无HOME。CONFIG白名单动作、signature保护的旧配置provider保留；清单和R8映射均无内置live实现。签名核验通过，且与旧协作目录各flavor测试APK证书一致。
- standard/guard大小分别3,072,434 / 3,073,109字节；仅为APK文件大小，不代表运行内存。SHA256分别`ba946b6146b96400517fe7ceb4452dd009e03f831d00cf3bbbed7c789f602012` / `ffb472806c4a9e45c8c02479d076e96ba152278e3f54ff7ceb71aa942a709223`。
- 本机日志及结构化检查仅local/desktop-independent-build.log、local/desktop-independent-evidence.json；测试包位于local/releases/aethertv-0.3.1-desktop-independent/。签名/设备资料未复制进版本库。
- 定时维护aethertv已改为当前桌面工作区，10:00每日计划、原目标对话和ACTIVE状态均核对保留；不再因其他工作区脏树暂停桌面，不自动操作共享设备。
- 直播独立检查点3ff74e9已由原Owner推送，其43项测试/独立Release通过；NAS已进入独立工作区，其构建结果由自身交接记录。本包不等待NAS验证，也不复验播放器功能。
- 本轮无手机/模拟器/电视操作；Android5实际运行、TCL安装、F3的HOME/遥控器/解码/性能均未验证。历史L1只保留原功能范围，不扩展为新补丁实机结论。

## Exact next action

桌面L0和构建计划已完成。后续从本独立工作区推进安装成功判断、Android5/6诊断与更新TLS等候选；保持当前UI和用户配置，按受影响范围验证、提交同步，不等待播放器。

## Deferred

- F3的白名单/HOME/遥控器/HDMI/音量/待机/性能与MStar硬解码验收。
- 播放器UI/后端、频道库、NAS内容扫描和首页预览不在本包实现。

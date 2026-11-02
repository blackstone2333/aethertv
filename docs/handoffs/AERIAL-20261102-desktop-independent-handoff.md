# 桌面独立工作区接管

Status: review
Owner: desktop conversation (main model)

## 当前目标与范围

2026-11-02用户要求桌面独立推进。桌面分支`codex/aerial/aerial-20261102-desktop-independent`，基线`5edcc5b`。本机工作区路径在忽略的`local/coordination/desktop-workspace.txt`及桌面对话工作区附件中；后续命令必须使用当前桌面工作区。旧共享协作目录保持原样，不回去改动、暂存或清理。

桌面Owner负责app、根构建、contracts、桌面文档；直播和NAS对话分别使用自己的worktree/分支/构建输出，只负责其播放器及相应工作包/交接。直播独立分支`codex/aerial/aerial-20261102-live-independent`已有检查点`3ff74e9`；NAS环境隔离由原Owner负责。桌面不以播放器提交/构建/后端/设备状态作为继续开发的门槛。

## 变更与约束

- 根Gradle仅`:app`；不需要players源码或播放器APK。保留0.3.1/106的UI、Lite、4–8列、自由排布、玻璃、个性化、分类和系统工具。
- 桌面不含旧内置live实现；只含MediaBridge、只读签名保护LegacyMediaProvider及协议v1。原有直播配置/缓存保留；用户在播放器显式导入，无后台轮询或直播解码器。
- 适配上游`60e0b8a`负T-Cast长度拒绝、`8364d1c`Android5不执行ADB的0→1重启。其他候选继续由UPSTREAM_SYNC逐项追踪，不整体关闭。
- Java17/min21、白名单com.dangbei.tvlauncher、namespace app.orchard.tv、Compose1.9/Media3 1.8.1和本仓库更新渠道不变。
- 不操作设备、HOME、原厂桌面、无障碍、keep_adb偏好、用户数据或正式Release。Android5真实运行和F3专属行为未验证。

## 证据与当前状态

Java17双Release、各77项单测、12项离线协议测试和双APK包名/签名/权限检查通过；定时维护入口已迁到桌面工作区，计划保持每日10:00。构建、测试、APK和同步结果以`docs/work/AERIAL-20261102-desktop-independent.md`的Evidence为准。历史手机/F3安装证据只在原范围有效，不能作为本轮新补丁的实机证据。

## Exact next action

完成本包验证和提交后，继续从桌面独立工作区逐项处理安装成功判断、Android5/6诊断与更新TLS等待办。只运行受影响回归、双Release及APK契约；无设备时明确未验证。播放器协议变更先提案，再由桌面Owner集成。

## 恢复路径

原协作目录和全部播放器未提交内容保留；不要reset/clean或反向复制整棵目录。若桌面工作区缺失，先根据Git分支和对话附件恢复独立worktree，不在旧共享目录切分支。设备测试仍须先协调。

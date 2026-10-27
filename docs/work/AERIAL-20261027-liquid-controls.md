# AERIAL-20261027-liquid-controls

Status: review
Owner: main model

## Spec

- 交付 0.3.0 测试版及双 flavor 安装包，供用户晚间电视验收。
- 本轮范围：上下亮/左右灰的玻璃边缘、聚焦弹性伸展、桌面/菜单独立材质与液态明暗、横纵溢出渐隐、仅应用菜单显示名称、四个可选独立顶部控件、五个 TV 客户端安装和前台直播。
- 验收：4–8 列聚焦图标及焦点框与 Dock 保持留白；菜单可读并支持方向键；TV/Web 设置一致且重启保留；独立管家和信号源 Back 返回原控件；直播首帧、EPG、换台、失败回退和解码器释放可观察。
- 唯一可变所有者为主模型；范围为设置、共享材质/首页/控制中心、Web/资源、独立 live 播放模块及文档。本轮无委派。
- 固定 Java 17、包名 com.dangbei.tvlauncher、namespace app.orchard.tv、Android 5/6 兼容线及 standard/guard Release。不改 HOME、无障碍、系统应用和用户数据。
- 产品版本遵循主/功能/修订三位规则；当前 0.3.0，安装序号独立递增为 105。仅提供 prerelease。

## Plan

1. 已查阅 Apple HIG Materials、Focus and selection 及 TV 客户端发行源。
2. 已实施设置迁移、共享光学边缘、焦点留白、菜单材质和滚动渐隐。
3. 已接入 TV/Web 五语言及直接控件；前台 Media3 播放保持单解码器所有权。
4. 双 Release、各 82 项 L0 和 Redmi L1 已验证；错误态重试焦点及频道表互斥已复核并封包。
5. 已更新项目图、版本规则和安装步骤；本包与代码同次提交，提供 GitHub 预发布双 APK 及校验和。

## Status

- 基线 71ab37b；分支 codex/aerial/aerial-20261027-liquid-controls。
- 六项桌面/菜单设置已实施并在 Redmi 验证，保留自由排序；测试后恢复原偏好。
- 五个 TV 客户端已安装、启动；详细版本见设备文档。未替用户登录，未将私人媒体/账号截图上传仓库。
- Roadmap 推进到前台直播 MVP；本包 review 表示等待目标电视及用户视觉验收，不代表 F3 已通过。

## Evidence

- Java 17：testStandardReleaseUnitTest、testGuardReleaseUnitTest、assembleStandardRelease、assembleGuardRelease 成功，各 82 项，零失败/错误。
- 新增六项测试覆盖流式 chunked、Range、跨 origin 认证隔离、截断响应及频道合并/源过滤。
- Web JS 语法、五套 226-key 语言表、Android 五语言资源通过；项目规则通过。
- APK：0.3.0 / 0.3.0-guard，versionCode 105，包名/namespace 不变，min21/target34；standard 有 HOME，guard 无 HOME；CONFIG 白名单、LiveActivity 非导出、双包签名校验通过，沿用开发测试签名。
- Redmi K40S / Android 14 / standard Release：覆盖安装并 speed 编译。4/8 列聚焦留白、菜单材质与 0/100 明暗独立、名称 grid、独立管家/输入页 Back 来源、非法设置拒绝、Lite/Full 切换、进程重启持久化和偏好恢复通过。
- 同机 L1：临时 HLS 与 chunked TS 首帧、EPG 节目/进度、503 主源到备用源、换台、失败提示和方向键重试已观察。日志包含 opens attempt 1/2 → 2/2 → rendered first frame，以及换台/离开释放解码器。
- 临时源仅按本次创建 ID 移除；未触碰现有用户源。测试输入与含二维码/用户媒体的截图仅存 local/。
- Apple 官方依据：[Materials](https://developer.apple.com/design/human-interface-guidelines/materials)、[Focus and selection](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection)。上下/左右边缘方向为本项目用户验收要求。

## Exact next action

用户使用 0.3.0 预发布包，按 docs/INSTALL.md 的晚间清单在目标电视验收；记录机型、flavor、现象与证据，再据此立修订工作包。

## Deferred

- 当贝 F3 L3：HOME、原厂心跳、防黑屏、守护/待机、真实遥控器、HDMI、音量、多任务、硬解码均未验证。
- 真实家庭直播源与长时间网络稳定性、首页静音预览、NAS 推荐/历史直连和服务认证待后续工作包。
- fnOS 官方 1.4.2 CDN 返回 403，手机安装厂商当贝发行的 1.4.1；第三方客户端影片播放未验收。

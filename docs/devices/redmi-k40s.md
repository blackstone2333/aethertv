# Redmi K40S：AetherTV L1 验收

设备为 Redmi K40S / Android 14（SDK 34）。这是手机验证，不替代当贝 F3 的电视能力验收；连接标识、网络地址、私有配置和截图只保存在 local/。

## 2026-10-27：0.3.0 / 105 standard Release

- 相同包名和签名覆盖安装，保留应用数据，执行 `cmd package compile -m speed -f com.dangbei.tvlauncher` 成功。
- 4/8 列焦点伸展、方向光边缘、grid 名称、桌面/菜单独立材质与明暗、控件直达与 Back 来源、Lite/Full 切换、重启保存通过。
- 使用本地临时信号源观察 HLS 与 chunked TS 首帧、EPG、503 主源回退、换台和失败重试；测试源按创建 ID 删除，偏好恢复。
- 真实遥控器、电视 HOME/守护/防黑屏、HDMI/音量、多任务、电视硬解码、真实家庭直播源长期播放均未验证。

## 本轮安装的 TV 客户端

| 应用 | 包名 | 已安装版本 | 来源 |
|---|---|---|---|
| Emby Android TV | tv.emby.embyatv | 2.5.03g | Emby 官方发行 |
| OrionTV | com.oriontv | 1.3.14 | orion-lib/OrionTV GitHub Releases |
| 云视听小电视 | com.xiaodianshi.tv.yst | 1.8.9 | 哔哩哔哩官方 TV 下载 |
| 网易爆米花 TV | com.netease.filmlytv | 2.6.23 | 网易官方 TV 下载 |
| 飞牛 TV | com.trim.tv | 1.4.1 | 当贝市场的厂商发行页 |

五包均做 APK 元数据/签名有效性检查、安装成功并启动。飞牛官网 1.4.2 CDN 拒绝下载，使用上述 1.4.1。未替用户接受登录协议、配置账号或验证各客户端影片播放。用户媒体、登录状态和服务器信息不进入公开证据。

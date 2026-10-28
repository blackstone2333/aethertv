# 当贝 F3

## 当前定制版证据（2026-10-28）

F3 / Android6.0 / API23，硬件上报maserati；连接信息和原始证据仅保存在忽略的local目录。

| 应用 | 包名 | Release测试版 | 本轮验证 |
| --- | --- | --- | --- |
| AetherTV | com.dangbei.tvlauncher | standard 0.3.1 / 106 | 同签名覆盖0.3.0 / 105，启动与桌面显示 |
| AetherLive | app.aethertv.live | 0.1.0 / 1 | 首次安装、启动至添加直播源页面 |
| AetherCinema | app.aethertv.cinema | 0.1.0 / 1 | 首次安装、启动至服务账号页面 |

三包均使用v1签名的Release测试包，安装回读版本正确。三次显式启动均返回Status: ok、焦点属于目标Activity，截图显示实际界面。最终回到以太桌面，保留原有Dock和壁纸。限定AndroidRuntime日志未发现三包崩溃；此结论限于本轮启动观察。

详情见[三包安装工作包](../work/AERIAL-20261028-f3-three-apps.md)。播放器完整手机证据与电视待验范围分别见独立媒体和NAS工作包。

## 固定边界

- 桌面applicationId必须保持com.dangbei.tvlauncher、namespace app.orchard.tv、Java17/min21。直播Java17/min21；NAS独立工程按用户授权使用Java21构建、JVM17字节码/min23，不升级桌面构建环境。
- 必须保留原厂桌面com.dangbei.leard.leradlauncher启用；MStar心跳/视频黑屏限制仍适用。本轮前后其状态均为系统默认启用。
- 不采用无障碍接管，不更改HOME默认项。本轮preferred-xml与既有无障碍服务值保持一致；历史服务条目原样保留，没有清理或覆盖。
- 相同签名覆盖安装，不能通过卸载、清数据或改名解决更新冲突。直播源及NAS账号不从手机自动搬入。

## 安装与诊断限制

- Android6固件没有cmd工具：按项目规则尝试`cmd package compile -m speed -f <package>`返回cmd: not found。旧shell协议可能仍返回外层exit0，必须按实际输出判断；本轮没有speed编译通过证据。
- 该固件的pm没有resolve-activity子命令。核对既有HOME可读取`dumpsys package preferred-xml`；不要为查询而设置默认桌面。
- 本轮原ADB服务报No route to host，而ICMP和当前进程TCP连通；新建独立端口ADB服务后成功。没有证据将其归因于电视或网络设置，不更改电视配置或重启共享ADB服务。任务自建服务在结束后关闭。

## 未验证

真实遥控器与Back来源、HOME/待机/开机复活、HDMI、音量、多任务、真实家庭直播源和NAS完整播放、MStar硬解码、视频黑屏、GL效果/降级路径、长时稳定性和内存/流畅度。本轮安装/静态启动不能关闭这些验收项。

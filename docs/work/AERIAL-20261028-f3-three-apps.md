# AERIAL-20261028-f3-three-apps

Status: complete
Owner: parent conversation / device deployment

## Spec

- 用户已开启电视，明确要求安装最新三个应用：AetherTV、AetherLive、AetherCinema。
- 使用已通过手机验证的Release测试包；桌面与直播保持Java17，NAS独立应用沿用用户授权的Java21构建边界。
- 桌面applicationId保持com.dangbei.tvlauncher、namespace保持app.orchard.tv；覆盖升级保留现有数据，先核对设备、flavor、版本和签名。
- 不更改默认HOME，不停用原厂桌面或系统应用，不覆盖无障碍，不清理用户配置和媒体，不发布正式Release。
- 验收限于目标电视三包安装、版本回读、启动及即时崩溃检查；遥控器、家庭源播放、MStar解码和长时性能分别记录，不能因安装成功而视为通过。

## Plan

1. 已阅读当前交接、取得设备独占并连接F3。
2. 已复核最新Release产物、同签名覆盖条件和已有安装状态；保留旧桌面APK本地副本。
3. 三包逐项安装成功；speed编译命令在该Android6系统不支持，实际结果已记录。
4. 三包启动与截图检查通过，恢复以太桌面前台，记录证据并释放设备锁。

## Ownership

仅新增本工作包、必要的F3设备结论及忽略的local部署证据；不覆盖独立播放器Owner的未提交源码或工作包。不reset/clean、切换分支或批量暂存。

## Status

安装与启动范围已完成：F3 / Android6 / API23上，桌面从0.3.0/105同签名standard覆盖更新至0.3.1/106，直播0.1.0/1和影音0.1.0/1首次安装成功。三包显式启动返回Status: ok，截图显示实际界面，限定日志未发现三包AndroidRuntime崩溃。最终停在以太桌面，既有Dock与壁纸可见。直播待添加源，影音待配置账号；没有迁移手机账号或自动导入源。

## Evidence

- 显式Java17运行桌面双Release及独立直播Release构建，exit0；桌面两flavor各70项单测通过、0失败。
- 三包aapt2包名、版本、minSdk与apksigner v1签名检查通过：AetherTV 0.3.1/106/min21、AetherLive 0.1.0/1/min21、AetherCinema 0.1.0/1/min23。NAS使用已有最终验证包，源码输入未晚于产物，哈希与NAS交接一致。
- 三包稳定副本及构建、测试、元数据、签名和连接日志存忽略的local/f3-three-apps-20261028；无源码变更、提交或发布。重打包携带新的VCS信息，桌面及直播哈希已在本地重新核验，不沿用旧产物哈希。
- 原ADB服务对同一地址报No route to host；用户确认地址后，ICMP及当前进程TCP直连可达，独立端口ADB服务建立连接。未重启原ADB服务，原因未进一步归因；独立连接和服务在部署后关闭。
- L3有限范围：三次`adb install -r`均Success，安装后回读三包版本/安装序号正确；原桌面APK签名与新包一致，保留standard的HOME声明。原厂桌面保持installed=true、enabled=0（系统默认启用）；安装前后preferred-xml一致，无障碍服务值规范化换行后完全一致。
- L3有限范围：三包`am start -W -n <package>/<activity>`均Status: ok且焦点属于目标Activity，实际界面截图已检查；新播放器分别显示未配置源/账号的初始页。返回桌面后Dock加载并显示原有应用，未改动用户布局/壁纸。限定AndroidRuntime错误日志中三包引用为0，不等同于长期稳定性验收。
- 已按规则尝试每包`cmd package compile -m speed -f <package>`；F3返回`/system/bin/sh: cmd: not found`。该ADB版本外层exit0不能视为编译通过，结构化结果已明确supported=false/success=false；未宣称speed编译完成。
- 本地最终状态、启动/安装/配置对照、APK副本和截图位于local/f3-three-apps-20261028；无源码改动、发布或用户数据清理。设备限制与后续验收见docs/devices/dangbei-f3.md。

## Exact next action

本安装工作包无剩余步骤。由用户试用新应用；另开验收工作包补真实遥控器、家庭直播源和NAS播放、MStar解码及性能，不能将本轮安装/启动证据扩展成这些能力通过。

## Deferred

真实遥控器验收、HOME/待机/HDMI/音量、多任务、真实家庭流与NAS完整播放、MStar硬解码和长时性能；本轮只按取得的实际证据更新范围。

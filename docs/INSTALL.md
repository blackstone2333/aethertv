# 安装与升级 AetherTV

源码与版本入口：[blackstone2333/aethertv](https://github.com/blackstone2333/aethertv)。
当前公开下载为 **0.3.0 测试版**：[v0.3.0 预发布](https://github.com/blackstone2333/aethertv/releases/tag/v0.3.0)；桌面源码仍为 0.3.1 / 106 优化测试版，本轮未发布新版本。一般设备选择 `aethertv-standard.apk`；拒绝 HOME 应用的固件选择 `aethertv-guard.apk`，`mt5873` 投影仪另见下文限制。

## 构建

使用 Java 17 和已安装的 Android SDK，在仓库根目录运行：

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleStandardRelease assembleGuardRelease
```

标准版位于 `app/build/outputs/apk/standard/release/app-standard-release.apk`；守护版位于对应的 `guard/release` 目录。标准版有 HOME 声明；守护版用于拒绝安装 HOME 应用的固件。

## 安装工具的机型预检

`tools/install.sh` 在构建、下载或安装前读取机型；读取失败或输出无效时停止。上游报告 Redmi 投影仪 4 / 5 Pro（`ro.product.model` 恰为 `mt5873`）使用 HOME 版本可能卡在开机画面。此机型禁止 standard，工具会明确停止，需使用原签名的 guard APK 并另行指定 `guard`，不会自动替换请求的版本。这条限制不扩大到当贝 F3 或其他 MStar 机型。

`mt5873` 的 guard 安装还要求本机 Android SDK build-tools 中有 `aapt2`。工具检查实际选中的 APK，包括 `APK_FILE` 提供的文件：包名必须是 `com.dangbei.tvlauncher`、版本名以 `-guard` 结尾、包含完整类名 `app.orchard.tv.MainActivity` 且不声明 HOME 类别。缺少检查工具、APK 无法解析或元数据不符时，在安装及系统设置步骤前退出，文件名不能代替检查。

覆盖安装的签名一致性仍由 Android 校验；签名冲突会停止，不尝试卸载或清数据。TCL Android 5.1 的 `install apk has be disabled from pm` 提示进入已有的 guard / 多屏互动或本地安装流程，仍需电视端确认，不能据此宣称安装成功。正常 ADB 安装只接受退出码为零且含完整 `Success` 行的结果。

上述预检与拒绝提示处理已通过离线替身测试和真实 Release APK 静态检查；对应投影仪、TCL 及 F3 的实际安装/启动行为未验证。安装工具原有授权与桌面接管步骤仍需按目标设备另行确认，不用于自动维护时操作设备。

## 手机试用

手机只用来验证页面、配置、解析和缓存。开启 USB 调试并授权电脑；MIUI 还需开启“USB 安装”，保持解锁并同意安装提示。

```bash
adb -s <设备序列号> install -r -g app/build/outputs/apk/standard/release/app-standard-release.apk
adb -s <设备序列号> shell am start -n com.dangbei.tvlauncher/app.orchard.tv.MainActivity
adb -s <设备序列号> shell cmd package compile -m speed -f com.dangbei.tvlauncher
```

保留手机原有默认桌面。打开 AetherTV 的“个性化”，扫描二维码进入手机控制台；“更多”中可选择 Auto/Full/Lite，并添加 M3U 与 XMLTV/XMLTV.gz 源。地址和配置留在应用私有存储，不会上传 GitHub。

## 当贝 F3

包名必须保持 `com.dangbei.tvlauncher`，源码 namespace 保持 `app.orchard.tv`。原厂桌面 `com.dangbei.leard.leradlauncher` 必须保留启用；F3 不使用无障碍接管方案。

当前版本在 Redmi K40S 上的测试不能证明 F3 的 HOME、黑屏、遥控器、HDMI、待机和硬解码兼容性。这些项目仍需单独验收，状态见工作包。

## 晚间电视验收（0.3.0）

1. 覆盖安装与现有版本相同 flavor 的 APK，打开桌面并执行 speed 编译；先保留电视现有 HOME 设置。
2. 在「个性化」切换 4/8 列，聚焦首尾图标，检查 Dock 留白、上下亮边/左右灰边和航拍中底栏持续显示。
3. 分别切换桌面/菜单液态或磨砂、调明暗；选择「应用名称 → 仅应用菜单」，向下进入应用菜单检查名称与边缘渐隐。
4. 打开四个独立控件，检查直接到达和 Back 返回；网络、HDMI 与系统设置只做本机能力验收。
5. 用手机添加已有合法 M3U/EPG 源，从天线按钮播放；检查换台、失效源回退、返回后航拍恢复，再单独验证待机唤醒和其他视频 App。

测试版通过 Redmi L1；当贝 F3 的 HOME/守护/防黑屏/硬解码和真实遥控器尚未验证。第三方 TV 客户端只安装到 Redmi，没有替用户登录或配置电视服务。

## 后续升级

应用只检查本仓库的 `updates/version.txt`，只下载本仓库 Releases 的 `aethertv-standard.apk` 或 `aethertv-guard.apk`，不会使用上游 Aerial 的安装包。版本提示仅在 manifest 数字高于本机时出现。发布维护者应先上传经过验收的同签名 APK，再更新版本 manifest；未发布前不要提高 manifest。

覆盖安装必须使用相同包名和签名密钥。密钥放在本机 `local/`；未配置时会使用开发机调试密钥，该包不应作为长期公开发行签名。不要通过卸载应用解决签名冲突，以免丢失设置和照片。

0.3.0 采用新的产品编号基线；早期 1.x 开发包可用相同签名的 APK 手动覆盖安装，安装序号仍为递增的 105。测试包通过预发布链接获取，应用内稳定版下载通道待正式发布后使用。

# 安装与升级 AetherTV

源码与版本入口：[blackstone2333/aethertv](https://github.com/blackstone2333/aethertv)。
当前 v0.1 为开发验收版，尚未发布 GitHub Release；可从源码构建。

## 构建

使用 Java 17 和已安装的 Android SDK，在仓库根目录运行：

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleStandardRelease assembleGuardRelease
```

标准版位于 `app/build/outputs/apk/standard/release/app-standard-release.apk`；守护版位于对应的 `guard/release` 目录。标准版有 HOME 声明；守护版用于拒绝安装 HOME 应用的固件。

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

## 后续升级

应用只检查本仓库的 `updates/version.txt`，只下载本仓库 Releases 的 `aethertv-standard.apk` 或 `aethertv-guard.apk`，不会使用上游 Aerial 的安装包。版本提示仅在 manifest 数字高于本机时出现。发布维护者应先上传经过验收的同签名 APK，再更新版本 manifest；未发布前不要提高 manifest。

覆盖安装必须使用相同包名和签名密钥。密钥放在本机 `local/`；未配置时会使用开发机调试密钥，该包不应作为长期公开发行签名。不要通过卸载应用解决签名冲突，以免丢失设置和照片。

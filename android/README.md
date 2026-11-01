# Android 试用版 0.2.0

面向 iQOO 15（OriginOS 6 / Android 16）制作，最低 Android 11。APK 已编译并通过签名、对齐、素材完整性及 14 项文字匹配检查，**尚未在真机或模拟器运行验收**。不能保证全部应用和输入法均能触发。

## 手机上使用

1. 在手机浏览器 [下载 APK](https://github.com/vakasu11/mj-spiderman-desktop-effect/releases/download/android-v0.2.0/mj-spiderman-android-0.2.0.apk)，或打开 [安卓发布页](https://github.com/vakasu11/mj-spiderman-desktop-effect/releases/tag/android-v0.2.0)，在 Assets 中选择 `mj-spiderman-android-0.2.0.apk`。下载完成后点击安装，按系统提示核对安装来源授权。`Source code (zip)` 是源码，不是手机安装包。
2. 打开“mj-spiderman”，阅读说明，点“无障碍服务设置”。
3. 在系统无障碍设置中找到同名服务，由你手动开启，然后回到应用。
4. 先点两个试听按钮确认画面和声音，再在应用内测试框切到英文输入 `mj`。
5. 应用显示“已开启”后，可切到其他应用的输入框试用。拼音候选栏中尚未提交的 mj 不一定可识别；可先输入并提交英文 mj。
6. 主按钮可暂停／恢复，声音开关可静音。“停止并关闭无障碍服务”会同时停用系统服务。仅返回桌面或关闭应用界面不会主动停止服务。

若没有播放，先确认本应用试听与测试框是否工作，再检查目标应用是否提供文字变化事件。密码控件会跳过。不要据此推断所有微信、QQ、游戏或浏览器输入框均可用。系统锁屏或熄屏时不会新建特效。系统可能回收服务；可在手机的应用电池管理中检查后台限制，具体菜单名称随版本变化。

## 权限与数据

服务通过系统无障碍 API 接收输入控件的文本变化，并检查本次编辑光标前两个字符是否为 mj。**无障碍服务具有读取其他应用控件文字的能力**，因此应用会先展示说明，只有你明确前往系统设置并开启才会工作。

不记录文本、不上传数据，不声明 INTERNET、存储、麦克风、通知、普通悬浮窗权限；使用无障碍服务自带的非触摸、非焦点悬浮层。保存的只有开关偏好；音效文件会放入应用缓存。动画与音效沿用原作者资源。

识别的是系统提供的输入框文本变化，和 Windows 版读取实际按键不同。粘贴 mj 也可能触发；中文输入法内部候选栏或自绘控件不一定提供事件。

## 后续更新

应用没有内置自动更新。从 [全部发布版本](https://github.com/vakasu11/mj-spiderman-desktop-effect/releases) 选择新版 Android APK 下载，尝试直接覆盖安装；更新需要包名、签名一致且版本号满足系统要求。请勿先卸载旧版，卸载会清除应用设置。更新后回应用检查无障碍服务，再测试试听和输入框。

当前仍为 0.2.0 试用版，文档更新不代表 APK 功能已更新或完成真机验证。[返回首页查看 Windows 下载与两端说明](../README.md)。

## 构建

使用 JDK 21、Google Android SDK Platform 36 与 Build Tools 36.0.0，Python 3 仅负责编排。无需 Gradle 或第三方 Android 库。

```powershell
python android/build_apk.py --jdk "JDK目录" --platform "SDK/platforms/android-36" --tools "SDK/build-tools/36.0.0" --out "dist/android"
```

构建脚本会运行文字匹配测试，打包桌面版原始素材，使用本地开发密钥签名并校验 APK。密钥在被 Git 忽略的 `work/android-development.keystore`，不得提交。此为本地开发签名，后续覆盖更新需保留同一密钥；本次交付不包含签名私钥。

原作者：https://github.com/f20880479-lab/mj-spiderman-desktop-effect

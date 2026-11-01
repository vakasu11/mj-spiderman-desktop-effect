<div align="center">

# 🕷️ 蜘蛛侠桌面特效

**Spiderman Desktop Effect · Windows & Android**

输入 `mj`，蜘蛛侠从天而降 🎉 每次随机播放一套原版动画！

**[🖥️ 下载 Windows 版](https://github.com/vakasu11/mj-spiderman-desktop-effect/releases/tag/windows-v1.0.0)** · **[📱 下载安卓试用版](https://github.com/vakasu11/mj-spiderman-desktop-effect/releases/tag/android-v0.1.0)** · **[📦 全部版本](https://github.com/vakasu11/mj-spiderman-desktop-effect/releases)**

</div>

---

✨ 一个有趣的桌面特效小应用：Windows 启动后监听按键，连续输入 `mj`，屏幕顶部就会出现带声音的透明蜘蛛侠动画，播放完自动消失。支持最小化运行、反复触发、暂停和静音，也提供独立的安卓试用版。

> Windows 默认**输入 mj 立即触发**，不用回车，也可切换成回车触发。安卓通过无障碍服务识别输入框文字，当前仍为试用版，尚未经过真机或模拟器运行验收。

## 🎯 功能亮点

| 功能 | 说明 |
| --- | --- |
| 🕷️ 一键召唤 | Windows 连续按 `m`、`j` 即可触发；安卓在支持的输入框提交英文 `mj` 触发 |
| 🎲 双特效随机 | 内置原版倒挂 / 荡蛛丝两套动画，每次随机播放一套 |
| 🎬 透明动画 | 带音效的透明悬浮特效，播放后自动消失 |
| 🖱️ 点击穿透 | 特效层不接收鼠标或触摸操作，可继续操作下面的应用 |
| ⏯️ 暂停与恢复 | 主按钮控制自动触发，暂停时立即停止当前动画和声音 |
| 🔊 静音与试听 | 可关闭声音，也可分别试听两套动画 |
| 💻 最小化运行 | Windows 控制窗口最小化后仍可触发，关闭窗口即可退出 |
| ⚙️ 保存偏好 | 保存触发、声音等对应平台支持的设置 |

## 📥 下载入口

| 平台 | 程序下载 | 要求与状态 |
| --- | --- | --- |
| 🖥️ Windows | **[直接下载 EXE](https://github.com/vakasu11/mj-spiderman-desktop-effect/releases/download/windows-v1.0.0/MJ-Spiderman.exe)** · [ZIP 使用包](https://github.com/vakasu11/mj-spiderman-desktop-effect/releases/download/windows-v1.0.0/MJ-Spiderman-Windows-1.0.0.zip) | Windows 10/11 x64；已在 Windows 11 x64 检查运行 |
| 📱 Android | **[直接下载 APK](https://github.com/vakasu11/mj-spiderman-desktop-effect/releases/download/android-v0.1.0/MJ-Spiderman-Android-0.1.0.apk)** | Android 11+；面向 iQOO 15 制作，尚未设备运行验收 |

Windows EXE 约 70 MB，Android APK 约 40 MB。也可以进入 [Releases](https://github.com/vakasu11/mj-spiderman-desktop-effect/releases)，展开对应版本的 **Assets（附件）** 下载。

> `Source code (zip)` 和 `Source code (tar.gz)` 是源码压缩包。直接使用请选择上面的 **EXE / ZIP 使用包 / APK**。

## 🖥️ Windows 使用方法

### 方式一：免安装运行（推荐）

1. [下载 `MJ-Spiderman.exe`](https://github.com/vakasu11/mj-spiderman-desktop-effect/releases/download/windows-v1.0.0/MJ-Spiderman.exe)，也可下载 ZIP 使用包并解压。
2. **把 EXE 放到电脑桌面，双击即可打开**。无需安装 Python，正常使用无需管理员权限。
3. 看到“已开启”后，切到记事本等普通输入框，使用英文连续按 `m`、`j`。
4. 蜘蛛侠随机出现并播放音效，播放后自动消失；最小化控制窗口后仍然有效。

如果想把程序保存在其他文件夹，可右键 EXE 创建快捷方式，再把快捷方式放到桌面。当前版本需要手动放置文件或快捷方式，没有自动创建桌面图标或开机自启。

### 常用操作

| 操作 | 方法 |
| --- | --- |
| 立即触发 | 默认连续输入 `mj`，不用回车 |
| 回车触发 | 在控制窗口切换为回车模式 |
| 暂停 / 恢复 | 点击主按钮；暂停会清除当前动画和声音 |
| 静音 | 取消“播放声音” |
| 指定动画试听 | 点击两个试听按钮，暂停自动触发时也能试听 |
| 继续后台运行 | 最小化控制窗口 |
| 完全退出 | 关闭控制窗口，或输入 `stopmj` 后按回车 |

### 方式二：源码运行

安装 **Python 3.12 x64（包含 Tkinter）**，下载或克隆本仓库，在仓库根目录运行：

```powershell
python -m pip install -r requirements.txt
python "桌面特效/蜘蛛侠桌面特效.pyw"
```

[查看 Windows EXE 构建和检查方法 →](docs/windows.md)

## 📱 安卓手机使用方法

### 第一步：下载并安装

在手机浏览器 [下载 `MJ-Spiderman-Android-0.1.0.apk`](https://github.com/vakasu11/mj-spiderman-desktop-effect/releases/download/android-v0.1.0/MJ-Spiderman-Android-0.1.0.apk)，下载完成后点击安装。如果系统要求允许该来源安装应用，请核对下载来源后按提示处理。

### 第二步：开启服务

1. 打开 **“MJ 蜘蛛侠特效”**。
2. 点击 **“首次设置：开启无障碍服务”**，阅读权限说明。
3. 选择 **“了解，前往设置”**，在系统无障碍设置中手动开启同名服务。
4. 返回应用，确认显示 **“已开启 · 输入 mj 即可触发”**。

### 第三步：试听并测试

1. 分别点击 **“试听倒挂版”** 和 **“试听荡蛛丝版”**，检查画面和声音。
2. 在应用自己的测试框切换英文输入，输入并提交 `mj`。
3. 成功后再切换到其他应用的输入框试用。

主按钮控制 **暂停 / 开启特效**，声音开关控制静音。点击 **“停止并关闭无障碍服务”** 可完全停用；仅返回桌面或关闭应用界面不会主动停止服务。无需额外开启普通“显示在其他应用上层”权限。

> **安卓兼容范围：** 需要输入框提供文本变化事件，不能保证所有应用、游戏或输入法都能触发。尚未提交的拼音候选、自绘输入控件可能不可用；密码输入框会跳过，锁屏或熄屏时不会新建特效。

iQOO 手机若后台一段时间后失效，先返回应用检查服务状态，再查看系统的应用电池管理与后台限制。具体菜单名称随系统版本变化。

[查看安卓权限说明、更新和源码构建 →](android/README.md)

## 🎞️ 原项目演示

仓库保留了原作者的演示视频，可点击查看：

- [演示视频一](8月21日.mp4)
- [演示视频二](8月24日.mp4)

演示视频来自原项目，用于展示原版特效，不代表当前安卓版本已经通过真机验证。

## 📂 目录结构

```text
mj-spiderman-desktop-effect/
├── README.md                     # 项目介绍、两端下载与使用方法
├── requirements.txt              # Windows Python 依赖
├── build_windows.py              # Windows 单文件 EXE 构建脚本
├── 桌面特效/
│   ├── 蜘蛛侠桌面特效.pyw         # Windows 主程序与控制窗口
│   ├── trigger.py                # Windows 按键识别逻辑
│   ├── frames/                   # 原版倒挂动画帧
│   ├── frames2/                  # 荡蛛丝动画帧
│   ├── audio.wav                 # 特效一音效
│   ├── audio2.wav                # 特效二音效
│   ├── icon.ico                  # 原版图标
│   ├── 安装依赖.bat              # 源码运行辅助脚本
│   └── 启动特效.bat              # 源码启动脚本
├── android/
│   ├── README.md                 # 安卓权限、使用和构建说明
│   ├── AndroidManifest.xml       # 应用与无障碍服务声明
│   ├── src/                      # 原生 Java 界面、识别和特效逻辑
│   ├── res/                      # 界面资源与服务配置
│   ├── tests/                    # 安卓文字匹配检查
│   └── build_apk.py              # APK 构建与签名校验脚本
├── tests/                        # Windows 触发逻辑测试
├── docs/windows.md               # Windows 构建与检查说明
├── 8月21日.mp4                   # 原项目演示
└── 8月24日.mp4                   # 原项目演示
```

## 💡 使用提示

- **Windows 识别实际按键，粘贴 `mj` 不触发。** 两键间隔超过 3 秒、使用导航键或切换前台窗口会重置识别；Ctrl / Alt / Win 组合键不触发。
- 中文输入法中实际按 m、j 也可能触发 Windows 特效，建议英文输入；输入仍会正常进入当前应用。
- Windows 动画显示在主屏顶部；管理员窗口、安全桌面和部分独占全屏游戏不保证支持。
- Windows 输入 `stopmj` 后回车可退出，关闭控制窗口也会退出。
- 两端独立运行，目前均无应用内自动更新。

## 🔄 更新方法

| 平台 | 如何更新 |
| --- | --- |
| Windows | 完全退出旧程序，从 Windows 发布页下载新版 EXE，替换旧文件。设置保存在 `%LOCALAPPDATA%\MjSpidermanEffect`，替换 EXE 不会主动删除设置 |
| Android | 从 Android 发布页下载新 APK，尝试覆盖安装。需要同一包名、相同签名与符合系统要求的版本号；请勿先卸载，以免清除设置。更新后重新检查无障碍服务和后台限制 |

当前下载版本为 **Windows 1.0.0 / Android 0.1.0 试用版**。本次更新完善项目介绍与使用说明。

## ❓ 常见问题

| 问题 | 排查方法 |
| --- | --- |
| 下载后找不到程序 | 到 Releases 的 Assets 下载 EXE 或 APK；`Source code` 是源码 |
| Windows 输入 mj 没反应 | 检查是否暂停或切到回车模式；先试听，再在记事本用英文实际按 m、j |
| 安卓显示“尚未连接” | 按首次设置步骤手动开启系统无障碍服务，再返回应用 |
| 安卓试听正常，其他应用不触发 | 先测试本应用输入框并提交英文 mj；目标输入框可能没有可用的文本变化事件 |
| 安卓后台失效 | 检查无障碍服务是否开启，以及系统应用电池 / 后台限制 |
| 有画面没有声音 | 检查应用声音开关、系统音量及音频输出设备；安卓音效仍需真机确认 |

仍有问题，可到 [Issues](https://github.com/vakasu11/mj-spiderman-desktop-effect/issues) 提供系统与应用版本、试听结果、应用内输入结果和复现步骤，请勿提交密码或私人输入内容。

<details>
<summary><strong>🔒 隐私与权限说明</strong></summary>

Windows 不联网、不记录或上传输入文字，内存识别缓冲最多 8 个字符。设置与不含键入内容的诊断日志位于 `%LOCALAPPDATA%\MjSpidermanEffect`。

安卓无障碍服务具有读取其他应用控件文字的能力，需要你阅读说明并手动授权。应用不存储或上传输入文本，未声明联网、存储、麦克风、通知或普通悬浮窗权限。保存开关偏好，音效文件放在应用私有缓存。

</details>

<details>
<summary><strong>🧪 已完成的检查与验证范围</strong></summary>

- **Windows：** 12 项触发逻辑测试通过；源码与最终 EXE 的两套动画播放、自动退出、实际 mj 触发、暂停和退出口令已检查。已在 Windows 11 x64 运行；未独立验证扬声器实际听感，未覆盖全部多屏、高 DPI、管理员窗口和游戏组合。
- **Android：** 14 项文字匹配检查、编译、签名、对齐和原始素材完整性检查通过。**尚未在真机或模拟器运行验收**，画面、声音、输入法兼容与后台运行需要设备确认。

</details>

## 🙏 原作与致谢

本项目基于 [f20880479-lab/mj-spiderman-desktop-effect](https://github.com/f20880479-lab/mj-spiderman-desktop-effect) 二次开发，保留原作者提交历史与两套动画、音效、图标和演示视频。新增 Windows 即时触发、回车模式切换、暂停、静音、试听与设置保存，以及 Android 试用版。

[未修改的原版基准提交](https://github.com/vakasu11/mj-spiderman-desktop-effect/tree/688b8a00bd2d3ccc55d7407494c5ec6220ac0991)。原作者素材不声明为本项目原创。

---

<div align="center">

⭐ 如果喜欢这个项目，欢迎点个 Star！

</div>

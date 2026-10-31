# 蜘蛛侠桌面特效 · Windows 一键版

启动后连续输入 `mj`，原版蜘蛛侠动画立刻出现在主屏顶部，带声音，播放后自动消失。最小化控制窗口后仍可触发；关闭窗口退出。

基于 [f20880479-lab/mj-spiderman-desktop-effect](https://github.com/f20880479-lab/mj-spiderman-desktop-effect) 的 Fork。原作两套动画、音效、图标与作者提交历史均予以保留。本次升级添加即时触发、回车模式切换、静音、指定版本试听、设置保存，以及 64 位 Win32 调用修正。

## 使用

- 默认：连续按 `m`、`j`，不用回车；可切换为回车触发。
- 点击主按钮暂停／恢复；暂停会立即清除当前动画和声音。
- “播放声音”复选框控制静音，两种特效可分别试听（暂停自动触发时也可试听）。
- `stopmj` + 回车退出；退出口令不会先播放动画。
- 关闭控制窗口退出，最小化则继续运行。没有自动添加开机启动。

程序面向 Windows 10/11 x64，已在 Windows 11 x64 验证。检测实际按键而非输入框内容或粘贴；中文输入法中按 m、j 也可能触发，建议英文输入。组合键 Ctrl/Alt/Win 不触发；按键间隔超过 3 秒、导航键或切换前台窗口会重置识别。输入不会被拦截或删除。管理员窗口、安全桌面和部分全屏游戏不保证支持。

程序不联网，不记录或上传输入文字；最多保留 8 个字符的内存识别缓冲。配置与不含键入内容的诊断日志保存到 `%LOCALAPPDATA%\MjSpidermanEffect`。

## 源码运行

安装 Python 3.12 x64（包含 Tkinter），在仓库根目录运行：

```powershell
python -m pip install -r requirements.txt
python "桌面特效/蜘蛛侠桌面特效.pyw"
```

## 构建单文件 EXE

```powershell
python -m pip install -r requirements.txt
python -m pip install pyinstaller==6.22.3
python build_windows.py
```

生成 `dist/MJ-Spiderman.exe`，包含 Python 运行库与全部动画素材，可单独复制到其他 Windows x64 电脑。仓库不直接提交 EXE；本次本地交付包包含已构建版本。

## 检查

```powershell
python -m unittest discover -s tests -v
python "桌面特效/蜘蛛侠桌面特效.pyw" --smoke-test
```

第二条会打开窗口，依次播放两种特效并自动退出；最终 EXE 同样支持 `--smoke-test`。运行检查应同时确认诊断日志中没有异常。

2026-10-31：12 项触发逻辑测试通过；源码及最终 EXE 两套特效播放、自动退出通过；EXE 实际按键即时触发、暂停与退出口令已检查。声音调用已执行，未独立验证扬声器实际听感。未覆盖多显示器、高 DPI 全组合、管理员窗口和全屏游戏。

## 原版

未修改的原版基准为提交 `688b8a00bd2d3ccc55d7407494c5ec6220ac0991`，可从 Git 历史恢复。

## Android 试用版

已新增面向 iQOO 15 / Android 16 的 Android 11+ 试用版，包含无障碍文本识别、透明悬浮特效、暂停和试听。已经完成编译、签名和素材检查，尚未经过真机或模拟器运行验收。安装与权限说明、构建步骤见 [android/README.md](android/README.md)。Android 识别输入框文本，Windows 识别实际按键，两端独立运行。

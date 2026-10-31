# Windows 源码运行与构建

[返回下载和使用指南](../README.md)

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

生成 `dist/MJ-Spiderman.exe`，包含 Python 运行库与全部动画素材，可单独复制到其他 Windows x64 电脑。已构建的 EXE 和 ZIP 使用包可从 GitHub Releases 下载。

## 检查

```powershell
python -m unittest discover -s tests -v
python "桌面特效/蜘蛛侠桌面特效.pyw" --smoke-test
```

第二条会打开窗口，依次播放两种特效并自动退出；最终 EXE 同样支持 `--smoke-test`。运行检查应同时确认诊断日志中没有异常。

2026-10-31：12 项触发逻辑测试通过；源码及最终 EXE 两套特效播放、自动退出通过；EXE 实际按键即时触发、暂停与退出口令已检查。声音调用已执行，未独立验证扬声器实际听感。未覆盖多显示器、高 DPI 全组合、管理员窗口和全屏游戏。


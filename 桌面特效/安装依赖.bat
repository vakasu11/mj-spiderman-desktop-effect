@echo off
chcp 65001 >nul
cd /d "%~dp0"
python -m pip install -r "..\requirements.txt"
if errorlevel 1 (
    echo 安装失败，请检查 Python 和网络后重试。
    pause
    exit /b 1
)
echo 安装完成！双击启动特效.bat 即可使用。
pause

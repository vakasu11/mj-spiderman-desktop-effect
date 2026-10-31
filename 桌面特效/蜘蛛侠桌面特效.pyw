# -*- coding: utf-8 -*-
"""
蜘蛛侠桌面特效（窗口版）
- 主控制窗口（深色面板）：特效开关按钮；关闭窗口即退出程序（无论开关状态）
- 全局监听键盘：默认输入 mj 立即触发，可切换回车模式； → 屏幕正上方播放透明蜘蛛侠（带声音）
- 双特效随机：内置两个蜘蛛侠动画（原版倒挂 / 荡蛛丝版），每次触发随机抽取一个播放
- 持续生效：应用常驻，无论触发多少次都重播；播放中再触发会先清掉旧画面声音再从头播
- 透明实现：Win32 UpdateLayeredWindow（逐像素 alpha），参考 desk-pet 项目方案
"""
import os, sys, time, queue, random, ctypes, json, traceback
from pathlib import Path
from trigger import TriggerMatcher
from ctypes import wintypes
import tkinter as tk
import winsound
import numpy as np
from PIL import Image

# ===== DPI awareness 必须在创建任何 Tk 窗口之前设置 =====
try:
    ctypes.windll.shcore.SetProcessDpiAwareness(2)   # PER_MONITOR_DPI_AWARE
except Exception:
    try:
        ctypes.windll.user32.SetProcessDPIAware()
    except Exception:
        pass

try:
    from pynput import keyboard
except ImportError:
    ctypes.windll.user32.MessageBoxW(0, '缺少 pynput，请先双击运行「安装依赖.bat」', '蜘蛛侠特效', 0x40)
    sys.exit(1)

# 单实例保护
ctypes.windll.kernel32.CreateMutexW.argtypes = [ctypes.c_void_p, wintypes.BOOL, wintypes.LPCWSTR]
ctypes.windll.kernel32.CreateMutexW.restype = wintypes.HANDLE
_mutex = ctypes.windll.kernel32.CreateMutexW(None, False, 'SpidermanMjEffect')
if ctypes.windll.kernel32.GetLastError() == 183:
    sys.exit(0)

if getattr(sys, 'frozen', False):
    HERE = getattr(sys, '_MEIPASS', os.path.dirname(sys.executable))   # 素材所在（临时解压）
    LOG = os.path.join(os.path.dirname(sys.executable), 'effect.log')  # 日志放 exe 旁（持久）
else:
    HERE = os.path.dirname(os.path.abspath(__file__))
    LOG = os.path.join(HERE, 'effect.log')

DATA_DIR = Path(os.environ.get('MJ_EFFECT_DATA_DIR', str(Path(os.environ.get('LOCALAPPDATA', str(Path.home()))) / 'MjSpidermanEffect')))
DATA_DIR.mkdir(parents=True, exist_ok=True)
LOG = str(DATA_DIR / 'effect.log')
CONFIG = DATA_DIR / 'settings.json'
try:
    settings = json.loads(CONFIG.read_text(encoding='utf-8'))
    if not isinstance(settings, dict):
        settings = {}
except (OSError, ValueError):
    settings = {}
matcher = TriggerMatcher('enter' if settings.get('mode') == 'enter' else 'instant')
sound_enabled = settings.get('sound', True) is not False
ICON = os.path.join(HERE, 'icon.ico')
TRIGGER = 'mj'
QUIT_WORD = 'stopmj'

# ===== 特效库：每次输入 mj 从里面随机抽取一个播放 =====
EFFECTS = [
    # 原版：倒挂 Q 版蜘蛛侠（8月21日.mp4 抠图，60fps，166 帧）
    dict(name='原版蜘蛛侠', dir=os.path.join(HERE, 'frames'),
         first=12, count=166, fps=60, cell=(434, 720),
         wav=os.path.join(HERE, 'audio.wav')),
    # 新版：荡蛛丝 Q 版蜘蛛侠（8月24日.mp4 抠图，30fps，80 帧，抖音水印已去除；
    # 帧已统一缩放到 720x720，与原版人物同高，两个特效视觉大小一致）
    dict(name='荡蛛丝蜘蛛侠', dir=os.path.join(HERE, 'frames2'),
         first=4, count=80, fps=30, cell=(720, 720),
         wav=os.path.join(HERE, 'audio2.wav')),
]

# 主窗口配色（深色蜘蛛侠主题）
BG = '#14171f'
PANEL = '#1d2330'
FG = '#f2f4f8'
MUTED = '#8b93a5'
ACCENT = '#fe2c55'      # 蜘蛛侠红
GREEN = '#2bd97c'
GRAY = '#3a4150'

# ---------------- Win32 ----------------
user32 = ctypes.windll.user32
gdi32 = ctypes.windll.gdi32
# Explicit pointer-sized Win32 signatures are required on 64-bit Windows.
user32.GetDC.argtypes = [wintypes.HWND]
user32.GetDC.restype = wintypes.HDC
user32.ReleaseDC.argtypes = [wintypes.HWND, wintypes.HDC]
user32.GetForegroundWindow.restype = wintypes.HWND
user32.GetWindowLongW.argtypes = [wintypes.HWND, ctypes.c_int]
user32.SetWindowLongW.argtypes = [wintypes.HWND, ctypes.c_int, wintypes.LONG]
user32.ShowWindow.argtypes = [wintypes.HWND, ctypes.c_int]
user32.SetWindowPos.argtypes = [wintypes.HWND, wintypes.HWND, ctypes.c_int, ctypes.c_int, ctypes.c_int, ctypes.c_int, wintypes.UINT]
gdi32.CreateCompatibleDC.argtypes = [wintypes.HDC]
gdi32.CreateCompatibleDC.restype = wintypes.HDC
gdi32.CreateDIBSection.argtypes = [wintypes.HDC, ctypes.c_void_p, wintypes.UINT, ctypes.POINTER(ctypes.c_void_p), wintypes.HANDLE, wintypes.DWORD]
gdi32.CreateDIBSection.restype = wintypes.HBITMAP
gdi32.SelectObject.argtypes = [wintypes.HDC, wintypes.HANDLE]
gdi32.SelectObject.restype = wintypes.HANDLE
gdi32.DeleteObject.argtypes = [wintypes.HANDLE]
gdi32.DeleteDC.argtypes = [wintypes.HDC]
user32.UpdateLayeredWindow.argtypes = [wintypes.HWND, wintypes.HDC, ctypes.c_void_p, ctypes.c_void_p, wintypes.HDC, ctypes.c_void_p, wintypes.DWORD, ctypes.c_void_p, wintypes.DWORD]
user32.UpdateLayeredWindow.restype = wintypes.BOOL
GWL_EXSTYLE = -20
WS_EX_LAYERED = 0x00080000
WS_EX_TRANSPARENT = 0x00000020
WS_EX_TOPMOST = 0x00000008
SW_SHOWNOACTIVATE = 4
ULW_ALPHA = 0x00000002
AC_SRC_OVER = 0x00
AC_SRC_ALPHA = 0x01
SWP_NOSIZE = 0x0001
SWP_NOMOVE = 0x0002
SWP_NOACTIVATE = 0x0010
SWP_SHOWWINDOW = 0x0040


class BITMAPINFOHEADER(ctypes.Structure):
    _fields_ = [
        ('biSize', wintypes.DWORD), ('biWidth', wintypes.LONG), ('biHeight', wintypes.LONG),
        ('biPlanes', wintypes.WORD), ('biBitCount', wintypes.WORD), ('biCompression', wintypes.DWORD),
        ('biSizeImage', wintypes.DWORD), ('biXPelsPerMeter', wintypes.LONG), ('biYPelsPerMeter', wintypes.LONG),
        ('biClrUsed', wintypes.DWORD), ('biClrImportant', wintypes.DWORD),
    ]


class BITMAPINFO(ctypes.Structure):
    _fields_ = [('bmiHeader', BITMAPINFOHEADER)]


class POINT(ctypes.Structure):
    _fields_ = [('x', wintypes.LONG), ('y', wintypes.LONG)]


class SIZE(ctypes.Structure):
    _fields_ = [('cx', wintypes.LONG), ('cy', wintypes.LONG)]


class BLENDFUNCTION(ctypes.Structure):
    _fields_ = [('BlendOp', wintypes.BYTE), ('BlendFlags', wintypes.BYTE),
                ('SourceConstantAlpha', wintypes.BYTE), ('AlphaFormat', wintypes.BYTE)]


def make_premultiplied_bgra(img):
    arr = np.asarray(img, dtype=np.uint16)
    a = arr[..., 3:4]
    premul = (arr[..., 0:3] * a + 127) // 255
    out = np.empty(arr.shape[:2] + (4,), dtype=np.uint8)
    out[..., 0] = premul[..., 2]
    out[..., 1] = premul[..., 1]
    out[..., 2] = premul[..., 0]
    out[..., 3] = arr[..., 3]
    return out


class LayeredWindow:
    """UpdateLayeredWindow 逐像素 alpha 透明窗口（点击穿透 + 置顶）"""

    def __init__(self, hwnd):
        self.hwnd = hwnd
        style = user32.GetWindowLongW(hwnd, GWL_EXSTYLE)
        user32.SetWindowLongW(hwnd, GWL_EXSTYLE, style | WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_TOPMOST)
        user32.ShowWindow(hwnd, SW_SHOWNOACTIVATE)
        self._screen_dc = user32.GetDC(0)
        self._mem_dc = gdi32.CreateCompatibleDC(self._screen_dc)
        self._bitmap = None
        self._old_bitmap = None
        self._cap_w = 0
        self._cap_h = 0
        self._bits = None

    def update(self, img, x, y):
        w, h = img.size
        if self._bitmap is None or w > self._cap_w or h > self._cap_h:
            if self._old_bitmap:
                gdi32.SelectObject(self._mem_dc, self._old_bitmap)
            if self._bitmap:
                gdi32.DeleteObject(self._bitmap)
            self._cap_w = max(w, self._cap_w)
            self._cap_h = max(h, self._cap_h)
            bmi = BITMAPINFO()
            bmi.bmiHeader.biSize = ctypes.sizeof(BITMAPINFOHEADER)
            bmi.bmiHeader.biWidth = self._cap_w
            bmi.bmiHeader.biHeight = -self._cap_h
            bmi.bmiHeader.biPlanes = 1
            bmi.bmiHeader.biBitCount = 32
            bmi.bmiHeader.biCompression = 0
            bmi.bmiHeader.biSizeImage = self._cap_w * self._cap_h * 4
            bits = ctypes.c_void_p()
            self._bitmap = gdi32.CreateDIBSection(self._mem_dc, ctypes.byref(bmi), 0, ctypes.byref(bits), None, 0)
            if not self._bitmap:
                raise OSError('CreateDIBSection failed')
            self._old_bitmap = gdi32.SelectObject(self._mem_dc, self._bitmap)
            self._bits = bits
        data = make_premultiplied_bgra(img)
        ctypes.memset(self._bits, 0, self._cap_w * self._cap_h * 4)
        row_bytes = w * 4
        src = data.tobytes()
        stride = self._cap_w * 4
        for row in range(h):
            dst = ctypes.c_void_p(self._bits.value + row * stride)
            ctypes.memmove(dst, src[row * row_bytes:(row + 1) * row_bytes], row_bytes)
        blend = BLENDFUNCTION(AC_SRC_OVER, 0, 255, AC_SRC_ALPHA)
        pt_src = POINT(0, 0)
        pt_dst = POINT(x, y)
        size = SIZE(w, h)
        ok = user32.UpdateLayeredWindow(self.hwnd, self._screen_dc, ctypes.byref(pt_dst),
                                        ctypes.byref(size), self._mem_dc, ctypes.byref(pt_src),
                                        0, ctypes.byref(blend), ULW_ALPHA)
        if not ok:
            raise OSError('UpdateLayeredWindow failed: %d' % ctypes.get_last_error())

    def close(self):
        if self._old_bitmap:
            gdi32.SelectObject(self._mem_dc, self._old_bitmap)
        if self._bitmap:
            gdi32.DeleteObject(self._bitmap)
        if self._mem_dc:
            gdi32.DeleteDC(self._mem_dc)
        if self._screen_dc:
            user32.ReleaseDC(0, self._screen_dc)


# ---------------- 全局状态 ----------------
q = queue.Queue()
typed = ''
anim_id = 0
enabled_flag = True   # 特效开关（普通布尔，pynput 线程安全读取）


def log(msg):
    try:
        with open(LOG, 'a', encoding='utf-8') as f:
            f.write(time.strftime('%Y-%m-%d %H:%M:%S') + '  ' + msg + '\n')
    except Exception:
        pass


pressed_keys = set()


def on_press(key):
    # Ignore auto-repeat, shortcuts and navigation; callbacks only enqueue work.
    if key in pressed_keys:
        return
    pressed_keys.add(key)
    if any(user32.GetAsyncKeyState(vk) & 0x8000 for vk in (0x11, 0x12, 0x5B, 0x5C)):
        matcher.reset()
        return
    ch = getattr(key, 'char', None)
    if ch is None:
        if key in (keyboard.Key.shift, keyboard.Key.shift_l, keyboard.Key.shift_r):
            return
        ch = 'enter' if key == keyboard.Key.enter else 'backspace' if key == keyboard.Key.backspace else 'reset'
    action = matcher.feed(ch, enabled=enabled_flag, context=user32.GetForegroundWindow())
    if action:
        q.put(action)


def on_release(key):
    pressed_keys.discard(key)


# ================ 窗口 ================
root = tk.Tk()
sw, sh = root.winfo_screenwidth(), root.winfo_screenheight()

# 缩放：小屏幕缩小特效帧；蜘蛛侠出现在屏幕正上方（按每个特效自己的尺寸计算）
def effect_geometry(eff):
    cw, ch = eff['cell']
    sc = min(1.0, max(0.2, (sh - 80) / ch), max(0.2, (sw - 40) / cw))
    fw, fh = max(1, int(cw * sc)), max(1, int(ch * sc))
    return fw, fh, (sw - fw) // 2, 20

FW, FH, WIN_X, WIN_Y = effect_geometry(EFFECTS[0])

# ---- 特效窗口（UpdateLayeredWindow，透明）----
win = tk.Toplevel(root)
win.overrideredirect(True)
win.attributes('-topmost', True)
win.geometry('%dx%d+%d+%d' % (FW, FH, WIN_X, WIN_Y))
win.update_idletasks()
win.update()
hwnd = int(win.wm_frame(), 16) if isinstance(win.wm_frame(), str) else win.wm_frame()
layered = LayeredWindow(hwnd)
layered.update(Image.new('RGBA', (FW, FH), (0, 0, 0, 0)), WIN_X, WIN_Y)


def force_topmost():
    try:
        ctypes.windll.user32.SetWindowPos(hwnd, -1, 0, 0, 0, 0,
                                          SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE | SWP_SHOWWINDOW)
    except Exception:
        pass


def show_effect(eff):
    """播放一个特效（eff 来自 EFFECTS，每次触发随机抽取）"""
    global anim_id
    anim_id += 1
    my_id = anim_id
    fw, fh, wx, wy = effect_geometry(eff)
    blank = Image.new('RGBA', (fw, fh), (0, 0, 0, 0))
    log('effect trigger: %s' % eff['name'])
    layered.update(blank, wx, wy)
    try:
        winsound.PlaySound(None, winsound.SND_PURGE)
    except Exception:
        pass
    force_topmost()
    try:
        if sound_enabled:
            winsound.PlaySound(eff['wav'], winsound.SND_FILENAME | winsound.SND_ASYNC)
    except Exception:
        pass
    start = time.monotonic()
    last_frame = -1

    def step():
        nonlocal last_frame
        if my_id != anim_id:
            return
        i = int((time.monotonic() - start) * eff['fps'])
        if i >= eff['count']:
            layered.update(blank, wx, wy)
            return
        if i != last_frame:
            last_frame = i
            try:
                path = os.path.join(eff['dir'], 'f%04d.png' % (eff['first'] + i))
                img = Image.open(path).convert('RGBA')
                if (fw, fh) != eff['cell']:
                    img = img.resize((fw, fh), Image.Resampling.BILINEAR)
                layered.update(img, wx, wy)
            except Exception as e:
                log('frame %d: %s' % (i, e))
                layered.update(blank, wx, wy)
                return
        root.after(8, step)

    step()


def poll():
    try:
        while True:
            m = q.get_nowait()
            if m == 'go' and enabled_flag:
                show_effect(random.choice(EFFECTS))
            elif m == 'quit':
                on_close()
                return
    except queue.Empty:
        pass
    root.after(50, poll)


# ---- 主控制窗口（深色美化）----
def on_close():
    """关闭窗口（X）即退出程序，无论开关状态"""
    log('exit by window close')
    winsound.PlaySound(None, winsound.SND_PURGE)
    try:
        listener.stop()
    except Exception:
        pass
    try:
        layered.close()
    except Exception:
        pass
    root.quit()
    root.destroy()


root.title('蜘蛛侠特效 · MJ')
root.configure(bg=BG)
root.resizable(False, False)
root.protocol('WM_DELETE_WINDOW', on_close)
try:
    root.iconbitmap(ICON)
except Exception:
    pass

def save_settings():
    try:
        CONFIG.write_text(json.dumps({'mode': matcher.mode, 'sound': sound_enabled}), encoding='utf-8')
    except OSError:
        status.config(text='设置暂时无法保存，本次仍可正常使用')

def update_status():
    btn.config(text='已开启 · 点击暂停' if enabled_flag else '已暂停 · 点击开启',
               bg=GREEN if enabled_flag else GRAY,
               fg='#08211a' if enabled_flag else FG)
    status.config(text=('输入 mj 即刻触发' if matcher.mode == 'instant' else '输入 mj 后按回车触发')
                  if enabled_flag else '已暂停，特效不会自动触发')

def clear_effect():
    global anim_id
    anim_id += 1
    winsound.PlaySound(None, winsound.SND_PURGE)
    layered.update(Image.new('RGBA', (layered._cap_w, layered._cap_h)), WIN_X, WIN_Y)

def toggle():
    global enabled_flag
    enabled_flag = not enabled_flag
    matcher.reset()
    if not enabled_flag:
        clear_effect()
    update_status()

def change_mode():
    matcher.mode = mode_var.get()
    matcher.reset()
    update_status()
    save_settings()

def change_sound():
    global sound_enabled
    sound_enabled = sound_var.get()
    if not sound_enabled:
        winsound.PlaySound(None, winsound.SND_PURGE)
    save_settings()

body = tk.Frame(root, bg=BG, padx=32, pady=24)
body.pack(fill='both', expand=True)
tk.Label(body, text='蜘蛛侠特效', bg=BG, fg=FG, font=('Microsoft YaHei', 20, 'bold')).pack(anchor='w')
tk.Label(body, text='MJ  /  两套原版动画随机播放', bg=BG, fg='#a8b1c2', font=('Microsoft YaHei', 10)).pack(anchor='w', pady=(6, 20))
btn = tk.Button(body, command=toggle, font=('Microsoft YaHei', 13, 'bold'), bd=0,
                padx=20, pady=12, activebackground='#a1edc1', cursor='hand2')
btn.pack(fill='x')
status = tk.Label(body, bg=BG, fg=FG, font=('Microsoft YaHei', 10))
status.pack(pady=(10, 18))
mode_var = tk.StringVar(value=matcher.mode)
for label, value in [('输入 mj 立即触发', 'instant'), ('输入 mj 后按回车触发', 'enter')]:
    tk.Radiobutton(body, text=label, variable=mode_var, value=value, command=change_mode,
                   bg=BG, fg=FG, selectcolor=PANEL, activebackground=BG, activeforeground=FG,
                   font=('Microsoft YaHei', 11), pady=3).pack(anchor='w')
sound_var = tk.BooleanVar(value=sound_enabled)
tk.Checkbutton(body, text='播放声音', variable=sound_var, command=change_sound,
               bg=BG, fg=FG, selectcolor=PANEL, activebackground=BG, activeforeground=FG,
               font=('Microsoft YaHei', 11)).pack(anchor='w', pady=(8, 14))
previews = tk.Frame(body, bg=BG)
previews.pack(fill='x')
for i, label in enumerate(['试听倒挂版', '试听荡蛛丝版']):
    tk.Button(previews, text=label, command=lambda i=i: show_effect(EFFECTS[i]),
              bg=PANEL, fg=FG, activebackground=GRAY, activeforeground=FG,
              font=('Microsoft YaHei', 10), bd=0, padx=14, pady=10,
              cursor='hand2').pack(side='left', expand=True, fill='x', padx=(0, 8) if i == 0 else 0)
tk.Label(body, text='最小化后仍可触发；关闭窗口即可退出。\n英文输入 mj；退出口令：stopmj + 回车。',
         bg=BG, fg='#a8b1c2', justify='left', font=('Microsoft YaHei', 9)).pack(anchor='w', pady=(18, 0))
update_status()
root.update_idletasks()
cw, ch = max(420, root.winfo_reqwidth()), root.winfo_reqheight()
root.geometry('%dx%d+%d+%d' % (cw, ch, max(0, (sw-cw)//2), max(0, (sh-ch)//2)))

def report_error(exc, value, tb):
    log(''.join(traceback.format_exception(exc, value, tb)))
    status.config(text='播放遇到问题，请重新打开程序')
root.report_callback_exception = report_error

# ---------------- 启动 ----------------
listener = keyboard.Listener(on_press=on_press, on_release=on_release)
listener.daemon = True
listener.start()
log('app started')
if '--test' in sys.argv:
    # --test 随机触发一次；--test 0 / --test 1 强制指定特效（便于逐一验证）
    idx = None
    try:
        p = sys.argv.index('--test')
        if p + 1 < len(sys.argv) and sys.argv[p + 1] in ('0', '1'):
            idx = int(sys.argv[p + 1])
    except ValueError:
        pass
    eff = EFFECTS[idx] if idx is not None else random.choice(EFFECTS)
    root.after(2500, lambda: show_effect(eff))
if '--smoke-test' in sys.argv:
    root.after(500, lambda: show_effect(EFFECTS[0]))
    root.after(4000, lambda: show_effect(EFFECTS[1]))
    root.after(7500, on_close)
root.after(50, poll)
root.mainloop()
log('app exited')

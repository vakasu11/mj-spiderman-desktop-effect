package com.vakasu11.mjspiderman;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.*;
import android.graphics.*;
import android.media.MediaPlayer;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import android.widget.Toast;
import java.io.*;
import java.util.Random;
import java.util.concurrent.*;

public final class MjService extends AccessibilityService {
    static volatile MjService current;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private final TextTrigger trigger = new TextTrigger();
    private final Random random = new Random();
    private SharedPreferences prefs;
    private WindowManager windows;
    private EffectView overlay;
    private MediaPlayer player;
    private volatile long generation;
    private final int[] first = {12, 4}, count = {166, 80}, fps = {60, 30}, width = {434, 720};

    @Override protected void onServiceConnected() {
        prefs = getSharedPreferences("mj", MODE_PRIVATE);
        if (!prefs.getBoolean("disclosed", false)) { disableSelf(); return; }
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);
        current = this;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (prefs == null || !prefs.getBoolean("enabled", false)) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) { trigger.reset(); return; }
        if (event.isPassword()) return;
        AccessibilityNodeInfo source = event.getSource();
        if (source == null || !source.isEditable() || source.isPassword()) return;
        if (!event.getText().isEmpty() && trigger.matches(event.getText().get(0),
                event.getFromIndex(), event.getAddedCount(), false, true,
                event.getWindowId(), event.getEventTime())) preview(random.nextInt(2));
    }

    void settingsChanged() {
        trigger.reset();
        if (!prefs.getBoolean("enabled", false)) stopEffect();
        if (!prefs.getBoolean("sound", true)) stopSound();
    }

    void turnOff() {
        stopEffect();
        current = null;
        disableSelf();
    }

    void preview(int index) {
        KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (keyguard.isKeyguardLocked() || !power.isInteractive() || windows == null) return;
        stopEffect();
        final long token = ++generation;
        overlay = new EffectView(this);
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        Rect bounds = new Rect(0, 0, metrics.widthPixels, metrics.heightPixels);
        int h = Math.min(Math.round(240 * getResources().getDisplayMetrics().density), bounds.height()/2);
        h = Math.min(h, bounds.width() * 720 / width[index]);
        int w = Math.round(h * width[index] / 720f);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(w, h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = Math.round(48 * getResources().getDisplayMetrics().density);
        try {
            windows.addView(overlay, params);
        } catch (RuntimeException error) {
            overlay = null;
            Toast.makeText(this, "系统暂时无法显示特效，请重新开启服务", Toast.LENGTH_SHORT).show();
            return;
        }
        if (prefs.getBoolean("sound", true)) playSound(index);
        final long start = SystemClock.uptimeMillis();
        render(index, token, start, -1);
    }

    private void render(int index, long token, long start, int previous) {
        if (token != generation || overlay == null) return;
        int frame = (int) ((SystemClock.uptimeMillis()-start)*fps[index]/1000);
        if (frame >= count[index]) { stopEffect(); return; }
        if (frame == previous) {
            main.postDelayed(() -> render(index, token, start, previous), 8);
            return;
        }
        final int frameIndex = frame;
        decoder.execute(() -> {
            if (token != generation) return;
            Bitmap bitmap = null;
            String path = (index == 0 ? "frames/" : "frames2/")
                + String.format(java.util.Locale.ROOT, "f%04d.png", first[index]+frameIndex);
            try (InputStream input = getAssets().open(path)) {
                bitmap = BitmapFactory.decodeStream(input);
            } catch (IOException ignored) { }
            final Bitmap ready = bitmap;
            main.post(() -> {
                if (token != generation || overlay == null) { if (ready != null) ready.recycle(); return; }
                if (ready == null) {
                    stopEffect();
                    Toast.makeText(this, "动画素材未能读取，请重新安装", Toast.LENGTH_SHORT).show();
                    return;
                }
                overlay.setFrame(ready);
                main.postDelayed(() -> render(index, token, start, frameIndex), 8);
            });
        });
    }

    private void playSound(int index) {
        String name = index == 0 ? "audio.wav" : "audio2.wav";
        File file = new File(getCacheDir(), name);
        try {
            if (!file.exists()) {
                try (InputStream input = getAssets().open(name); OutputStream out = new FileOutputStream(file)) {
                    byte[] data = new byte[8192]; int n;
                    while ((n = input.read(data)) != -1) out.write(data, 0, n);
                }
            }
            player = new MediaPlayer();
            player.setDataSource(file.getAbsolutePath());
            player.setAudioAttributes(new android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.prepare(); player.start();
        } catch (IOException | RuntimeException error) {
            stopSound();
            Toast.makeText(this, "音效未能播放，动画仍可使用", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopSound() {
        if (player != null) { player.release(); player = null; }
    }
    private void stopEffect() {
        ++generation;
        stopSound();
        if (overlay != null) {
            try { windows.removeViewImmediate(overlay); } catch (RuntimeException ignored) { }
            overlay.clear(); overlay = null;
        }
    }
    @Override public void onInterrupt() { stopEffect(); }
    @Override public void onDestroy() {
        stopEffect(); decoder.shutdownNow();
        if (current == this) current = null;
        super.onDestroy();
    }

    private static final class EffectView extends View {
        private Bitmap bitmap;
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        EffectView(Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        void setFrame(Bitmap frame) { bitmap = frame; invalidate(); }
        void clear() { bitmap = null; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (bitmap != null) canvas.drawBitmap(bitmap, null, new Rect(0,0,getWidth(),getHeight()), paint);
        }
    }
}

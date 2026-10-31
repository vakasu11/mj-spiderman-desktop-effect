package com.vakasu11.mjspiderman;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;

public final class MainActivity extends Activity {
    private final int bg = Color.rgb(20,23,31), fg = Color.rgb(242,244,248);
    private LinearLayout body;
    private TextView status;
    private Button toggle;
    private SharedPreferences prefs;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() { updateStatus(); handler.postDelayed(this, 1000); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("mj", MODE_PRIVATE);
        getWindow().getDecorView().setBackgroundColor(bg);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(24), dp(20), dp(24), dp(28));
        scroll.addView(body);
        setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        label("蜘蛛侠特效", 28, fg);
        label("iQOO 15 / Android 16 试用版", 14, Color.rgb(168,177,194));
        status = label("正在检查服务…", 16, fg);
        toggle = button("开启特效", () -> {
            if (MjService.current == null) { explainAndOpenSettings(); return; }
            boolean enabled = !prefs.getBoolean("enabled", false);
            prefs.edit().putBoolean("enabled", enabled).apply();
            MjService.current.settingsChanged();
            updateStatus();
        });
        button("首次设置：开启无障碍服务", this::explainAndOpenSettings);
        Switch sound = new Switch(this);
        sound.setText("播放声音"); sound.setTextColor(fg); sound.setTextSize(17);
        sound.setMinHeight(dp(52)); sound.setChecked(prefs.getBoolean("sound", true));
        sound.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean("sound", checked).apply();
            if (MjService.current != null) MjService.current.settingsChanged();
        });
        body.addView(sound);
        button("试听倒挂版", () -> preview(0));
        button("试听荡蛛丝版", () -> preview(1));
        label("在下方测试输入 mj", 17, fg);
        EditText test = new EditText(this);
        test.setSingleLine(true); test.setTextColor(fg); test.setHintTextColor(Color.LTGRAY);
        test.setTextSize(18); test.setHint("请切到英文输入，键入 mj"); test.setMinHeight(dp(56));
        test.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        body.addView(test);
        button("停止并关闭无障碍服务", () -> {
            prefs.edit().putBoolean("enabled", false).apply();
            if (MjService.current != null) MjService.current.turnOff();
            updateStatus();
        });
        label("本地识别，不联网、不保存输入文字。只在系统提供文本变化事件的输入框中工作；密码框会跳过。拼音候选栏尚未提交的 mj 可能无法触发。", 14, Color.rgb(168,177,194));
        label("若退出界面后失效：在 iQOO 系统的应用电池管理中检查是否限制本应用后台运行。菜单名称随系统版本变化。", 14, Color.rgb(168,177,194));
        label("本版已完成编译和签名检查，尚未在 iQOO 15 真机验收。", 14, Color.rgb(168,177,194));
    }

    private void explainAndOpenSettings() {
        new AlertDialog.Builder(this).setTitle("开启前请了解")
            .setMessage("无障碍服务可以读取其他应用输入控件中的文字。本应用仅在本机检查光标前的 mj，跳过密码框，不记录文字、不上传数据，也没有联网权限。\n\n同意后将打开系统设置，请手动选择“MJ 蜘蛛侠特效”并开启。该设置不是自动授权；你可以随时关闭服务。")
            .setNegativeButton("暂不开启", null)
            .setPositiveButton("了解，前往设置", (d,w) -> {
                prefs.edit().putBoolean("disclosed", true).putBoolean("enabled", true).apply();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }).show();
    }

    private void preview(int index) {
        if (MjService.current == null) {
            Toast.makeText(this, "请先手动开启无障碍服务", Toast.LENGTH_SHORT).show();
        } else MjService.current.preview(index);
    }
    private void updateStatus() {
        if (status == null) return;
        boolean connected = MjService.current != null;
        boolean enabled = prefs.getBoolean("enabled", false);
        status.setText(!connected ? "尚未连接：请先开启无障碍服务" : enabled ? "已开启 · 输入 mj 即可触发" : "已暂停 · 可点击试听");
        toggle.setText(!connected ? "开始设置" : enabled ? "暂停特效" : "开启特效");
    }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(0,dp(10),0,dp(12)); body.addView(view); return view;
    }
    private Button button(String text, Runnable action) {
        Button button = new Button(this); button.setText(text); button.setTextSize(16); button.setAllCaps(false);
        button.setMinHeight(dp(52)); button.setOnClickListener(v -> action.run());
        body.addView(button, new LinearLayout.LayoutParams(-1,-2)); return button;
    }
}

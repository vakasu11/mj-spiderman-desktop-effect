package com.vakasu11.mjspiderman;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;

/** Native controls, shared ink/red palette and no decorative UI animation. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(21,23,29), PANEL = Color.rgb(32,36,45);
    private static final int FG = Color.rgb(246,247,251), MUTED = Color.rgb(174,182,198);
    private static final int RED = Color.rgb(206,48,69), BORDER = Color.rgb(88,101,123);
    private static final int GREEN = Color.rgb(120,214,176);
    private LinearLayout body;
    private TextView status, stateLabel;
    private Button toggle;
    private SharedPreferences prefs;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() { updateStatus(); handler.postDelayed(this, 1000); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("mj-spiderman");
        prefs = getSharedPreferences("mj", MODE_PRIVATE);
        getWindow().getDecorView().setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        FrameLayout host = new FrameLayout(this);
        body = column(BG,20);
        int available = getResources().getConfiguration().screenWidthDp;
        FrameLayout.LayoutParams content = new FrameLayout.LayoutParams(dp(Math.min(available,600)), -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        host.addView(body,content);
        host.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,oright,ob) -> {
            int width=Math.min(r-l,dp(600));
            if(width>0&&body.getLayoutParams().width!=width){
                android.view.ViewGroup.LayoutParams p=body.getLayoutParams();p.width=width;body.setLayoutParams(p);
            }
        });
        scroll.addView(host);
        setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            android.graphics.Insets keyboard = insets.getInsets(WindowInsets.Type.ime());
            v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,keyboard.bottom));
            return WindowInsets.CONSUMED;
        });

        LinearLayout header = row();
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.brand_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(logo,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout brand = column(BG,0);
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(0,-2,1);
        brandParams.leftMargin=dp(8);
        header.addView(brand,brandParams);
        TextView title=text("mj-spiderman",24,FG,true);
        title.setAccessibilityHeading(true);
        brand.addView(title);
        brand.addView(text("输入两个字母，召唤蜘蛛侠",14,MUTED,false));
        add(body,header,0,20);

        LinearLayout hero=column(PANEL,20);
        hero.setBackground(shape(PANEL,18,0));
        stateLabel=text("正在检查服务…",14,FG,true);
        stateLabel.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        hero.addView(stateLabel);
        LinearLayout heroRow=row();
        LinearLayout keyRow=row();
        for(String ch:new String[]{"m","j"}) {
            TextView key=text(ch,34,FG,true);
            key.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);
            key.setGravity(Gravity.CENTER);
            key.setPadding(dp(14),dp(3),dp(14),dp(5));
            key.setBackground(shape(BG,9,BORDER));
            key.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams kp=new LinearLayout.LayoutParams(-2,-2);
            kp.rightMargin=dp(8);
            keyRow.addView(key,kp);
        }
        heroRow.addView(keyRow,new LinearLayout.LayoutParams(0,-2,1));
        ImageView mascot=new ImageView(this);
        mascot.setImageResource(R.drawable.brand_logo);
        mascot.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mascot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        heroRow.addView(mascot,new LinearLayout.LayoutParams(dp(76),dp(76)));
        add(hero,heroRow,8,4);
        status=text("",15,MUTED,false);
        hero.addView(status);
        add(body,hero,0,12);

        toggle=button("开始设置",true,() -> {
            if(MjService.current==null){explainAndOpenSettings();return;}
            boolean enabled=!prefs.getBoolean("enabled",false);
            prefs.edit().putBoolean("enabled",enabled).apply();
            MjService.current.settingsChanged();
            updateStatus();
        });
        add(body,toggle,0,12);
        add(body,button("无障碍服务设置",false,this::explainAndOpenSettings),0,24);

        LinearLayout soundRow=row();
        LinearLayout soundCopy=column(BG,0);
        soundCopy.addView(text("播放声音",17,FG,true));
        soundCopy.addView(text("关闭后仅播放动画",14,MUTED,false));
        soundRow.addView(soundCopy,new LinearLayout.LayoutParams(0,-2,1));
        Switch sound=new Switch(this);
        sound.setContentDescription("播放声音");
        sound.setMinWidth(dp(56));sound.setMinHeight(dp(48));
        sound.setThumbTintList(ColorStateList.valueOf(FG));
        sound.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{RED,BORDER}));
        sound.setChecked(prefs.getBoolean("sound",true));
        sound.setOnCheckedChangeListener((b,checked) -> {
            prefs.edit().putBoolean("sound",checked).apply();
            if(MjService.current!=null)MjService.current.settingsChanged();
        });
        soundRow.addView(sound);
        add(body,soundRow,0,24);

        heading("选一款，先试听");
        add(body,text("自动触发时，随机播放一款",14,MUTED,false),4,12);
        LinearLayout previews=row();
        for(int i=0;i<2;i++) {
            final int index=i;
            Button b=button(i==0?"倒挂版  试听":"荡蛛丝版  试听",false,() -> preview(index));
            b.setTextSize(15);
            Drawable d=getDrawable(i==0?R.drawable.preview1:R.drawable.preview2);
            d.setBounds(0,0,dp(76),dp(76));
            b.setCompoundDrawables(null,d,null,null);
            b.setCompoundDrawablePadding(dp(8));
            b.setContentDescription(i==0?"试听倒挂版":"试听荡蛛丝版");
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);
            if(i==0)bp.rightMargin=dp(10);
            previews.addView(b,bp);
        }
        add(body,previews,0,24);

        TextView testTitle=heading("试试输入 mj");
        EditText test=new EditText(this);
        test.setId(View.generateViewId());
        testTitle.setLabelFor(test.getId());
        test.setSingleLine(true);
        test.setTextColor(FG);test.setHintTextColor(MUTED);test.setTextSize(17);
        test.setHint("切换英文输入，键入 mj");
        test.setMinHeight(dp(56));
        test.setPadding(dp(16),dp(12),dp(16),dp(12));
        test.setBackground(focusShape());
        test.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        add(body,test,12,8);
        add(body,text("先在这里输入并提交英文 mj，成功后再到其他应用试用。",14,MUTED,false),0,24);
        add(body,button("停止并关闭无障碍服务",false,() -> {
            prefs.edit().putBoolean("enabled",false).apply();
            if(MjService.current!=null)MjService.current.turnOff();
            updateStatus();
        }),0,20);

        TextView notes=text("仅在提供文字变化事件的输入框中工作，密码框会跳过。后台失效时，请检查手机的应用电池管理与服务状态。",14,MUTED,false);
        add(body,notes,0,12);
        add(body,text("本地识别，不联网、不保存输入文字。",14,MUTED,false),0,12);
        add(body,text("0.2.0 试用版 · 尚未完成真机运行验收",13,MUTED,false),0,8);
        updateStatus();
    }

    private void explainAndOpenSettings() {
        new AlertDialog.Builder(this).setTitle("开启前请了解")
            .setMessage("无障碍服务可以读取其他应用输入控件中的文字。本应用仅在本机检查光标前的 mj，跳过密码框，不记录文字、不上传数据，也没有联网权限。\n\n同意后将打开系统设置，请手动选择“mj-spiderman”并开启。你可以随时关闭服务。")
            .setNegativeButton("暂不开启",null)
            .setPositiveButton("了解，前往设置",(d,w) -> {
                prefs.edit().putBoolean("disclosed",true).putBoolean("enabled",true).apply();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }).show();
    }
    private void preview(int index) {
        if(MjService.current==null)Toast.makeText(this,"请先手动开启无障碍服务",Toast.LENGTH_SHORT).show();
        else MjService.current.preview(index);
    }
    private void updateStatus() {
        if(status==null)return;
        boolean connected=MjService.current!=null;
        boolean enabled=connected&&prefs.getBoolean("enabled",false);
        String state=!connected?"服务尚未开启":enabled?"特效已开启":"特效已暂停";
        if(!state.contentEquals(stateLabel.getText()))stateLabel.setText(state);
        stateLabel.setTextColor(enabled?GREEN:MUTED);
        status.setText(!connected?"先开启服务，再输入 mj 召唤特效":enabled?"输入 mj，即可召唤蜘蛛侠":"随时开启，也可以先试听");
        toggle.setText(!connected?"开始设置":enabled?"暂停特效":"开启特效");
    }
    @Override protected void onResume(){super.onResume();handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(int color,int padding){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(color);l.setPadding(dp(padding),dp(padding),dp(padding),dp(padding));return l;
    }
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String copy,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(copy);v.setTextSize(size);v.setTextColor(color);
        v.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        v.setLineSpacing(dp(2),1.06f);return v;
    }
    private TextView heading(String copy){TextView t=text(copy,18,FG,true);t.setAccessibilityHeading(true);body.addView(t);return t;}
    private void add(LinearLayout parent,View child,int top,int bottom){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);p.bottomMargin=dp(bottom);parent.addView(child,p);
    }
    private GradientDrawable shape(int fill,int radius,int stroke){
        GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke);return d;
    }
    private StateListDrawable focusShape(){
        StateListDrawable d=new StateListDrawable();
        d.addState(new int[]{android.R.attr.state_focused},shape(PANEL,12,Color.rgb(233,145,156)));
        d.addState(new int[]{},shape(PANEL,12,BORDER));return d;
    }
    private Button button(String copy,boolean primary,Runnable action){
        Button b=new Button(this);b.setText(copy);b.setTextColor(FG);b.setTextSize(16);b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setMinHeight(dp(52));
        b.setMinimumWidth(0);b.setMinWidth(0);b.setPadding(dp(12),dp(12),dp(12),dp(12));
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},shape(primary?RED:PANEL,12,Color.rgb(233,145,156)));
        states.addState(new int[]{},shape(primary?RED:PANEL,12,primary?0:BORDER));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff),states,null));
        b.setOnClickListener(v -> action.run());return b;
    }
}

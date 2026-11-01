package com.vakasu11.mjspiderman;

import android.app.*;
import android.media.AudioManager;
import android.text.TextWatcher;
import android.text.Editable;
import android.net.Uri;
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
    private TextView status, stateLabel, soundHint, connectionHint;
    private Button toggle;
    private SharedPreferences prefs;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() { updateStatus(); handler.postDelayed(this, 1000); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ServiceStatus.installCrashRecorder(this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
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
            View focused=getCurrentFocus();
            if(keyboard.bottom>0&&focused instanceof EditText)v.post(()->focused.requestRectangleOnScreen(new android.graphics.Rect(0,0,focused.getWidth(),focused.getHeight()),true));
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
            MjService service=MjService.active();
            if(!prefs.getBoolean("disclosed",false)){explainAndOpenSettings();return;}
            if(service==null){prefs.edit().putBoolean("enabled",true).apply();openSystemSettings();return;}
            boolean enabled=!prefs.getBoolean("enabled",false);
            prefs.edit().putBoolean("enabled",enabled).apply();service.settingsChanged();updateStatus();
        });
        add(body,toggle,0,12);
        add(body,button("无障碍服务设置",false,this::explainAndOpenSettings),0,8);
        connectionHint=text("",14,MUTED,false);add(body,connectionHint,0,12);
        add(body,button("后台运行与服务帮助",false,this::showServiceHelp),0,24);

        LinearLayout soundRow=row();
        LinearLayout soundCopy=column(BG,0);
        soundCopy.addView(text("播放声音",17,FG,true));
        soundHint=text("使用媒体音量",14,MUTED,false);soundCopy.addView(soundHint);
        soundRow.addView(soundCopy,new LinearLayout.LayoutParams(0,-2,1));
        Switch sound=new Switch(this);
        sound.setContentDescription("播放声音");
        sound.setMinWidth(dp(56));sound.setMinHeight(dp(48));
        sound.setThumbTintList(ColorStateList.valueOf(FG));
        sound.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{RED,BORDER}));
        sound.setChecked(prefs.getBoolean("sound",true));
        sound.setOnCheckedChangeListener((b,checked) -> {
            prefs.edit().putBoolean("sound",checked).apply();
            if(MjService.active()!=null)MjService.active().settingsChanged();
        });
        soundRow.addView(sound);
        add(body,soundRow,0,24);

        LinearLayout compatibilityRow=row();
        LinearLayout compatibilityCopy=column(BG,0);
        compatibilityCopy.addView(text("输入法兼容",17,FG,true));
        compatibilityCopy.addView(text("补查输入框，改善漏触发；可能增加耗电",14,MUTED,false));
        compatibilityRow.addView(compatibilityCopy,new LinearLayout.LayoutParams(0,-2,1));
        Switch compatibility=new Switch(this);compatibility.setContentDescription("输入法兼容");
        compatibility.setMinWidth(dp(56));compatibility.setMinHeight(dp(48));
        compatibility.setChecked(prefs.getBoolean("compatibility",true));
        compatibility.setThumbTintList(ColorStateList.valueOf(FG));
        compatibility.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{RED,BORDER}));
        compatibility.setOnCheckedChangeListener((b,checked)->{prefs.edit().putBoolean("compatibility",checked).apply();if(MjService.active()!=null)MjService.active().settingsChanged();});
        compatibilityRow.addView(compatibility);add(body,compatibilityRow,0,24);
        LinearLayout wechatRow=row();LinearLayout wechatCopy=column(BG,0);
        wechatCopy.addView(text("微信兼容模式",17,FG,true));
        wechatCopy.addView(text("系统输入连接，需手动同意；可能增加耗电",14,MUTED,false));
        wechatRow.addView(wechatCopy,new LinearLayout.LayoutParams(0,-2,1));
        Switch wechatSwitch=new Switch(this);wechatSwitch.setContentDescription("微信兼容模式");
        wechatSwitch.setMinHeight(dp(48));wechatSwitch.setMinWidth(dp(56));
        wechatSwitch.setThumbTintList(ColorStateList.valueOf(FG));
        wechatSwitch.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{RED,BORDER}));
        wechatSwitch.setEnabled(android.os.Build.VERSION.SDK_INT>=33);
        wechatSwitch.setChecked(prefs.getBoolean("wechat_ime",false)&&prefs.getBoolean("wechat_ime_consent",false));
        final boolean[] updatingWechat={false};
        wechatSwitch.setOnCheckedChangeListener((b,checked)->{
            if(updatingWechat[0])return;
            if(checked&&!prefs.getBoolean("wechat_ime_consent",false)){
                updatingWechat[0]=true;wechatSwitch.setChecked(false);updatingWechat[0]=false;
                new AlertDialog.Builder(this).setTitle("开启微信兼容？")
                    .setMessage("这个模式使用 Android 公开的无障碍输入连接接口，检查微信普通输入框光标附近少量文字。系统接口具备编辑能力，但本应用只查询文字，不代你输入、删除或发送消息。\n\n密码和选中文字不参与识别；只在本机临时处理，不保存文字历史、不上传。系统或微信不提供连接时仍可能无法识别，无需更换现有输入法。")
                    .setNegativeButton("暂不开启",null).setPositiveButton("同意并开启",(d,w)->{
                        prefs.edit().putBoolean("wechat_ime_consent",true).putBoolean("wechat_ime",true).apply();wechatSwitch.setChecked(true);
                        if(MjService.active()!=null)MjService.active().settingsChanged();
                        Toast.makeText(this,"请将无障碍服务关闭再开启，然后到微信输入 mj",Toast.LENGTH_LONG).show();
                    }).show();return;
            }
            prefs.edit().putBoolean("wechat_ime",checked).apply();if(MjService.active()!=null)MjService.active().settingsChanged();
        });
        wechatRow.addView(wechatSwitch);add(body,wechatRow,0,24);
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
        test.setHint("中文或英文输入法，键入 mj");
        test.setMinHeight(dp(56));
        test.setPadding(dp(16),dp(12),dp(16),dp(12));
        test.setBackground(focusShape());
        test.setInputType(InputType.TYPE_CLASS_TEXT);
        test.addTextChangedListener(new TextWatcher(){
            private String before="";
            @Override public void beforeTextChanged(CharSequence text,int start,int count,int after){before=text.toString();}
            @Override public void onTextChanged(CharSequence text,int start,int removed,int count){
                MjService service=MjService.active();
                if(service!=null)try{service.localInput(before,text,start,removed,count);}catch(RuntimeException error){ServiceStatus.note(MainActivity.this,"测试输入暂未触发，可重新连接服务");}
                before="";
            }
            @Override public void afterTextChanged(Editable text){before="";}
        });
        add(body,test,12,8);
        add(body,text("中文或英文输入法都可试 mj（含全角ｍｊ）。若拼音候选未出现在输入框中，请先提交英文字母。",14,MUTED,false),0,24);
        add(body,button("停止并关闭无障碍服务",false,()->{
            new AlertDialog.Builder(this).setTitle("停止特效？").setMessage("这会关闭系统无障碍服务。只想临时暂停，请使用上方的暂停按钮。")
                .setNegativeButton("取消",null).setPositiveButton("停止并关闭",(d,w)->{
                    prefs.edit().putBoolean("enabled",false).apply();if(MjService.active()!=null)MjService.active().turnOff();updateStatus();
                }).show();
        }),0,12);
        add(body,button("复制诊断信息",false,()->{
            MjService service=MjService.active();
            String info="mj-spiderman Android 0.3.2\nAndroid "+android.os.Build.VERSION.RELEASE+" / SDK "+android.os.Build.VERSION.SDK_INT
                +"\n系统服务开关（设置项）："+ServiceStatus.stateName(ServiceStatus.userSelection(this))
                +"\n无障碍总状态："+ServiceStatus.stateName(ServiceStatus.masterEnabled(this))
                +"\n系统绑定列表："+(ServiceStatus.systemBound(this)?"有":"无")
                +"\n实际连接："+(service!=null?"已连接":"未连接")+"\n自动触发："+prefs.getBoolean("enabled",false)
                +"\n微信兼容开关："+prefs.getBoolean("wechat_ime",false)+"\n输入法兼容："+prefs.getBoolean("compatibility",true)+"\n最近服务状态："+prefs.getString("service_note","尚无记录")
                +"\n解绑时开关："+prefs.getString("selection_at_disconnect","未记录")
                +"\n最近异常类型："+prefs.getString("last_error","无记录")
                +"\n最近进程退出（含升级退出）："+ServiceStatus.exitHistory(this)
                +"\n"+(service!=null?service.inputStats():prefs.getString("last_input_stats","无输入检查记录"));
            ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("mj-spiderman 诊断",info));
            Toast.makeText(this,"已复制诊断信息，不包含输入文字",Toast.LENGTH_SHORT).show();
        }),0,20);

        TextView notes=text("仅在提供文字变化事件的输入框中工作，密码框会跳过。后台失效时，请检查手机的应用电池管理与服务状态。",14,MUTED,false);
        add(body,notes,0,12);
        add(body,text("本地识别，不联网、不保存输入文字。",14,MUTED,false),0,12);
        add(body,text("0.3.2 修复试用版 · 微信兼容仍需真机复测",13,MUTED,false),0,8);
        updateStatus();
    }

    private void openSystemSettings(){startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}
    private void explainAndOpenSettings() {
        if(prefs.getBoolean("disclosed",false)){openSystemSettings();return;}
        new AlertDialog.Builder(this).setTitle("开启前请了解")
            .setMessage("无障碍服务可以读取其他应用输入控件中的文字。本应用仅在本机识别 mj；输入法兼容模式会定期检查当前输入框，跳过密码框，不记录文字、不上传数据，也没有联网权限。\n\n同意后将打开系统设置，请手动选择“mj-spiderman”并开启。服务已开启时，完成本说明即可启用识别；你可以随时停止服务。")
            .setNegativeButton("暂不开启",null)
            .setPositiveButton("了解，前往设置",(d,w)->{
                prefs.edit().putBoolean("disclosed",true).putBoolean("enabled",true).apply();
                if(MjService.active()!=null)MjService.active().settingsChanged();openSystemSettings();
            }).show();
    }
    private void showServiceHelp(){
        new AlertDialog.Builder(this).setTitle("服务开启后自动关闭？")
            .setMessage("先在本应用完成首次说明，再到系统设置开启 mj-spiderman。\n\n如果仍自动关闭：在应用信息中检查电池和后台运行限制。iQOO 可在设置中搜索“后台耗电管理”，若有此选项则允许本应用后台高耗电；菜单名称因系统版本而异。服务开关由系统管理，应用不能替你强制开启。若设置仍开但连接断开，不等于你关闭了权限。\n\n若开关仍开但未连接，请手动关闭后重新开启。再次发生时，可复制本页诊断信息发来排查。")
            .setNegativeButton("返回",null).setPositiveButton("打开应用信息",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).show();
    }
    private void preview(int index) {
        MjService service=MjService.active();
        if(service==null||!service.ready())Toast.makeText(this,"请先完成首次说明并手动开启服务",Toast.LENGTH_SHORT).show();
        else service.preview(index);
    }
    private void updateStatus() {
        if(status==null)return;
        MjService service=MjService.active();boolean bound=service!=null;
        int selected=ServiceStatus.userSelection(this);boolean authorized=selected==1||bound;boolean unknown=selected<0;
        boolean disclosed=prefs.getBoolean("disclosed",false);
        boolean enabled=bound&&disclosed&&prefs.getBoolean("enabled",false);
        String state=!disclosed?"等待完成首次说明":!bound?(authorized?"已授权，连接已断开":unknown?"暂无法确认系统开关":"服务已关闭"):enabled?"特效已开启":"特效已暂停";
        if(!state.contentEquals(stateLabel.getText()))stateLabel.setText(state);
        stateLabel.setTextColor(enabled?GREEN:MUTED);
        status.setText(!disclosed?"先阅读应用内说明，再开启识别":!bound?(authorized?"系统仍选中本服务，请关闭后重新开启来连接":unknown?"系统不允许读取开关，请到设置中核对":"请到无障碍设置手动开启 mj-spiderman"):enabled?"中英文输入法键入 mj，召唤蜘蛛侠":"随时开启，也可以先试听");
        toggle.setText(!disclosed?"完成首次说明":!bound?(authorized?"重新连接服务":"开始设置"):enabled?"暂停特效":"开启特效");
        if(connectionHint!=null){
            String note=prefs.getString("service_note","");String error=prefs.getString("last_error","");
            connectionHint.setText(bound?"服务已实际连接；系统开关与应用暂停互相独立":note.length()>0?note:"开启后请回到应用，确认显示服务已连接");
            if(bound&&note.length()>0&&!note.equals("系统已连接服务"))connectionHint.append("\n最近提示："+note);
            if(error.length()>0)connectionHint.append("\n曾发生服务异常，可复制诊断信息排查");
        }
        if(soundHint!=null){
            AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
            String hint=audio.getStreamVolume(AudioManager.STREAM_MUSIC)==0?"媒体音量为 0，请用音量键调高":"使用媒体音量，可用音量键调整";
            try{NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);int filter=n.getCurrentInterruptionFilter();if(filter==NotificationManager.INTERRUPTION_FILTER_PRIORITY||filter==NotificationManager.INTERRUPTION_FILTER_NONE||filter==NotificationManager.INTERRUPTION_FILTER_ALARMS)hint+="\n勿扰已开启，是否出声取决于系统设置";}catch(RuntimeException ignored){}
            if(!hint.contentEquals(soundHint.getText()))soundHint.setText(hint);
        }
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

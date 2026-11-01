package com.vakasu11.mjspiderman;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.graphics.*;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import android.widget.Toast;
import java.io.*;
import java.util.Random;
import java.util.concurrent.*;

public final class MjService extends AccessibilityService {
    static volatile MjService current;
    private final Handler main=new Handler(Looper.getMainLooper());
    private ExecutorService decoder;
    private final TextTrigger trigger=new TextTrigger();
    private final Random random=new Random();
    private SharedPreferences prefs;
    private WindowManager windows;
    private PowerManager power;
    private KeyguardManager keyguard;
    private EffectView overlay;
    private SoundPool sounds;
    private final int[] soundIds=new int[2];
    private final boolean[] soundReady=new boolean[2];
    private int soundStream,pendingSound=-1;
    private long pendingSoundToken;
    private volatile long generation;
    private volatile boolean connected,destroyed;
    private boolean userStopped;
    private final int[] first={12,4},count={166,80},fps={60,30},width={434,720};
    private static final long LOCAL_FIELD=Long.MIN_VALUE;

    static MjService active(){MjService s=current;return s!=null&&s.connected&&!s.destroyed?s:null;}
    boolean ready(){return connected&&!destroyed&&prefs!=null&&prefs.getBoolean("disclosed",false);}
    @Override public void onCreate(){
        super.onCreate();ServiceStatus.installCrashRecorder(this);
        prefs=getSharedPreferences("mj",MODE_PRIVATE);decoder=Executors.newSingleThreadExecutor();
        power=(PowerManager)getSystemService(POWER_SERVICE);keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
    }
    @Override protected void onServiceConnected(){
        super.onServiceConnected();connected=true;current=this;userStopped=false;
        windows=(WindowManager)getSystemService(WINDOW_SERVICE);
        // Keep the user-enabled system switch on. Until disclosure is accepted,
        // remain idle instead of calling disableSelf() and immediately turning it off.
        ServiceStatus.note(this,"系统已连接服务");
        trigger.reset();settingsChanged();
    }
    private boolean canObserve(){return ready()&&prefs.getBoolean("enabled",false)&&power.isInteractive()&&!keyguard.isKeyguardLocked();}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(!canObserve()||event==null||event.isPassword())return;
        if(event.getEventType()==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            &&(event.getContentChangeTypes()&AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT)==0)return;
        if(getPackageName().contentEquals(event.getPackageName()==null?"":event.getPackageName()))return;
        AccessibilityNodeInfo node=null;
        try{
            node=editableSource(event);
            if(node==null||node.isPassword())return;
            CharSequence text=node.getText();long field=field(node);
            int type=event.getEventType();
            if(type==AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED){
                if(text==null&&!event.getText().isEmpty())text=event.getText().get(0);
                boolean match;
                if(event.getFromIndex()>=0&&event.getAddedCount()>=0)
                    match=trigger.changed(event.getBeforeText(),text,event.getFromIndex(),event.getAddedCount(),event.getRemovedCount(),false,true,field);
                else match=trigger.observe(text,node.getTextSelectionEnd(),false,true,field);
                if(match)preview(random.nextInt(2));
            }else if(type==AccessibilityEvent.TYPE_VIEW_FOCUSED){
                trigger.prime(text,node.getTextSelectionEnd(),field);
            }else if(trigger.observe(text,node.getTextSelectionEnd(),false,true,field))preview(random.nextInt(2));
        }catch(RuntimeException error){ServiceStatus.note(this,"输入控件暂不可读取，等待下一次更新");}
        finally{if(node!=null)node.recycle();}
    }
    private AccessibilityNodeInfo editableSource(AccessibilityEvent event){
        AccessibilityNodeInfo source=event.getSource();
        if(source!=null){if(source.isPassword()){source.recycle();return null;}if(source.isEditable())return source;source.recycle();}
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null)return null;
        AccessibilityNodeInfo focused=null;
        try{focused=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);}finally{if(focused!=root)root.recycle();}
        if(focused!=null&&(!focused.isEditable()||focused.isPassword())){focused.recycle();return null;}
        return focused;
    }
    private long field(AccessibilityNodeInfo node){return ((long)node.getWindowId()<<32)^(node.hashCode()&0xffffffffL);}
    private final Runnable focusCheck=new Runnable(){
        @Override public void run(){
            if(!connected||destroyed)return;
            if(canObserve()&&prefs.getBoolean("compatibility",true)){
                AccessibilityNodeInfo root=null,node=null;
                try{
                    root=getRootInActiveWindow();if(root!=null)node=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                    if(node!=null&&node.isEditable()&&!node.isPassword()&&!getPackageName().contentEquals(node.getPackageName()==null?"":node.getPackageName())){
                        if(trigger.observe(node.getText(),node.getTextSelectionEnd(),false,true,field(node)))preview(random.nextInt(2));
                    }
                }catch(RuntimeException ignored){}
                finally{if(node!=null&&node!=root)node.recycle();if(root!=null)root.recycle();}
            }
            if(connected&&!destroyed&&prefs.getBoolean("enabled",false))main.postDelayed(this,canObserve()&&prefs.getBoolean("compatibility",true)?250:1500);
        }
    };
    void localInput(CharSequence before,CharSequence text,int start,int removed,int added){
        if(canObserve()&&trigger.changed(before,text,start,added,removed,false,true,LOCAL_FIELD))preview(random.nextInt(2));
    }
    void settingsChanged(){
        trigger.reset();main.removeCallbacks(focusCheck);
        if(!ready()||!prefs.getBoolean("enabled",false))stopEffect();
        else{if(sounds==null)loadSounds();main.post(focusCheck);}
        if(!prefs.getBoolean("sound",true))stopSound();
    }
    void turnOff(){userStopped=true;prefs.edit().putBoolean("enabled",false).apply();ServiceStatus.note(this,"已由应用内停止按钮关闭");disconnect();disableSelf();}
    void preview(int index){
        if(!ready()||windows==null||keyguard.isKeyguardLocked()||!power.isInteractive())return;
        stopEffect();final long token=++generation;
        overlay=new EffectView(this);
        android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
        int h=Math.min(Math.round(240*metrics.density),metrics.heightPixels/2);
        h=Math.min(h,metrics.widthPixels*720/width[index]);int w=Math.round(h*width[index]/720f);
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;params.y=Math.round(48*metrics.density);
        try{windows.addView(overlay,params);}catch(RuntimeException error){overlay=null;ServiceStatus.note(this,"系统暂时无法显示特效");Toast.makeText(this,"系统暂时无法显示特效，请重新连接服务",Toast.LENGTH_SHORT).show();return;}
        if(prefs.getBoolean("sound",true))playSound(index,token);
        render(index,token,SystemClock.uptimeMillis(),-1);
    }
    private void render(int index,long token,long start,int previous){
        if(token!=generation||overlay==null||destroyed)return;
        int frame=(int)((SystemClock.uptimeMillis()-start)*fps[index]/1000);
        if(frame>=count[index]){stopEffect();return;}
        if(frame==previous){main.postDelayed(()->render(index,token,start,previous),16);return;}
        final int frameIndex=frame;
        try{decoder.execute(()->{
            if(token!=generation||destroyed)return;
            Bitmap bitmap=null;String path=(index==0?"frames/":"frames2/")+String.format(java.util.Locale.ROOT,"f%04d.png",first[index]+frameIndex);
            try(InputStream input=getAssets().open(path)){bitmap=BitmapFactory.decodeStream(input);}catch(IOException|RuntimeException ignored){}
            final Bitmap ready=bitmap;
            main.post(()->{
                if(token!=generation||overlay==null||destroyed){if(ready!=null)ready.recycle();return;}
                if(ready==null){stopEffect();ServiceStatus.note(this,"动画素材读取失败");return;}
                overlay.setFrame(ready);main.postDelayed(()->render(index,token,start,frameIndex),16);
            });
        });}catch(RejectedExecutionException ignored){stopEffect();}
    }
    private void loadSounds(){
        java.util.Arrays.fill(soundReady,false);java.util.Arrays.fill(soundIds,0);
        try{
            sounds=new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
            final SoundPool pool=sounds;
            pool.setOnLoadCompleteListener((p,id,status)->{
                if(destroyed||sounds!=p)return;
                for(int i=0;i<2;i++)if(soundIds[i]==id){
                    soundReady[i]=status==0;
                    if(status!=0)ServiceStatus.note(this,"音效加载失败，可重新连接服务");
                    if(status==0&&pendingSound==i&&pendingSoundToken==generation&&prefs.getBoolean("sound",true)){pendingSound=-1;playSound(i,generation);}
                }
            });
            for(int i=0;i<2;i++)try(AssetFileDescriptor fd=getAssets().openFd(i==0?"audio.wav":"audio2.wav")){soundIds[i]=pool.load(fd,1);}
        }catch(IOException|RuntimeException error){if(sounds!=null)sounds.release();sounds=null;ServiceStatus.note(this,"音效素材加载失败，请重新安装");}
    }
    private void playSound(int index,long token){
        if(sounds==null)loadSounds();
        if(sounds==null)return;
        if(!soundReady[index]){pendingSound=index;pendingSoundToken=token;return;}
        try{soundStream=sounds.play(soundIds[index],1f,1f,1,0,1f);}catch(RuntimeException error){soundStream=0;}
        if(soundStream==0)ServiceStatus.note(this,"系统未能播放音效，请检查媒体音量与勿扰设置");
    }
    private void stopSound(){pendingSound=-1;if(sounds!=null&&soundStream!=0)try{sounds.stop(soundStream);}catch(RuntimeException ignored){}soundStream=0;}
    private void stopEffect(){++generation;stopSound();if(overlay!=null){try{windows.removeViewImmediate(overlay);}catch(RuntimeException ignored){}overlay.clear();overlay=null;}}
    private void disconnect(){connected=false;if(current==this)current=null;main.removeCallbacks(focusCheck);trigger.reset();stopEffect();}
    @Override public void onInterrupt(){stopEffect();}
    @Override public boolean onUnbind(Intent intent){if(!userStopped)ServiceStatus.note(this,"系统已断开服务，请检查后台限制并重新开启");disconnect();return super.onUnbind(intent);}
    @Override public void onDestroy(){disconnect();destroyed=true;if(decoder!=null)decoder.shutdownNow();if(sounds!=null){sounds.release();sounds=null;}super.onDestroy();}
    private static final class EffectView extends View {
        private Bitmap bitmap;private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
        EffectView(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        void setFrame(Bitmap frame){bitmap=frame;invalidate();}void clear(){bitmap=null;}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(bitmap!=null)canvas.drawBitmap(bitmap,null,new Rect(0,0,getWidth(),getHeight()),paint);}
    }
}

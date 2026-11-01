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
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import android.text.InputType;
import java.util.concurrent.*;

public final class MjService extends AccessibilityService {
    static volatile MjService current;
    private final Handler main=new Handler(Looper.getMainLooper());
    private ExecutorService decoder;
    private final TextTrigger trigger=new TextTrigger();
    private final TextTrigger localTrigger=new TextTrigger();
    private HandlerThread observerThread;
    private Handler observer;
    private volatile long inputEpoch;
    private final ArrayDeque<PendingEvent> pendingEvents=new ArrayDeque<>();
    private final AtomicInteger wechatEvents=new AtomicInteger(),wechatEditable=new AtomicInteger(),wechatMatches=new AtomicInteger(),inputErrors=new AtomicInteger(),droppedEvents=new AtomicInteger();
    private final AtomicLong maxReadMs=new AtomicLong();
    private volatile long activeReadStart;
    private volatile String wechatClass="未收到";
    private boolean draining;
    private static final class PendingEvent {
        final AccessibilityEvent event;final long epoch;
        PendingEvent(AccessibilityEvent e,long value){event=e;epoch=value;}
    }
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
        observerThread=new HandlerThread("mj-input-observer");observerThread.start();observer=new Handler(observerThread.getLooper());
        power=(PowerManager)getSystemService(POWER_SERVICE);keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
    }
    @Override protected void onServiceConnected(){
        super.onServiceConnected();connected=true;current=this;userStopped=false;
        windows=(WindowManager)getSystemService(WINDOW_SERVICE);
        // Keep the user-enabled system switch on. Until disclosure is accepted,
        // remain idle instead of calling disableSelf() and immediately turning it off.
        ServiceStatus.note(this,"系统已连接服务");
        settingsChanged();
    }
    private boolean canObserve(){return ready()&&prefs.getBoolean("enabled",false)&&power.isInteractive()&&!keyguard.isKeyguardLocked();}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(!canObserve()||event==null||event.isPassword())return;
        if(event.getEventType()==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            &&(event.getContentChangeTypes()&AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT)==0)return;
        if(getPackageName().contentEquals(event.getPackageName()==null?"":event.getPackageName()))return;
        if("com.tencent.mm".contentEquals(event.getPackageName()==null?"":event.getPackageName())){
            wechatEvents.incrementAndGet();String name=String.valueOf(event.getClassName());
            if(name.matches("[A-Za-z0-9_.$]{1,160}"))wechatClass=name;
        }
        PendingEvent copy=new PendingEvent(AccessibilityEvent.obtain(event),inputEpoch);
        synchronized(pendingEvents){
            if(pendingEvents.size()>=16){pendingEvents.removeFirst().event.recycle();droppedEvents.incrementAndGet();}
            pendingEvents.addLast(copy);
            if(!draining){draining=true;observer.post(drainEvents);}
        }
    }
    private final Runnable drainEvents=new Runnable(){
        @Override public void run(){
            PendingEvent pending;
            synchronized(pendingEvents){pending=pendingEvents.pollFirst();if(pending==null){draining=false;return;}}
            long start=SystemClock.uptimeMillis();activeReadStart=start;
            try{if(canObserve()&&pending.epoch==inputEpoch&&start-pending.event.getEventTime()<1800)processEvent(pending.event,pending.epoch);}
            catch(RuntimeException error){inputErrors.incrementAndGet();ServiceStatus.note(MjService.this,"控件读取暂不可用，服务继续等待新输入");}
            finally{pending.event.recycle();recordReadTime(SystemClock.uptimeMillis()-start);activeReadStart=0;}
            observer.post(this);
        }
    };
    private void recordReadTime(long ms){long old=maxReadMs.get();while(ms>old&&!maxReadMs.compareAndSet(old,ms))old=maxReadMs.get();}
    private void clearInputEvents(){synchronized(pendingEvents){while(!pendingEvents.isEmpty())pendingEvents.removeFirst().event.recycle();}}
    private void requestEffect(long epoch,boolean wechat){
        if(wechat)wechatMatches.incrementAndGet();
        main.post(()->{if(epoch==inputEpoch&&canObserve())preview(random.nextInt(2));});
    }
    private void processEvent(AccessibilityEvent event,long epoch){
        boolean wechat="com.tencent.mm".contentEquals(event.getPackageName()==null?"":event.getPackageName());
        AccessibilityNodeInfo node=null;
        try{
            node=editableSource(event);
            if(SystemClock.uptimeMillis()-event.getEventTime()>1800)return;
            if(node==null||passwordNode(node))return;
            if(wechat)wechatEditable.incrementAndGet();
            CharSequence text=node.getText();long field=field(node);
            int type=event.getEventType();
            if(type==AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED){
                if(text==null&&!event.getText().isEmpty())text=event.getText().get(0);
                boolean match;
                if(event.getFromIndex()>=0&&event.getAddedCount()>=0)
                    match=trigger.changed(event.getBeforeText(),text,event.getFromIndex(),event.getAddedCount(),event.getRemovedCount(),false,true,field);
                else match=trigger.observe(text,node.getTextSelectionEnd(),false,true,field);
                if(match)requestEffect(epoch,wechat);
            }else if(type==AccessibilityEvent.TYPE_VIEW_FOCUSED){
                trigger.prime(text,node.getTextSelectionEnd(),field);
            }else if(trigger.observe(text,node.getTextSelectionEnd(),false,true,field))requestEffect(epoch,wechat);
        }catch(RuntimeException error){inputErrors.incrementAndGet();ServiceStatus.note(this,"输入控件暂不可读取，等待下一次更新");}
        finally{if(node!=null)node.recycle();}
    }
    private boolean passwordNode(AccessibilityNodeInfo node){
        if(node.isPassword())return true;
        int type=node.getInputType(),kind=type&InputType.TYPE_MASK_CLASS,variation=type&InputType.TYPE_MASK_VARIATION;
        return kind==InputType.TYPE_CLASS_TEXT&&(variation==InputType.TYPE_TEXT_VARIATION_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
            ||kind==InputType.TYPE_CLASS_NUMBER&&variation==InputType.TYPE_NUMBER_VARIATION_PASSWORD;
    }
    private boolean textInput(AccessibilityNodeInfo node){
        if(node==null||passwordNode(node))return false;
        if(node.isEditable())return true;
        String name=String.valueOf(node.getClassName());
        return node.isFocused()&&(name.endsWith("EditText")||(node.getInputType()&InputType.TYPE_MASK_CLASS)!=InputType.TYPE_NULL);
    }
    private AccessibilityNodeInfo inputInRoot(AccessibilityNodeInfo root){
        if(root==null)return null;AccessibilityNodeInfo node=null;
        try{node=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);}finally{if(root!=node)root.recycle();}
        if(!textInput(node)){if(node!=null)node.recycle();return null;}return node;
    }
    private AccessibilityNodeInfo focusedInput(){
        AccessibilityNodeInfo node=inputInRoot(getRootInActiveWindow());
        if(node!=null)return node;
        // An accessibility overlay or IME can be active while the app input owns focus.
        java.util.List<AccessibilityWindowInfo> snapshot=getWindows();
        try{for(AccessibilityWindowInfo window:snapshot){
            if(window.getType()==AccessibilityWindowInfo.TYPE_APPLICATION&&(window.isFocused()||window.isActive())){
                node=inputInRoot(window.getRoot());if(node!=null)return node;
            }
        }}finally{for(AccessibilityWindowInfo window:snapshot)window.recycle();}
        return null;
    }
    private AccessibilityNodeInfo editableSource(AccessibilityEvent event){
        AccessibilityNodeInfo source=event.getSource();
        if(source!=null){if(passwordNode(source)){source.recycle();return null;}if(textInput(source))return source;source.recycle();}
        return focusedInput();
    }
    private long field(AccessibilityNodeInfo node){return ((long)node.getWindowId()<<32)^(node.hashCode()&0xffffffffL);}
    private final Runnable focusCheck=new Runnable(){
        @Override public void run(){
            if(!connected||destroyed)return;
            if(canObserve()&&prefs.getBoolean("compatibility",true)){
                AccessibilityNodeInfo node=null;long start=SystemClock.uptimeMillis(),epoch=inputEpoch;activeReadStart=start;
                try{
                    node=focusedInput();
                    if(node!=null&&!getPackageName().contentEquals(node.getPackageName()==null?"":node.getPackageName())){
                        boolean wechat="com.tencent.mm".contentEquals(node.getPackageName()==null?"":node.getPackageName());
                        if(wechat)wechatEditable.incrementAndGet();
                        if(trigger.observe(node.getText(),node.getTextSelectionEnd(),false,true,field(node))&&SystemClock.uptimeMillis()-start<1800)requestEffect(epoch,wechat);
                    }
                }catch(RuntimeException ignored){inputErrors.incrementAndGet();}
                finally{if(node!=null)node.recycle();recordReadTime(SystemClock.uptimeMillis()-start);activeReadStart=0;}
            }
            if(connected&&!destroyed&&prefs.getBoolean("enabled",false))observer.postDelayed(this,canObserve()&&prefs.getBoolean("compatibility",true)?350:1500);
        }
    };
    void localInput(CharSequence before,CharSequence text,int start,int removed,int added){
        if(canObserve()&&localTrigger.changed(before,text,start,added,removed,false,true,LOCAL_FIELD))preview(random.nextInt(2));
    }
    void settingsChanged(){
        inputEpoch++;localTrigger.reset();clearInputEvents();observer.removeCallbacks(focusCheck);observer.post(()->trigger.reset());
        if(!ready()||!prefs.getBoolean("enabled",false))stopEffect();
        else{if(sounds==null)loadSounds();observer.post(focusCheck);}
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
    String inputStats(){return "微信事件："+wechatEvents.get()+"；可读输入："+wechatEditable.get()+"；匹配："+wechatMatches.get()
        +"\n微信控件类："+wechatClass+"\n最长控件查询："+Math.max(maxReadMs.get(),activeReadStart==0?0:SystemClock.uptimeMillis()-activeReadStart)+"ms；读取异常："+inputErrors.get()+"；丢弃过期积压："+droppedEvents.get();}
    private void saveDisconnectSnapshot(){prefs.edit().putString("last_input_stats",inputStats())
        .putString("selection_at_disconnect",ServiceStatus.stateName(ServiceStatus.userSelection(this)))
        .putLong("disconnected_at",System.currentTimeMillis()).apply();}
    private void disconnect(){connected=false;inputEpoch++;if(current==this)current=null;if(observer!=null){observer.removeCallbacks(focusCheck);clearInputEvents();}localTrigger.reset();stopEffect();}
    @Override public void onInterrupt(){stopEffect();}
    @Override public boolean onUnbind(Intent intent){saveDisconnectSnapshot();if(!userStopped)ServiceStatus.note(this,"系统已解绑服务；请核对设置中的开关与进程退出记录");disconnect();return super.onUnbind(intent);}
    @Override public void onDestroy(){disconnect();destroyed=true;if(observerThread!=null)observerThread.quitSafely();if(decoder!=null)decoder.shutdownNow();if(sounds!=null){sounds.release();sounds=null;}super.onDestroy();}
    private static final class EffectView extends View {
        private Bitmap bitmap;private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
        EffectView(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        void setFrame(Bitmap frame){bitmap=frame;invalidate();}void clear(){bitmap=null;}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(bitmap!=null)canvas.drawBitmap(bitmap,null,new Rect(0,0,getWidth(),getHeight()),paint);}
    }
}

package com.vakasu11.mjspiderman;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.provider.Settings;
import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.view.accessibility.AccessibilityManager;

final class ServiceStatus {
    private static boolean recorderInstalled;
    // -1 means unreadable, not disabled. This is the user's actual settings selection.
    static int userSelection(Context context){
        try{return ServiceSelection.contains(Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),context.getPackageName(),MjService.class.getName())?1:0;}
        catch(RuntimeException ignored){return -1;}
    }
    static int masterEnabled(Context context){
        try{return Settings.Secure.getInt(context.getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED,0)==0?0:1;}
        catch(RuntimeException ignored){return -1;}
    }
    static String stateName(int state){return state<0?"未知（系统不允许读取）":state==1?"开":"关";}
    static String exitHistory(Context context){
        StringBuilder text=new StringBuilder();
        try{
            ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            for(ApplicationExitInfo info:manager.getHistoricalProcessExitReasons(context.getPackageName(),0,3)){
                if(text.length()>0)text.append("; ");
                text.append(new java.text.SimpleDateFormat("MM-dd HH:mm:ss",java.util.Locale.ROOT).format(new java.util.Date(info.getTimestamp()))).append(' ');
                switch(info.getReason()){
                    case ApplicationExitInfo.REASON_ANR:text.append("无响应(ANR)");break;
                    case ApplicationExitInfo.REASON_CRASH:text.append("Java异常退出");break;
                    case ApplicationExitInfo.REASON_CRASH_NATIVE:text.append("原生异常退出");break;
                    case ApplicationExitInfo.REASON_LOW_MEMORY:text.append("系统内存回收");break;
                    case ApplicationExitInfo.REASON_USER_REQUESTED:text.append("用户或系统请求停止");break;
                    case ApplicationExitInfo.REASON_USER_STOPPED:text.append("用户停止");break;
                    case ApplicationExitInfo.REASON_PACKAGE_UPDATED:text.append("应用更新");break;
                    case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE:text.append("资源占用过多");break;
                    default:text.append("系统原因码 ").append(info.getReason());break;
                }
            }
        }catch(RuntimeException ignored){return "当前系统未提供";}
        return text.length()==0?"无可用记录":text.toString();
    }
    // Android 16's API lists mBoundServices. Do not call this the settings switch.
    static boolean systemBound(Context context){
        AccessibilityManager manager=(AccessibilityManager)context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(manager==null)return false;
        try{
            for(AccessibilityServiceInfo info:manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)){
                android.content.pm.ServiceInfo service=info.getResolveInfo().serviceInfo;
                if(context.getPackageName().equals(service.packageName)&&MjService.class.getName().equals(service.name))return true;
            }
        }catch(RuntimeException ignored){}
        return false;
    }
    static void note(Context context,String message){
        android.content.SharedPreferences p=context.getSharedPreferences("mj",Context.MODE_PRIVATE);
        if(!message.equals(p.getString("service_note","")))p.edit().putString("service_note",message).apply();
    }
    static synchronized void installCrashRecorder(Context context){
        if(recorderInstalled)return;recorderInstalled=true;
        final Context app=context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            // Store only the exception class, never its message or any input text.
            try{app.getSharedPreferences("mj",Context.MODE_PRIVATE).edit().putString("last_error",error.getClass().getSimpleName()).commit();}catch(RuntimeException ignored){}
            if(previous!=null)previous.uncaughtException(thread,error);
            else android.os.Process.killProcess(android.os.Process.myPid());
        });
    }
}

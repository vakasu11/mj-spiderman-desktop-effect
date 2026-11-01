package com.vakasu11.mjspiderman;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.view.accessibility.AccessibilityManager;

final class ServiceStatus {
    private static boolean recorderInstalled;
    static boolean systemEnabled(Context context){
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

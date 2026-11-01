package com.vakasu11.mjspiderman;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.text.InputType;

/** API-33 code is isolated so Android 11/12 retain the ordinary node path. */
final class WechatInputBridge {
    static final class Snapshot {
        final Object connection;final long context;final boolean wechat;final String state;
        Object service;
        Snapshot(Object c,long key,boolean target,String note){connection=c;context=key;wechat=target;state=note;}
    }
    static final class Slice {
        final CharSequence text;final int start,end,offset;
        Slice(CharSequence t,int a,int b,int o){text=t;start=a;end=b;offset=o;}
    }
    static Snapshot capture(AccessibilityService service){
        if(Build.VERSION.SDK_INT<33)return new Snapshot(null,0,false,"需要 Android 13 或更新版本");
        return Api33.capture(service);
    }
    static Slice read(Snapshot snapshot){return Api33.read(snapshot);}
    @android.annotation.TargetApi(33)
    private static final class Api33 {
        static Snapshot capture(AccessibilityService service){
            android.accessibilityservice.InputMethod ime=service.getInputMethod();
            if(ime==null)return new Snapshot(null,0,false,"系统尚未建立输入连接接口，请重新开启服务");
            android.view.inputmethod.EditorInfo info=ime.getCurrentInputEditorInfo();
            if(info==null||!ime.getCurrentInputStarted())return new Snapshot(null,0,false,"当前未聚焦输入框");
            if(!"com.tencent.mm".equals(info.packageName))return new Snapshot(null,0,false,"当前不是微信输入框");
            int type=info.inputType,kind=type&InputType.TYPE_MASK_CLASS,variation=type&InputType.TYPE_MASK_VARIATION;
            boolean password=kind==InputType.TYPE_CLASS_TEXT&&(variation==InputType.TYPE_TEXT_VARIATION_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
                ||kind==InputType.TYPE_CLASS_NUMBER&&variation==InputType.TYPE_NUMBER_VARIATION_PASSWORD;
            if(password||kind!=InputType.TYPE_CLASS_TEXT)return new Snapshot(null,0,true,"跳过密码或不可确认的输入类型");
            Object connection=ime.getCurrentInputConnection();
            long key=((long)System.identityHashCode(info)<<32)^(info.fieldId&0xffffffffL);
            Snapshot result=new Snapshot(connection,key,true,connection==null?"微信输入连接未就绪":"微信输入连接已建立");result.service=service;return result;
        }
        static Slice read(Snapshot snapshot){
            AccessibilityService service=(AccessibilityService)snapshot.service;
            Snapshot before=capture(service);
            if(!before.wechat||before.connection==null||before.context!=snapshot.context)return null;
            android.accessibilityservice.InputMethod.AccessibilityInputConnection connection=(android.accessibilityservice.InputMethod.AccessibilityInputConnection)before.connection;
            android.view.inputmethod.SurroundingText value=connection.getSurroundingText(64,64,0);
            Snapshot after=capture(service);
            if(!after.wechat||after.connection==null||after.context!=snapshot.context)return null;
            return value==null?null:new Slice(value.getText(),value.getSelectionStart(),value.getSelectionEnd(),value.getOffset());
        }
    }
}

package com.remzone.blink.tv;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebView;
import com.vk.id.captcha.api.VKCaptcha;

public final class BlinkApplication extends Application implements Application.ActivityLifecycleCallbacks {
    @Override public void onCreate(){super.onCreate();try{mobile.Mobile.prepareAssets(new java.io.File(getFilesDir(),"xray-assets").getAbsolutePath());}catch(Exception e){StateStore.detail="Не удалось подготовить сетевые правила. Перезапустите клиент.";}VKCaptcha.INSTANCE.init(this);registerActivityLifecycleCallbacks(this);}
    @Override public void onActivityCreated(Activity activity,Bundle state){
        if(DeviceLayout.isTV(activity)&&activity.getClass().getName().equals("com.vk.id.captcha.web.VKCaptchaWebViewActivity"))activity.getWindow().getDecorView().post(()->{
            WebView web=findWeb(activity.getWindow().getDecorView());if(web!=null){TvCaptchaPointer pointer=new TvCaptchaPointer(activity,web);activity.addContentView(pointer,new ViewGroup.LayoutParams(-1,-1));pointer.requestFocus();}
        });
    }
    private WebView findWeb(android.view.View view){if(view instanceof WebView)return (WebView)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){WebView web=findWeb(group.getChildAt(i));if(web!=null)return web;}}return null;}
    @Override public void onActivityStarted(Activity a){} @Override public void onActivityResumed(Activity a){} @Override public void onActivityPaused(Activity a){} @Override public void onActivityStopped(Activity a){} @Override public void onActivitySaveInstanceState(Activity a,Bundle b){} @Override public void onActivityDestroyed(Activity a){}
}

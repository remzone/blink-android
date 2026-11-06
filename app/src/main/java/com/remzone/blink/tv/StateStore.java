package com.remzone.blink.tv;

import android.content.Context;
import android.net.Uri;

final class StateStore {
    static final Uri URI=Uri.parse("content://com.remzone.blink.status/state");
    static volatile String state="stopped",detail="Туннель отключён",mode="vk",probe="Проверка ещё не выполнялась",egress="";
    static volatile long tx,rx,rtt;
    static void set(Context c,String value,String description){state=value;detail=description;notifyChanged(c);}
    static void notifyChanged(Context c){c.getContentResolver().notifyChange(URI,null);}
    static boolean active(){return "connecting".equals(state)||"ready".equals(state);}
    static String label(){switch(state){case "ready":return "Подключён";case "connecting":return "Подключение…";case "captcha_required":return "VK требует CAPTCHA";case "error":return "Ошибка подключения";default:return "Отключён";}}
}

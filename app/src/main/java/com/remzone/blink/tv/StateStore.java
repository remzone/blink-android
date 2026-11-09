package com.remzone.blink.tv;

import android.content.Context;
import android.net.Uri;

final class StateStore {
    static final Uri URI=Uri.parse("content://com.remzone.blink.status/state");
    static volatile String turnTransport="auto",timings="",state="stopped",detail="Туннель отключён",mode="vk",probe="Проверка ещё не выполнялась",egress="";
    static volatile long tx,rx,rtt;
    static void set(Context c,String value,String description){if(!state.equals(value))DiagnosticJournal.record("connection.state",DiagnosticJournal.fields("state","ready".equals(value)?2:"connecting".equals(value)?1:"captcha_required".equals(value)?3:"error".equals(value)?4:0));state=value;detail=description;notifyChanged(c);}
    static void notifyChanged(Context c){c.getContentResolver().notifyChange(URI,null);}
    static boolean active(){return "connecting".equals(state)||"ready".equals(state)||("captcha_required".equals(state)&&TunnelService.hasPendingCaptcha());}
    static String label(){switch(state){case "ready":return "Подключён";case "connecting":return "Подключение…";case "captcha_required":return "Проверка TURN";case "error":return "Ошибка подключения";default:return "Отключён";}}
}

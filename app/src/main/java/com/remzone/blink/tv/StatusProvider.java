package com.remzone.blink.tv;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import org.json.JSONObject;

/** Signature-only launcher contract; never exposes a feed URL, VLESS UUID or raw config. */
public final class StatusProvider extends ContentProvider {
    public static final String PERMISSION="com.remzone.blink.permission.STATUS";
    @Override public boolean onCreate(){return true;}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
        enforce();MatrixCursor c=new MatrixCursor(new String[]{"state","label","detail","mode","profile","expiresAt","revision","tx","rx","probe","rtt","egress","pendingCaptcha"});
        String name="Подписка не добавлена",mode=StateStore.mode;long expiry=0,revision=0;
        try{JSONObject saved=new SecureStore(getContext()).load();if(!StateStore.active())mode=saved.optString("mode","vk");JSONObject doc=new JSONObject(saved.getString("document"));expiry=doc.getLong("expires_at");revision=doc.getLong("revision");org.json.JSONArray profiles=doc.getJSONArray("profiles");int selected=Math.min(saved.optInt("selected",0),profiles.length()-1);name=profiles.getJSONObject(Math.max(0,selected)).getString("name");}catch(Exception ignored){ }
        if("profile".equals(mode)){expiry=0;revision=0;try{JSONObject p=ProfilesStore.selected(new SecureStore(getContext()).load());name=p.optString("name","VPN");expiry=0;revision=0;}catch(Exception ignored){name="Выберите VPN-профиль";}}
        c.addRow(new Object[]{StateStore.state,StateStore.label(),StateStore.detail,mode,name,expiry,revision,StateStore.tx,StateStore.rx,StateStore.probe,StateStore.rtt,StateStore.egress,TunnelService.hasPendingCaptcha()?1:0});
        c.setNotificationUri(getContext().getContentResolver(),StateStore.URI);return c;
    }
    @Override public Bundle call(String method,String arg,Bundle extras){
        enforce();Bundle out=new Bundle();Context c=getContext();
        if("toggle".equals(method)){
            if(StateStore.active()){c.startService(new Intent(c,TunnelService.class).setAction(TunnelService.STOP));out.putBoolean("accepted",true);}
            else if(!hasValidSubscription(c))out.putBoolean("needsSubscription",true);
            else if(VpnService.prepare(c)!=null)out.putBoolean("needsConsent",true);
            else{c.startForegroundService(new Intent(c,TunnelService.class).setAction(TunnelService.START));out.putBoolean("accepted",true);}
        }else throw new IllegalArgumentException("Unsupported method");
        return out;
    }
    private boolean hasValidSubscription(Context c){try{SecureStore s=new SecureStore(c);JSONObject saved=s.load();if("profile".equals(saved.optString("mode")))return ProfilesStore.valid(saved);s.subscription();return true;}catch(Exception e){return false;}}
    private void enforce(){getContext().enforceCallingOrSelfPermission(PERMISSION,"Launcher signature required");}
    @Override public String getType(Uri uri){return "vnd.android.cursor.item/vnd.blink.status";}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String where,String[] args){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String where,String[] args){throw new UnsupportedOperationException();}
}

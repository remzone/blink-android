package com.gathin.tvhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;

/** Launcher entry to Blink. No network permission, credentials or native VPN runtime in Home. */
public final class NetworkSettingsActivity extends Activity {
    private static final Uri STATUS=Uri.parse("content://com.remzone.blink.status/state");
    private static final ComponentName CLIENT=new ComponentName("com.remzone.blink.tv","com.remzone.blink.tv.NetworkActivity");
    private final Handler main=new Handler(Looper.getMainLooper());
    private TextView state,detail,subscription,diagnostic;private Button toggle,captcha;private boolean active;
    private final ContentObserver observer=new ContentObserver(main){@Override public void onChange(boolean self){refresh();}};
    @Override protected void attachBaseContext(android.content.Context context){super.attachBaseContext(LauncherLocale.wrap(context));}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(50),dp(30),dp(50),dp(35));root.setBackgroundColor(0xff0d1520);scroll.addView(root);
        TextView title=text("Сетевые настройки",30,Color.WHITE);title.setTypeface(null,Typeface.BOLD);root.addView(title);root.addView(text("Обычная сеть и туннель Blink",16,0xff9eafc3));
        state=text("Blink не установлен",25,0xff65dfc3);root.addView(state);detail=text("",15,0xffc5d3e2);root.addView(detail);
        toggle=button(root,"Подключить туннель",this::toggle);
        captcha=button(root,"Пройти проверку VK",()->openClient("captcha",false));captcha.setVisibility(android.view.View.GONE);
        subscription=text("",15,0xffc5d3e2);root.addView(subscription);
        button(root,"Подписка и профили Vokop",()->openClient("subscription",false));
        button(root,"Режим туннеля и диагностика",()->openClient("diagnostics",false));
        diagnostic=text("",14,0xff9eafc3);root.addView(diagnostic);
        button(root,"Wi-Fi и Ethernet",()->startActivity(SystemSettings.intent(this,Settings.ACTION_WIFI_SETTINGS)));
        root.addView(text("Туннель включается вручную. Локальная сеть остаётся доступной напрямую. Ссылку подписки хранит только Blink.",13,0xff9eafc3));
        button(root,"Назад",this::finish);setContentView(scroll);toggle.requestFocus();
    }
    @Override protected void onResume(){super.onResume();try{getContentResolver().registerContentObserver(STATUS,true,observer);}catch(SecurityException unavailable){}refresh();}
    @Override protected void onPause(){getContentResolver().unregisterContentObserver(observer);super.onPause();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(6),0,dp(8));return v;}
    private Button button(LinearLayout parent,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setTextSize(17);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setPadding(dp(20),0,dp(20),0);StateListDrawable bg=new StateListDrawable();GradientDrawable focused=new GradientDrawable();focused.setColor(0xff286861);focused.setCornerRadius(dp(10));focused.setStroke(dp(2),0xff91f2dc);GradientDrawable plain=new GradientDrawable();plain.setColor(0xff182535);plain.setCornerRadius(dp(10));bg.addState(new int[]{android.R.attr.state_focused},focused);bg.addState(new int[]{},plain);b.setBackground(bg);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.topMargin=dp(9);parent.addView(b,p);b.setOnClickListener(v->action.run());return b;}
    private void refresh(){
        try(Cursor c=getContentResolver().query(STATUS,null,null,null,null)){
            if(c==null||!c.moveToFirst())throw new IllegalStateException();
            String value=c.getString(c.getColumnIndexOrThrow("state"));active="ready".equals(value)||"connecting".equals(value);
            int pendingColumn=c.getColumnIndex("pendingCaptcha");boolean pending=pendingColumn>=0&&c.getInt(pendingColumn)!=0;active=active||pending;captcha.setVisibility(pending?android.view.View.VISIBLE:android.view.View.GONE);
            String mode=c.getString(c.getColumnIndexOrThrow("mode"));state.setText("Blink · "+c.getString(c.getColumnIndexOrThrow("label")));detail.setText(c.getString(c.getColumnIndexOrThrow("detail"))+"\nРежим: "+("profile".equals(mode)?"Обычный VPN-профиль":"direct".equals(mode)?"Прямой UDP — проверка сервера":"VK TURN"));
            long expiry=c.getLong(c.getColumnIndexOrThrow("expiresAt"));subscription.setText(c.getString(c.getColumnIndexOrThrow("profile"))+(expiry>0?"\nДо "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(expiry*1000)):""));
            diagnostic.setText(c.getString(c.getColumnIndexOrThrow("probe")));toggle.setText(active?"Отключить туннель":"Подключить туннель");
        }catch(Exception unavailable){captcha.setVisibility(android.view.View.GONE);active=false;state.setText("Blink не настроен");detail.setText("Установите клиент Blink с той же подписью, что и лаунчер.");subscription.setText("Подписка не добавлена");diagnostic.setText("");toggle.setText("Открыть Blink");}
    }
    private void toggle(){try{Bundle result=getContentResolver().call(STATUS,"toggle",null,null);if(result==null||result.getBoolean("needsConsent")||result.getBoolean("needsSubscription"))openClient("connection",true);else refresh();}catch(Exception e){openClient("connection",false);}}
    private void openClient(String screen,boolean requestConnect){try{startActivityForResult(new Intent().setComponent(CLIENT).putExtra("screen",screen).putExtra("requestConnect",requestConnect),48);}catch(Exception e){Toast.makeText(this,"Клиент Blink ещё не установлен",Toast.LENGTH_LONG).show();}}
}

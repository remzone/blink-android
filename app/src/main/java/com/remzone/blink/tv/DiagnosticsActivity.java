package com.remzone.blink.tv;

import android.app.Activity;
import android.net.*;
import android.os.*;
import android.widget.*;
import java.util.concurrent.*;

/** Each probe binds to the explicitly chosen network; failures never switch paths. */
public final class DiagnosticsActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());
    private TextView direct,tunnel,google,youtube;private boolean busy;private Button ownButton,vpnButton,googleButton,youtubeButton;
    private String directResult="Ещё не проверен",tunnelResult="Ещё не проверен";
    @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout page=BlinkStyle.page(this,"BLINK  /  Диагностика","Два маршрута · проверяем сеть, а не только значок VPN");
        LinearLayout ips=BlinkStyle.card(this,page,"Внешний IP");direct=BlinkStyle.text(this,"Обычная сеть: "+directResult,18,BlinkStyle.TEXT);ips.addView(direct);ownButton=BlinkStyle.button(this,ips,"Проверить мой IP · Wi-Fi / Ethernet",()->probe(false,"www.cloudflare.com","/cdn-cgi/trace",true,direct));tunnel=BlinkStyle.text(this,"Туннель: "+tunnelResult,18,BlinkStyle.ACCENT);ips.addView(tunnel);vpnButton=BlinkStyle.button(this,ips,"Проверить IP туннеля",()->probe(true,"www.cloudflare.com","/cdn-cgi/trace",true,tunnel));
        LinearLayout checks=BlinkStyle.card(this,page,"Проверка после подключения");google=BlinkStyle.text(this,"Google: ещё не проверен",16,BlinkStyle.TEXT);checks.addView(google);googleButton=BlinkStyle.button(this,checks,"Google · HTTPS-пинг через туннель",()->probe(true,"www.google.com","/generate_204",false,google));youtube=BlinkStyle.text(this,"YouTube: ещё не проверен",16,BlinkStyle.TEXT);checks.addView(youtube);youtubeButton=BlinkStyle.button(this,checks,"Проверить доступ к YouTube через туннель",()->probe(true,"www.youtube.com","/generate_204",false,youtube));checks.addView(BlinkStyle.text(this,"HTTPS-пинг проверяет DNS, TCP, TLS и ответ сервиса. Это не ICMP и не замер скорости видео. Мой IP проверяется напрямую по вашему нажатию; остальные проверки требуют VPN.",13,BlinkStyle.MUTED));BlinkStyle.button(this,page,"Назад",this::finish);
    }
    private Network network(boolean vpn)throws Exception{ConnectivityManager cm=getSystemService(ConnectivityManager.class);if(vpn){Network n=cm.getActiveNetwork();NetworkCapabilities c=cm.getNetworkCapabilities(n);if(n==null||c==null||!c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)||!"ready".equals(StateStore.state))throw new Exception();return n;}for(Network n:cm.getAllNetworks()){NetworkCapabilities c=cm.getNetworkCapabilities(n);if(c!=null&&!c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)&&c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))return n;}throw new Exception();}
    private void probe(boolean vpn,String host,String path,boolean wantIP,TextView target){if(busy)return;busy=true;enabled(false);target.setText("Проверяем "+(vpn?"туннель":"обычную сеть")+"…");worker.execute(()->{String message;try{Network selected=network(vpn);NetworkProbe.Result r=NetworkProbe.measure(selected,host,path,wantIP,!vpn);if(!selected.equals(network(vpn)))throw new Exception();String now=java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(new java.util.Date());message=(wantIP?r.ip:host+" · доступен")+" · "+r.totalMs+" мс · "+now+"\n"+r.summary();if(wantIP){if(vpn)tunnelResult=message;else directResult=message;}}catch(Exception e){message=(vpn?"Туннель":"Обычная сеть")+": проверка не прошла"+(e instanceof NetworkProbe.Failure?"\n"+((NetworkProbe.Failure)e).summary():". Проверьте подключение.");}String result=message;main.post(()->{busy=false;if(isDestroyed())return;target.setText(result);enabled(true);});});}
    private void enabled(boolean enabled){ownButton.setEnabled(enabled);vpnButton.setEnabled(enabled);googleButton.setEnabled(enabled);youtubeButton.setEnabled(enabled);}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}

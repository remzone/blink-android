package com.remzone.blink.tv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.VpnService;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import org.json.JSONObject;
import java.io.FileDescriptor;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import mobile.Engine;
import mobile.Mobile;

public final class TunnelService extends VpnService {
    private static volatile TunnelService live;
    static JSONObject captcha(){try{TunnelService s=live;Engine e=s==null?null:s.engine;return e==null?new JSONObject():new JSONObject(e.captchaJSON());}catch(Exception ignored){return new JSONObject();}}
    static boolean hasPendingCaptcha(){return captcha().has("id");}
    static boolean answerCaptcha(String id,String kind,String value){TunnelService s=live;Engine e=s==null?null:s.engine;return e!=null&&e.submitCaptcha(id,kind,value);}
    static final String RESTART="com.remzone.blink.RESTART";
    static final String START="com.remzone.blink.START", STOP="com.remzone.blink.STOP";
    private final ExecutorService control=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Object outputLock=new Object();
    private volatile Engine engine;private volatile ParcelFileDescriptor tun;
    private volatile boolean running;private Thread packets;
    private JSONObject importedProfile;private String profileName="Blink";private volatile boolean establishing;
    private long unreadySince;private String lastNative="";
    static boolean protectDiagnostic(java.net.Socket socket){TunnelService service=live;return service==null||service.protect(socket);}
    @Override public void onCreate(){super.onCreate();live=this;NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("tunnel","VPN-соединение",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&STOP.equals(intent.getAction())){control.execute(()->finishTunnel("stopped","Туннель отключён"));return START_NOT_STICKY;}
        startForeground(17,notification("Подключение…"));if(intent!=null&&RESTART.equals(intent.getAction()))control.execute(()->{closeResources();startTunnel();});else control.execute(this::startTunnel);return START_NOT_STICKY;
    }
    private Notification notification(String text){
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,NetworkActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,TunnelService.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,"tunnel").setSmallIcon(com.remzone.blink.tv.R.drawable.ic_blink).setContentTitle("Blink · "+text).setContentText("Нажмите, чтобы открыть сетевые настройки").setOngoing(true).setContentIntent(open).addAction(new Notification.Action.Builder(null,"Отключить",stop).build()).build();
    }
    private void startTunnel(){
        if(running)return;
        try{
            if(VpnService.prepare(this)!=null)throw new Exception("Подтвердите разрешение VPN в сетевых настройках");
            JSONObject saved=new SecureStore(this).load();
            String mode=saved.optString("mode","vk");
            StateStore.mode=mode;StateStore.turnTransport=saved.optString("turnTransport","tcp");StateStore.tx=0;StateStore.rx=0;StateStore.egress="";StateStore.probe="Проверка ещё не выполнялась";StateStore.timings="";importedProfile=null;
            if("profile".equals(mode)){
                importedProfile=new JSONObject(Mobile.importProfile(ProfilesStore.selected(saved).getString("source")));
                profileName=importedProfile.getString("name");StateStore.set(this,"connecting","Подготовка обычного VPN-профиля…");
                engine=Mobile.startProfile(importedProfile.toString(),this::writePacket,fd->protect((int)fd));
            }else{
                if(!mode.equals("vk")&&!mode.equals("direct"))throw new Exception("Выберите режим туннеля");
                Subscription s=Subscription.parse(saved.getString("document"),System.currentTimeMillis()/1000,saved.optLong("revision",0));
                int selected=saved.optInt("selected",0);if(selected<0||selected>=s.profiles.size())throw new Exception("Выберите профиль подписки");
                StateStore.set(this,"connecting",mode.equals("vk")?"Авторизация VK и подключение TURN…":"Прямое DTLS-соединение с UDP-сервером…");
                engine=Mobile.startVK(s.profiles.get(selected).nativeJSON(mode,"1.1.1.1",s.expiresAt,saved.optString("turnTransport","tcp")),this::writePacket,fd->protect((int)fd));
                profileName=s.profiles.get(selected).name;
            }
            running=true;unreadySince=android.os.SystemClock.elapsedRealtime();lastNative="";main.post(monitor);
        }catch(Exception ex){finishTunnel("error",safeError(ex));}
    }
    // Keep ordinary networking until authentication and the transport are ready.
    private void establishTun(){
        try{if(!running||engine==null||tun!=null||!("ready".equals(engine.status())||"profile".equals(StateStore.mode)&&"running".equals(engine.status())))return;
            Builder b=new Builder().setSession("Blink · "+profileName).setMtu(importedProfile==null?1280:importedProfile.getInt("mtu")).setBlocking(false).setConfigureIntent(PendingIntent.getActivity(this,0,new Intent(this,NetworkActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
            if(importedProfile==null||"xray".equals(importedProfile.optString("kind"))){
                b.addAddress("10.77.0.2",32).addDnsServer("1.1.1.1");for(String route:RoutePolicy.ipv4())addRoute(b,route);
            }else{
                org.json.JSONArray addresses=importedProfile.getJSONArray("addresses"),dns=importedProfile.getJSONArray("dns"),routes=importedProfile.getJSONArray("routes");
                for(int i=0;i<addresses.length();i++){String[] a=addresses.getString(i).split("/");b.addAddress(a[0],Integer.parseInt(a[1]));}
                for(int i=0;i<routes.length();i++){String route=routes.getString(i);if("0.0.0.0/0".equals(route)){for(String r:RoutePolicy.wireGuardDefault(localNetworks()))addRoute(b,r);}else addRoute(b,route);}
                for(int i=0;i<dns.length();i++){String address=dns.getString(i);b.addDnsServer(address);b.addRoute(address,address.contains(":")?128:32);}
            }
            // Xray/VK stay IPv4-only. Explicit WG .conf addresses/routes control its families.
            tun=b.establish();if(tun==null)throw new Exception("Система не разрешила создать VPN-интерфейс");
            ConnectivityManager cm=getSystemService(ConnectivityManager.class);
            for(Network n:cm.getAllNetworks()){NetworkCapabilities caps=cm.getNetworkCapabilities(n);if(caps!=null&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&!caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)){setUnderlyingNetworks(new Network[]{n});break;}}
            packets=new Thread(this::readPackets,"Blink-TUN");packets.start();
        }catch(Exception ex){finishTunnel("error",safeError(ex));}finally{establishing=false;}
    }
    private java.util.List<String> localNetworks(){
        java.util.List<String> result=new java.util.ArrayList<>();ConnectivityManager cm=getSystemService(ConnectivityManager.class);
        for(Network network:cm.getAllNetworks()){NetworkCapabilities caps=cm.getNetworkCapabilities(network);if(caps==null||caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)||!(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))continue;android.net.LinkProperties links=cm.getLinkProperties(network);if(links!=null)for(android.net.LinkAddress a:links.getLinkAddresses())if(a.getAddress() instanceof java.net.Inet4Address)result.add(a.getAddress().getHostAddress()+"/"+a.getPrefixLength());}
        return result;
    }
    private static void addRoute(Builder b,String route){String[] p=route.split("/");b.addRoute(p[0],Integer.parseInt(p[1]));}
    private void closeTun(){
        ParcelFileDescriptor old; synchronized(outputLock){old=tun;tun=null;}
        if(old!=null)try{old.close();}catch(Exception ignored){}
        Thread reader=packets;packets=null;if(reader!=null&&reader!=Thread.currentThread())try{reader.join(1500);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
    }
    private boolean writePacket(byte[] packet){
        synchronized(outputLock){ParcelFileDescriptor current=tun;if(!running||current==null)return false;try{return Os.write(current.getFileDescriptor(),packet,0,packet.length)==packet.length;}catch(ErrnoException | java.io.InterruptedIOException ex){return false;}}
    }
    private void readPackets(){
        byte[] buffer=new byte[65535];
        ParcelFileDescriptor current=tun;try{
            if(current==null)return;FileDescriptor fd=current.getFileDescriptor();
            StructPollfd poll=new StructPollfd();poll.fd=fd;poll.events=(short)OsConstants.POLLIN;
            while(running&&tun==current){
                if(Os.poll(new StructPollfd[]{poll},500)<=0)continue;
                if(!running||tun!=current)break;
                int length;try{length=Os.read(fd,buffer,0,buffer.length);}catch(ErrnoException ex){if(ex.errno==OsConstants.EAGAIN||ex.errno==OsConstants.EINTR)continue;throw ex;}
                if(length<=0)break;Engine currentEngine=engine;if(currentEngine!=null)currentEngine.input(Arrays.copyOf(buffer,length));
            }
        }catch(Exception ex){if(running&&tun==current)control.execute(()->finishTunnel("error","VPN-интерфейс был закрыт системой"));}
    }
    private final Runnable monitor=new Runnable(){@Override public void run(){
        Engine current=engine;if(!running||current==null)return;
        try{
            String state=current.status();if("profile".equals(StateStore.mode)&&"running".equals(state))state="ready";JSONObject counters=new JSONObject(current.statsJSON());StateStore.tx=counters.optLong("tx");StateStore.rx=counters.optLong("rx");
            if("captcha_required".equals(state)){
                if(!hasPendingCaptcha()){control.execute(()->finishTunnel("captcha_required","VK не выдал доступную проверку или её время истекло. Подключитесь заново."));return;}
                if(tun!=null)control.execute(TunnelService.this::closeTun);
                unreadySince=0;lastNative=state;StateStore.set(TunnelService.this,state,"Откройте «Пройти проверку VK». Обычная сеть доступна; после проверки продолжится тот же запрос.");
                getSystemService(NotificationManager.class).notify(17,notification("Пройдите проверку VK"));main.postDelayed(this,1000);return;
            }
            if("ready".equals(state)&&tun==null){if(!establishing){establishing=true;control.execute(TunnelService.this::establishTun);}main.postDelayed(this,300);return;}

            if("stopped".equals(state)){control.execute(()->finishTunnel("error","Срок подписки закончился или ядро остановлено. Обновите подписку."));return;}
            if("ready".equals(state))unreadySince=0;else if(unreadySince==0)unreadySince=android.os.SystemClock.elapsedRealtime();
            if(!state.equals(lastNative)){
                lastNative=state;StateStore.set(TunnelService.this,"ready".equals(state)?"ready":"connecting","ready".equals(state)?("profile".equals(StateStore.mode)?"Настройки VPN применены. Проверьте интернет; это не подтверждение удалённого соединения.":"vk".equals(StateStore.mode)?"VK TURN ("+StateStore.turnTransport.toUpperCase(java.util.Locale.ROOT)+") готов. Проверьте доступ в интернет.":"Прямой DTLS-туннель готов. Это не проверка инфраструктуры VK."):"Восстановление соединения…");
                getSystemService(NotificationManager.class).notify(17,notification(StateStore.label()));
            }else StateStore.notifyChanged(TunnelService.this);
            if(!"ready".equals(state)&&unreadySince>0&&android.os.SystemClock.elapsedRealtime()-unreadySince>45000){control.execute(()->finishTunnel("error","Сервер не ответил за 45 секунд. Обычная сеть восстановлена."));return;}
            main.postDelayed(this,1000);
        }catch(Exception ex){control.execute(()->finishTunnel("error","Не удалось получить состояние VPN-ядра"));}
    }};
    private void finishTunnel(String state,String detail){closeResources();StateStore.set(this,state,detail);stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    private void closeResources(){
        running=false;main.removeCallbacks(monitor);closeTun();
        Engine oldEngine=engine;engine=null;if(oldEngine!=null)oldEngine.stop();

    }
    @Override public void onRevoke(){control.execute(()->finishTunnel("stopped","Разрешение VPN отозвано системой"));}
    @Override public void onDestroy(){main.removeCallbacks(monitor);if(live==this)live=null;control.execute(this::closeResources);control.shutdown();super.onDestroy();}
    private static String safeError(Exception ex){String msg=ex.getMessage();if(msg!=null&&(msg.startsWith("Подписка")||msg.startsWith("Выберите")||msg.startsWith("Система")||msg.startsWith("Подтвердите")||msg.startsWith("Не удалось прочитать")))return msg;return "Не удалось запустить VPN. Проверьте подписку и выбранный режим.";}
}

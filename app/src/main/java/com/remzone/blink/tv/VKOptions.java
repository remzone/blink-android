package com.remzone.blink.tv;

import java.net.URI;
import org.json.JSONObject;

/** Local tuning only; defaults preserve wire framing and use one relay allocation. */
final class VKOptions {
    String transport="auto",preset="compatibility",dns="1.1.1.1",relay="";
    int connections=1,mtu=1200,pace=0,window=64,interval=20;
    boolean congestion=true,serverDefaults=true;
    static VKOptions from(JSONObject saved){
        VKOptions v=new VKOptions();JSONObject o=saved.optJSONObject("vkOptions");
        if(o==null){v.transport=Subscription.transport(saved);return v;}
        v.serverDefaults=o.optBoolean("serverDefaults",true);v.transport=o.optString("transport","auto");v.preset=o.optString("preset","compatibility");v.dns=o.optString("dns","1.1.1.1");v.relay=o.optString("relay","");
        v.connections=o.optInt("connections",1);v.mtu=o.optInt("mtu",1200);v.pace=o.optInt("pace",0);v.window=o.optInt("window",64);v.interval=o.optInt("interval",20);v.congestion=o.optBoolean("congestion",true);return v;
    }
    boolean valid(){return java.util.Arrays.asList("auto","tcp","udp").contains(transport)&&java.util.Arrays.asList("compatibility","balanced","conservative","custom").contains(preset)&&connections>=0&&connections<=16&&mtu>=576&&mtu<=1200&&(pace==0||pace>=16&&pace<=4096)&&window>=16&&window<=256&&interval>=10&&interval<=100&&ip(dns)&&relay(relay);}
    static boolean ip(String s){if(s.matches("[0-9.]+")){String[] a=s.split("\\.",-1);if(a.length!=4)return false;for(String p:a)try{if(!p.matches("[0-9]{1,3}")||Integer.parseInt(p)>255)return false;}catch(Exception e){return false;}return true;}if(!s.matches("[0-9a-fA-F:]+")||!s.contains(":"))return false;try{return java.net.InetAddress.getByName(s) instanceof java.net.Inet6Address;}catch(Exception e){return false;}}
    static boolean relay(String s){if(s.isEmpty())return true;try{URI u=new URI("turn://"+s);return s.length()<=260&&u.getHost()!=null&&u.getPort()>0&&u.getPort()<=65535&&u.getUserInfo()==null&&u.getPath().isEmpty()&&u.getQuery()==null&&u.getFragment()==null;}catch(Exception e){return false;}}
    JSONObject json()throws Exception{return new JSONObject().put("serverDefaults",serverDefaults).put("transport",transport).put("preset",preset).put("dns",dns).put("relay",relay).put("connections",connections).put("mtu",mtu).put("pace",pace).put("window",window).put("interval",interval).put("congestion",congestion);}
    String nativeJSON(Subscription.Profile p,String mode,long expiry)throws Exception{
        if(serverDefaults){
            VKOptions remote=p.options==null?new VKOptions():from(new JSONObject().put("vkOptions",p.options));
            remote.serverDefaults=false;remote.connections=0;return remote.nativeJSON(p,mode,expiry);
        }
        if(!valid())throw new Exception("Неверные параметры TURN");
        JSONObject tuning=new JSONObject().put("preset",preset).put("mtu",mtu).put("paceKiB",pace).put("window","custom".equals(preset)?window:0).put("intervalMs","custom".equals(preset)?interval:0).put("congestion","custom".equals(preset)&&congestion);
        return new JSONObject(p.nativeJSON(mode,dns,expiry,transport)).put("turnServer",relay).put("streams",connections==0?p.streams:connections).put("tuning",tuning).toString();
    }
}

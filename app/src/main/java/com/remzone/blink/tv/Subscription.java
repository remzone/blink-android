package com.remzone.blink.tv;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class Subscription {
    final long revision,issuedAt,expiresAt; final List<Profile> profiles;
    Subscription(long r,long i,long e,List<Profile> p){revision=r;issuedAt=i;expiresAt=e;profiles=p;}
    static final class Profile {
        final String id,name,uuid,host,call;final int port,streams;
        Profile(String i,String n,String u,String h,int p,String c,int s){id=i;name=n;uuid=u;host=h;port=p;call=c;streams=s;}
        String nativeJSON(String mode,String dns,long expiry)throws Exception{
            return new JSONObject().put("uuid",uuid).put("peerHost",host).put("peerPort",port).put("vkCallURL",call).put("streams",streams).put("dnsServer",dns).put("mode",mode).put("expiresAt",expiry).toString();
        }
    }
    static String validateURL(String raw)throws Exception{
        if(raw==null||raw.length()>2048)throw new Exception("Нужна HTTPS-ссылка подписки Vokop");
        URI u=new URI(raw);
        if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getFragment()!=null||u.getRawQuery()!=null||!u.getRawPath().matches("(?:/[^/]+)*/api/vokop/v1/subscriptions/[A-Za-z0-9_-]{32,128}"))throw new Exception("Нужна полная HTTPS-ссылка подписки Vokop, а не адрес панели");
        return raw;
    }
    static Subscription parse(String raw,long now,long minimumRevision)throws Exception{
        JsonGuard.validate(raw);JSONObject o=new JSONObject(raw);
        if(!"vokop.subscription.v1".equals(str(o,"schema")))throw new Exception("Неподдерживаемый формат подписки");
        long r=integer(o,"revision"),i=integer(o,"issued_at"),e=integer(o,"expires_at");
        if(r<1||r<minimumRevision||i<0||i>now+300||e<=now||e<=i||e-i>2678400)throw new Exception("Подписка истекла, имеет неверный срок или устаревшую версию");
        Object list=o.get("profiles");if(!(list instanceof JSONArray))throw new Exception("Некорректный список профилей");JSONArray a=(JSONArray)list;
        if(a.length()<1||a.length()>16)throw new Exception("В подписке должно быть от 1 до 16 профилей");
        List<Profile> profiles=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(int n=0;n<a.length();n++){
            JSONObject p=a.getJSONObject(n),v=p.getJSONObject("vless"),t=p.getJSONObject("turn");
            String id=str(p,"id"),name=str(p,"name"),uuid=str(v,"uuid"),host=str(t,"peer_host"),call=str(t,"vk_call_url");
            long port=integer(t,"peer_port"),streams=integer(t,"streams");
            if(!id.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,63}")||!ids.add(id)||name.codePointCount(0,name.length())>100||name.codePoints().anyMatch(Character::isISOControl)||!uuid.matches("[a-fA-F0-9]{8}-(?:[a-fA-F0-9]{4}-){3}[a-fA-F0-9]{12}")||!validHost(host)||port<1||port>65535||streams<1||streams>16||call.length()>2048||!call.matches("https://(?:www\\.)?vk\\.(?:com|ru)/call/join/[A-Za-z0-9_=-]+"))throw new Exception("Некорректный профиль Vokop");
            profiles.add(new Profile(id,name,uuid,host,(int)port,call,(int)streams));
        }
        return new Subscription(r,i,e,profiles);
    }
    private static String str(JSONObject o,String k)throws Exception{Object v=o.get(k);if(!(v instanceof String))throw new Exception("Неверный тип поля подписки");return (String)v;}
    private static long integer(JSONObject o,String k)throws Exception{Object v=o.get(k);if(!(v instanceof Number))throw new Exception("Неверный тип числа");double d=((Number)v).doubleValue();if(!Double.isFinite(d)||Math.rint(d)!=d||Math.abs(d)>9007199254740991d)throw new Exception("Некорректное число");return ((Number)v).longValue();}
    static boolean validHost(String h){
        if(h.length()>253||!h.contains(".")||h.toLowerCase(java.util.Locale.ROOT).endsWith(".localhost"))return false;
        if(h.matches("[0-9.]+")){
            String[] parts=h.split("\\.",-1);if(parts.length!=4)return false;int[] b=new int[4];
            try{for(int k=0;k<4;k++){if(!parts[k].matches("0|[1-9][0-9]{0,2}"))return false;b[k]=Integer.parseInt(parts[k]);if(b[k]>255)return false;}}catch(NumberFormatException ex){return false;}
            return b[0]>0&&b[0]<224&&b[0]!=10&&b[0]!=127&&!(b[0]==172&&b[1]>=16&&b[1]<=31)&&!(b[0]==192&&b[1]==168)&&!(b[0]==169&&b[1]==254)&&!(b[0]==100&&b[1]>=64&&b[1]<=127);
        }
        for(String p:h.split("\\.",-1))if(!p.matches("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"))return false;return true;
    }
}

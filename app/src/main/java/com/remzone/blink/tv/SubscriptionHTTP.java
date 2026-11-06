package com.remzone.blink.tv;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;

final class SubscriptionHTTP {
    static String fetch(String url)throws Exception{
        Subscription.validateURL(url);return fetchHTTPS(url);
    }
    static String fetchStandard(String raw)throws Exception{
        java.net.URI u=new java.net.URI(raw);
        if(raw.length()>2048||!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getFragment()!=null)throw new Exception("Нужна HTTPS-ссылка подписки");
        return fetchHTTPS(raw);
    }
    private static String fetchHTTPS(String url)throws Exception{
        HttpsURLConnection c=(HttpsURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
        c.setRequestProperty("Accept","application/json");c.setRequestProperty("Cache-Control","no-store");
        c.setRequestProperty("User-Agent","BlinkAndroidTV/0.1");
        try{
            int status=c.getResponseCode();
            if(status==401||status==403)throw new Exception("Доступ к подписке закрыт. Проверьте её у администратора.");
            if(status>=300&&status<400)throw new Exception("Перенаправление ссылки подписки запрещено");
            if(status!=200)throw new Exception("Панель временно не выдала подписку");
            if(c.getContentLengthLong()>131072)throw new Exception("Ответ панели слишком большой");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1){if(out.size()+count>131072)throw new Exception("Ответ панели слишком большой");out.write(buffer,0,count);}
                return out.toString(StandardCharsets.UTF_8.name());
            }
        }catch(javax.net.ssl.SSLException e){throw new Exception("HTTPS-сертификат панели не прошёл проверку");}
        catch(java.io.IOException e){throw new Exception("Не удалось связаться с панелью. Проверьте сеть и ссылку.");}
        finally{c.disconnect();}
    }
}

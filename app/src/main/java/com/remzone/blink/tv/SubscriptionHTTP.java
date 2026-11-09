package com.remzone.blink.tv;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;

final class SubscriptionHTTP {
    static final class Failure extends Exception { Failure(String message){super(message);} }
    interface ConnectionFactory { HttpsURLConnection open(URL url) throws Exception; }
    static String fetch(String url)throws Exception{
        Subscription.validateURL(url);return fetchHTTPS(url,"application/json",u->(HttpsURLConnection)u.openConnection());
    }
    static URI validateURL(String raw)throws Exception{
        URI u;try{u=new URI(raw);}catch(Exception e){throw new Failure("Некорректная ссылка подписки.");}
        if(raw.length()>4096||!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getFragment()!=null)
            throw new Failure("Нужна полная HTTPS-ссылка подписки без логина и пароля.");
        return u;
    }
    static String fetchStandard(String raw)throws Exception{
        return fetchHTTPS(raw,"*/*",u->(HttpsURLConnection)u.openConnection());
    }
    static boolean allowsRedirect(URI original,URI target,int count){
        try{
            validateURL(target.toString());
            return count>=1&&count<=5&&original.getHost().equalsIgnoreCase(target.getHost())&&port(original)==port(target);
        }catch(Exception invalid){return false;}
    }
    private static int port(URI u){return u.getPort()<0?443:u.getPort();}
    static final class Document {final String body;final org.json.JSONObject metadata;Document(String b,org.json.JSONObject m){body=b;metadata=m;}}
    static Document fetchStandardDocument(String raw)throws Exception{return fetchDocument(raw,"*/*",u->(HttpsURLConnection)u.openConnection());}
    static String fetchHTTPS(String raw,String accept,ConnectionFactory factory)throws Exception{return fetchDocument(raw,accept,factory).body;}
    static Document fetchDocument(String raw,String accept,ConnectionFactory factory)throws Exception{
        URI original=validateURL(raw),current=original;
        long deadline=System.nanoTime()+30_000_000_000L;
        for(int redirects=0;;){
            if(System.nanoTime()>=deadline)throw new Failure("Сервер не ответил вовремя.");
            HttpsURLConnection c=factory.open(current.toURL());
            c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
            c.setRequestProperty("Accept-Encoding","identity");
            c.setRequestProperty("Accept",accept);c.setRequestProperty("Cache-Control","no-store");
            c.setRequestProperty("User-Agent","BlinkAndroidTV/0.1");
            try{
                int status=c.getResponseCode();
                if(status==401||status==403||status==404)throw new Failure("Подписка недоступна. Проверьте актуальную ссылку у провайдера.");
                if(status==301||status==302||status==303||status==307||status==308){
                    URI target;try{target=current.resolve(c.getHeaderField("Location"));}catch(Exception e){throw new Failure("Сервер вернул некорректное перенаправление.");}
                    if(!allowsRedirect(original,target,++redirects))throw new Failure("Сервер перенаправляет подписку на другой адрес или по кругу.");
                    current=target;continue;
                }
                if(status!=200)throw new Failure("Сервер подписки вернул HTTP "+status+".");
                if(c.getContentLengthLong()>1048576)throw new Failure("Ответ подписки превышает 1 МБ.");
                try(InputStream rawStream=c.getInputStream();
                    InputStream in="gzip".equalsIgnoreCase(c.getContentEncoding())?new GZIPInputStream(rawStream):rawStream;
                    ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[4096];int count;
                    while((count=in.read(buffer))!=-1){
                        if(System.nanoTime()>=deadline)throw new Failure("Сервер не ответил вовремя.");
                        if(out.size()+count>1048576)throw new Failure("Ответ подписки превышает 1 МБ.");out.write(buffer,0,count);
                    }
                    String body=out.toString(StandardCharsets.UTF_8.name());
                    if(!accept.equals("application/json"))SubscriptionFeedNotice.validate(body);
                    java.util.Map<String,String> headers=new java.util.HashMap<>();for(String key:new String[]{"profile-title","announce","subscription-userinfo"})headers.put(key,c.getHeaderField(key));
                    return new Document(body,SubscriptionMetadata.parse(headers,body));
                }
            }catch(javax.net.ssl.SSLException e){throw new Failure("HTTPS-сертификат сервера не прошёл проверку.");}
            catch(java.net.SocketTimeoutException e){throw new Failure("Сервер не ответил вовремя.");}
            catch(java.io.IOException e){throw new Failure("Не удалось связаться с сервером подписки. Проверьте сеть.");}
            finally{c.disconnect();}
        }
    }
}

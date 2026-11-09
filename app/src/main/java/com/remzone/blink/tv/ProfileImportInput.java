package com.remzone.blink.tv;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Unwrap only a complete import link; never decode proxy credentials or config text. */
final class ProfileImportInput {
    final String source;
    final String subscriptionURL;
    private ProfileImportInput(String source, String url) { this.source=source; this.subscriptionURL=url; }
    static ProfileImportInput parse(String source) throws Exception { return resolve(source,0); }
    private static String decode(String value) throws Exception {
        return URLDecoder.decode(value.replace("+","%2B"), StandardCharsets.UTF_8.name());
    }
    private static ProfileImportInput resolve(String value,int depth) throws Exception {
        if(depth>=4)throw new SubscriptionHTTP.Failure("Слишком много вложенных ссылок импорта.");
        if(value.getBytes(StandardCharsets.UTF_8).length>1048576)throw new SubscriptionHTTP.Failure("Конфигурация превышает 1 МБ.");
        String text=value.trim();if(text.startsWith("\uFEFF"))text=text.substring(1).trim();
        if(text.isEmpty())throw new SubscriptionHTTP.Failure("Вставьте ссылку или конфигурацию.");
        if(!text.contains("\n")&&!text.contains("\r")){
            if(text.startsWith("<")&&text.endsWith(">"))text=text.substring(1,text.length()-1);
            int split=text.indexOf("](");if(text.startsWith("[")&&text.endsWith(")")&&split>=0)text=text.substring(split+2,text.length()-1);
            if(text.toLowerCase(Locale.ROOT).startsWith("hiddify://import/")){
                String nested=text.substring("hiddify://import/".length());
                return resolve(nested.contains("://")?nested:decode(nested),depth+1);
            }
            URI uri;try{int hash=text.indexOf('#');uri=new URI(hash<0?text:text.substring(0,hash));}catch(Exception invalid){return new ProfileImportInput(text,null);}
            String scheme=uri.getScheme()==null?"":uri.getScheme().toLowerCase(Locale.ROOT);
            if(scheme.equals("clash")||scheme.equals("clashmeta")||scheme.equals("hiddify")){
                String action=uri.getHost();boolean supported="install-config".equals(action)||(scheme.equals("hiddify")&&("install-sub".equals(action)||"install-proxy".equals(action)));
                String nested=null;int count=0;
                if(uri.getRawQuery()!=null)for(String pair:uri.getRawQuery().split("&")){String[] kv=pair.split("=",2);if(decode(kv[0]).equals("url")){count++;nested=kv.length==2?decode(kv[1]):"";}}
                if(!supported||count!=1)throw new SubscriptionHTTP.Failure("Некорректная ссылка импорта подписки.");
                return resolve(nested,depth+1);
            }
            if(scheme.equals("https")||scheme.equals("http")){
                // Older copied HTTP links use the provider's verified HTTPS endpoint.
                int fragment=text.indexOf('#');String normalized="https"+text.substring(text.indexOf(':'));
                if(fragment>=0)normalized=normalized.substring(0,normalized.indexOf('#'));
                SubscriptionHTTP.validateURL(normalized);
                return new ProfileImportInput(normalized,normalized);
            }
        }
        return new ProfileImportInput(text,null);
    }
}

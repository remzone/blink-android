package com.remzone.blink.tv;
import java.net.URI;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
final class SubscriptionFeedNotice {
    static void validate(String source)throws SubscriptionHTTP.Failure{
        String text=source.trim();if(text.startsWith("\uFEFF"))text=text.substring(1);
        if(!text.contains("://"))try{text=new String(Base64.getDecoder().decode(text.replaceAll("\\s","").replace('-','+').replace('_','/')),StandardCharsets.UTF_8);}catch(IllegalArgumentException invalid){return;}
        String node=null;
        for(String line:text.split("\\r?\\n")){line=line.trim();if(line.isEmpty()||line.startsWith("#"))continue;if(node!=null)return;node=line;}
        if(node==null)return;
        boolean reissued=false;
        try{
            int fragment=node.indexOf('#');
            URI u=new URI(fragment<0?node:node.substring(0,fragment));String host=u.getHost();
            if(!"vless".equals(u.getScheme())||u.getPort()!=1||!("127.0.0.1".equals(host)||"localhost".equalsIgnoreCase(host)||"[::1]".equals(host)||"::1".equals(host)))return;
            if(u.getRawQuery()!=null)for(String pair:u.getRawQuery().split("&"))if(pair.equals("x-durev-reissue=true"))reissued=true;
        }catch(Exception invalid){return;}
        if(reissued)throw new SubscriptionHTTP.Failure("Ключ подписки перевыпущен. Получите новую ссылку в боте провайдера.");
    }
}

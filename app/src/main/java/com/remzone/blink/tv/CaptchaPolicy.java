package com.remzone.blink.tv;

import java.net.URI;
import java.util.Locale;

/** Only VK's HTTPS challenge origins may reach the SDK, never arbitrary feed URLs. */
final class CaptchaPolicy {
    static String origin(String raw)throws Exception{
        if(raw==null||raw.length()>16384)throw new Exception("Некорректная ссылка проверки TURN");
        URI u=new URI(raw);String host=u.getHost();if(host==null)throw new Exception("Некорректный адрес TURN");host=host.toLowerCase(Locale.ROOT);
        if(!"https".equals(u.getScheme())||u.getRawUserInfo()!=null||u.getRawFragment()!=null||(u.getPort()!=-1&&u.getPort()!=443)||!(host.equals("vk.ru")||host.equals("vk.com")||host.endsWith(".vk.ru")||host.endsWith(".vk.com")))throw new Exception("Проверка должна открываться на доверенном HTTPS-сайте провайдера");
        return "https://"+host+(u.getPort()==443?":443":"");
    }
}

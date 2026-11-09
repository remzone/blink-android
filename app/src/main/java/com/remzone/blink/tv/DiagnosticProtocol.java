package com.remzone.blink.tv;

import java.net.URI;
import java.util.*;
import org.json.*;

/** Only numeric allowlisted evidence is eligible for storage/upload. */
final class DiagnosticProtocol {
    static final Set<String> EVENTS=new HashSet<>(Arrays.asList(("app.launch app.foreground app.background connection.begin connection.failed connection.cancel connection.state connection.disconnected provider.error captcha.phase subscription.fetch subscription.result upload.state extension.start extension.core_started extension.routes_ready extension.heartbeat extension.stop extension.failure network.probe native.xray.error native.wireguard.error native.vk.request native.vk.response native.vk.credentials_begin native.vk.credentials_ready native.vk.credentials_failed native.vk.turn_resolve native.vk.turn_connected native.vk.turn_allocated native.vk.dtls_begin native.vk.dtls_ready native.vk.dtls_failed native.vk.smux_ready native.vk.transport native.vk.smux_closed native.vk.retry native.vk.captcha_required native.vk.captcha_submitted").split(" ")));
    static final Set<String> FIELDS=new HashSet<>(Arrays.asList(("mode state phase reason stage errorCode errorDomain elapsedMs uptimeMs residentBytes footprintBytes availableBytes heapBytes goBytes heapObjects goroutines gcCycles txBytes rxBytes inputPackets outputPackets inputErrors outputErrors queueDelayMs interfaceIndex pathStatus interfaceType expensive constrained ipv4 ipv6 protectFailures sessions retries stream httpStatus count streams gcPercent memoryLimitBytes rttMs rtoMs").split(" ")));
    static JSONObject numeric(JSONObject input)throws Exception{JSONObject out=new JSONObject();Iterator<String> keys=input.keys();while(keys.hasNext()){String key=keys.next();Object value=input.get(key);if(FIELDS.contains(key)&&value instanceof Number){double n=((Number)value).doubleValue();if(Double.isFinite(n)&&Math.rint(n)==n&&Math.abs(n)<=9007199254740991d)out.put(key,((Number)value).longValue());}}return out;}
    static JSONObject destination(String raw)throws Exception{
        URI u=new URI(raw);if(raw.length()>4096||!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||u.getPort()>65535)throw new Exception();
        String[] markers={"/api/vokop/v1/diagnostics/enroll/","/api/vokop/v1/subscriptions/","/sub/"};String[] kinds={"grant","vokop","standard"};
        for(int i=0;i<markers.length;i++){int n=u.getRawPath().lastIndexOf(markers[i]);if(n<0)continue;String token=u.getRawPath().substring(n+markers[i].length());if(!token.matches("[A-Za-z0-9_-]{16,128}"))throw new Exception();String path=u.getRawPath().substring(0,n)+"/api/vokop/v1/diagnostics/sessions";return new JSONObject().put("endpoint",new URI("https",null,u.getHost(),u.getPort(),path,null,null).toString()).put("kind",kinds[i]).put("token",token);}
        throw new Exception();
    }
}

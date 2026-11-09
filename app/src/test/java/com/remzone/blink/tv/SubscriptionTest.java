package com.remzone.blink.tv;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class SubscriptionTest {
    private String feed()throws Exception{return new JSONObject().put("schema","vokop.subscription.v1").put("revision",4).put("issued_at",1000).put("expires_at",5000).put("profiles",new org.json.JSONArray().put(new JSONObject().put("id","tv").put("name","TV").put("vless",new JSONObject().put("uuid","00000000-0000-4000-8000-000000000001")).put("turn",new JSONObject().put("peer_host","vpn.example.org").put("peer_port",56000).put("vk_call_url","https://vk.ru/call/join/demo").put("streams",1)))).toString();}
    private void reject(String raw,long now,long minimum){try{Subscription.parse(raw,now,minimum);fail("accepted invalid feed");}catch(Exception expected){}}
    @Test public void acceptsPanelFieldsAndBothVKDomains()throws Exception{for(String domain:new String[]{"vk.ru","www.vk.ru","vk.com","www.vk.com"}){Subscription s=Subscription.parse(feed().replace("vk.ru",domain),2000,4);assertEquals(4,s.revision);assertEquals(56000,s.profiles.get(0).port);assertEquals("direct",new JSONObject(s.profiles.get(0).nativeJSON("direct","1.1.1.1",s.expiresAt)).getString("mode"));assertEquals("https://"+domain+"/call/join/demo",new JSONObject(s.profiles.get(0).nativeJSON("vk","1.1.1.1",s.expiresAt)).getString("vkCallURL"));}}
    @Test public void preservesExplicitTURNTransport()throws Exception{
        Subscription s=Subscription.parse(feed(),2000,4);Subscription.Profile p=s.profiles.get(0);
        assertEquals("auto",new JSONObject(p.nativeJSON("vk","1.1.1.1",s.expiresAt)).getString("turnTransport"));
        assertEquals("udp",new JSONObject(p.nativeJSON("vk","1.1.1.1",s.expiresAt,"udp")).getString("turnTransport"));
        try{p.nativeJSON("vk","1.1.1.1",s.expiresAt,"automatic");fail("unknown transport accepted");}catch(Exception expected){}
    }
    @Test public void refusesExpiredOrReplayedFeed()throws Exception{reject(feed(),5000,0);reject(feed(),2000,5);reject(feed().replace("5000","253402300800"),2000,0);reject(feed().replace("1000","3001"),2000,0);}
    @Test public void acceptsLongSubscriptionsAndV3Routing()throws Exception{
        assertEquals(9999999,Subscription.parse(feed().replace("5000","9999999"),2000,0).expiresAt);
        JSONObject doc=new JSONObject(feed()).put("schema","vokop.subscription.v3");reject(doc.toString(),2000,0);
        doc.put("routing",new JSONObject().put("schema","blink.routing.v1").put("mode","selected").put("revision",1).put("domains",new org.json.JSONArray().put("example.org")).put("ips",new org.json.JSONArray()));
        assertNotNull(Subscription.parse(doc.toString(),2000,0).routing);
    }
    @Test public void rejectsFractionalAndStringNumbers()throws Exception{reject(feed().replace("56000","56000.5"),2000,0);reject(feed().replace("56000","\"56000\""),2000,0);}
    @Test public void rejectsHostTricksAndPrivatePeers()throws Exception{for(String call:new String[]{"http://vk.ru/call/join/demo","https://vk.ru.evil.example/call/join/demo","https://user@vk.ru/call/join/demo","https://vk.ru:443/call/join/demo","https://vk.ru/call/join/demo?x=1"})reject(feed().replace("https://vk.ru/call/join/demo",call),2000,0);for(String host:new String[]{"127.0.0.1","192.168.1.50","10.0.0.1","localhost","fake.localhost"})reject(feed().replace("vpn.example.org",host),2000,0);}
    @Test public void rejectsDuplicateAndEscapedDuplicateKeys()throws Exception{reject(feed().replace("\"revision\":4","\"revision\":4,\"revision\":5"),2000,0);reject(feed().replace("\"revision\":4","\"revision\":4,\"rev\\u0069sion\":5"),2000,0);}
    @Test public void rejectsLenientJsonExtensions()throws Exception{for(String input:new String[]{"{'a':1}","{\"a\":1,}","{\"a\":NaN}","{\"a\":/* comment */1}","{\"a\":1}{}","{\"a\":\"x\n\"}"}){try{JsonGuard.validate(input);fail(input);}catch(Exception expected){}}}
    @Test public void onlyDedicatedHttpsFeedUrls()throws Exception{String valid="https://panel.example.org/vokop-panel/api/vokop/v1/subscriptions/"+"x".repeat(43);assertEquals(valid,Subscription.validateURL(valid));for(String input:new String[]{valid.replace("https:","http:"),valid+"?key=x",valid+"#x",valid.replace("panel.example.org","user:pass@panel.example.org"),"https://panel.example.org/vokop-panel/panel/api/vokop/overview"}){try{Subscription.validateURL(input);fail(input);}catch(Exception expected){}}}
 @Test public void wireGuardRequiresV2AndMatchingProtocol()throws Exception{
 JSONObject doc=new JSONObject(feed()),p=doc.getJSONArray("profiles").getJSONObject(0),turn=p.getJSONObject("turn");p.put("backend","wireguard").put("wireguard","[Interface]\nPrivateKey = test\n");turn.put("protocol","srtp");reject(doc.toString(),2000,4);
 doc.put("schema","vokop.subscription.v2");Subscription parsed=Subscription.parse(doc.toString(),2000,4);assertEquals("wireguard",parsed.profiles.get(0).backend);assertEquals("srtp",new JSONObject(parsed.profiles.get(0).nativeJSON("vk","1.1.1.1",5000)).getString("protocol"));
 turn.put("protocol","kcp");reject(doc.toString(),2000,4);turn.put("protocol","wrap");reject(doc.toString(),2000,4);turn.put("wrap_key","1".repeat(64));Subscription.parse(doc.toString(),2000,4);
 p.put("backend","vless");reject(doc.toString(),2000,4);
 }
}

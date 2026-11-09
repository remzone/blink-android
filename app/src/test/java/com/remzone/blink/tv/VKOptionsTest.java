package com.remzone.blink.tv;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONObject;
public class VKOptionsTest {
 @Test public void defaultsAndNativePayload()throws Exception{VKOptions o=VKOptions.from(new JSONObject());assertEquals(1,o.connections);assertTrue(o.valid());o.preset="balanced";o.pace=128;o.relay="relay.example.org:3478";Subscription.Profile p=new Subscription.Profile("one","Test","00000000-0000-4000-8000-000000000001","vpn.example.org",56000,"https://vk.ru/call/join/test",3);JSONObject raw=new JSONObject(o.nativeJSON(p,"vk",1799999999));assertEquals(1,raw.getInt("streams"));assertEquals("relay.example.org:3478",raw.getString("turnServer"));assertEquals(128,raw.getJSONObject("tuning").getInt("paceKiB"));assertEquals(0,raw.getJSONObject("tuning").getInt("window"));o.connections=0;assertEquals(3,new JSONObject(o.nativeJSON(p,"vk",1799999999)).getInt("streams"));o.preset="custom";assertEquals(64,new JSONObject(o.nativeJSON(p,"vk",1799999999)).getJSONObject("tuning").getInt("window"));}
 @Test public void invalidInputs(){for(String relay:new String[]{"https://relay:443","user:secret@relay:443","relay:0","relay:65536","relay:443/path","relay"})assertFalse(VKOptions.relay(relay));assertTrue(VKOptions.relay("[2001:db8::1]:3478"));assertFalse(VKOptions.ip("dns.example.org"));VKOptions o=new VKOptions();o.mtu=1500;assertFalse(o.valid());}
}

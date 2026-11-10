package com.remzone.blink.tv;
import org.json.*;import org.junit.Test;import static org.junit.Assert.*;import java.util.ArrayList;
public class ManagedSubscriptionTest {
 @Test public void validatesPublicBotIdentity(){assertEquals("https://t.me/provider_bot",ManagedSubscription.botURL("https://t.me/provider_bot"));for(String bad:new String[]{"https://evil.example/provider_bot","https://user@t.me/provider_bot","https://t.me/provider_bot?secret=1","http://t.me/provider_bot","https://t.me/+invite"})assertEquals("",ManagedSubscription.botURL(bad));}
 @Test public void unbindingManagedVPNKeepsManualProfileAndSelection()throws Exception{
  JSONObject manual=new JSONObject().put("schema","blink.profile.v1").put("source","manual");JSONObject panel=new JSONObject().put("schema","blink.profile.v1").put("source","panel").put("managedByPanel","url");JSONObject saved=new JSONObject().put("vpnProfiles",new JSONArray().put(panel).put(manual)).put("vpnSelected",1);
  Subscription s=new Subscription(1,1,9000000000L,new ArrayList<>());s.vpnProfiles=new JSONArray();ManagedSubscription.merge(saved,s,"url");assertEquals(1,saved.getJSONArray("vpnProfiles").length());assertEquals("manual",ProfilesStore.selected(saved).getString("source"));
 }
}

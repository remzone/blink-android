package com.remzone.blink.tv;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
public class SubscriptionMetadataTest {
 @Test public void headersAndBody()throws Exception{Map<String,String> h=new HashMap<>();h.put("Profile-Title","base64:"+Base64.getEncoder().encodeToString("⚡ Provider".getBytes(StandardCharsets.UTF_8)));h.put("subscription-userinfo","upload=1024;download=2048;total=4096;expire=1794222000");JSONObject m=SubscriptionMetadata.parse(h,"#profile-title: Other\n#announce: Ready\nvless://fixture");assertEquals("⚡ Provider",m.getString("title"));assertEquals("Ready",m.getString("announcement"));assertEquals(4096,m.getLong("total"));String b=Base64.getEncoder().encodeToString("#profile-title: 🇩🇪 Test\nvless://fixture".getBytes(StandardCharsets.UTF_8));assertEquals("🇩🇪 Test",SubscriptionMetadata.parse(Collections.emptyMap(),b).getString("title"));}
 @Test public void emojiAndNumbers()throws Exception{assertEquals("🇱🇻",SubscriptionMetadata.emoji("🇱🇻 Test"));assertFalse(SubscriptionMetadata.country("🇱🇻 Test").isEmpty());assertEquals("👩‍💻",SubscriptionMetadata.emoji("👩‍💻 Work"));assertEquals("",SubscriptionMetadata.emoji("1. Server"));Map<String,String> h=new HashMap<>();h.put("subscription-userinfo","upload=-1;total=999999999999999999999;expire=0");JSONObject m=SubscriptionMetadata.parse(h,"");assertFalse(m.has("upload"));assertFalse(m.has("total"));assertFalse(m.has("expire"));}
}

package com.remzone.blink.tv;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONObject;
public class DiagnosticProtocolTest {
 @Test public void destinationScopes()throws Exception{String token="fixture_token_012345678901234567890";JSONObject d=DiagnosticProtocol.destination("https://panel.example/base/api/vokop/v1/diagnostics/enroll/"+token);assertEquals("grant",d.getString("kind"));assertEquals("https://panel.example/base/api/vokop/v1/diagnostics/sessions",d.getString("endpoint"));assertFalse(d.getString("endpoint").contains(token));for(String bad:new String[]{"http://panel.example/sub/"+token,"https://user:secret@panel.example/sub/"+token,"https://panel.example/sub/a","https://panel.example/sub/"+token+"?secret=1"})try{DiagnosticProtocol.destination(bad);fail("unsafe destination accepted");}catch(Exception expected){}}
 @Test public void numericEvidenceOnly()throws Exception{JSONObject n=DiagnosticProtocol.numeric(new JSONObject().put("txBytes",100).put("count",1.5).put("errorCode",-1).put("url","secret").put("token",123));assertEquals(2,n.length());assertEquals(100,n.getLong("txBytes"));assertFalse(n.has("url"));assertFalse(n.has("token"));}
}

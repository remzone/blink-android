package com.remzone.blink.tv;
import org.junit.Test;
import static org.junit.Assert.*;
import java.net.URI;
import java.net.URL;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;

public class SubscriptionImportTest {
    private static final String FEED="https://panel.example/sub/fixture?token=a%2Bb%26c";
    private static final String NODE="vless://00000000-0000-4000-8000-000000000001@vpn.example:443?security=tls#DE";
    @Test public void wrappersPreserveSubscriptionTokensAndConfigCredentials()throws Exception{
        for(String input:new String[]{FEED," \n"+FEED+"\n", "<"+FEED+">",FEED+"#Подписка для телефона","[Подписка]("+FEED+")","hiddify://import/"+FEED+"#Display",FEED.replace("https:","http:"),"clash://install-config?url=https%3A%2F%2Fpanel.example%2Fsub%2Ffixture%3Ftoken%3Da%252Bb%2526c"})
            assertEquals(FEED,ProfileImportInput.parse(input).subscriptionURL);
        for(String input:new String[]{NODE,NODE+"\n"+NODE,"[Interface]\nPrivateKey = fixture=","{\"outbounds\":[]}"}){
            assertNull(ProfileImportInput.parse(input).subscriptionURL);assertEquals(input,ProfileImportInput.parse(input).source);
        }
        assertEquals(NODE,ProfileImportInput.parse("hiddify://import/"+NODE).source);
        for(String bad:new String[]{"","clash://install-config?url=a&url=b","hiddify://import/hiddify://import/hiddify://import/hiddify://import/"+FEED,"https://u:password@panel.example/sub"}){
            try{ProfileImportInput.parse(bad);fail("Invalid input accepted");}catch(SubscriptionHTTP.Failure expected){}
        }
    }
    static class Reply extends HttpsURLConnection {
        final int status;final String location,body;boolean disconnected;
        Reply(URL url,int status,String location,String body){super(url);this.status=status;this.location=location;this.body=body;}
        public int getResponseCode(){return status;}
        public String getHeaderField(String key){return "Location".equals(key)?location:null;}
        public long getContentLengthLong(){return body.length();}
        public InputStream getInputStream(){return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));}
        public void disconnect(){disconnected=true;}
        public boolean usingProxy(){return false;}
        public void connect(){}
        public String getCipherSuite(){return "fixture";}
        public Certificate[] getLocalCertificates(){return null;}
        public Certificate[] getServerCertificates(){return null;}
    }
    @Test public void followsProviderSlashRedirectAndNegotiatesActualFeed()throws Exception{
        List<Reply> replies=new ArrayList<>();
        String result=SubscriptionHTTP.fetchHTTPS(FEED,"*/*",url->{Reply r=new Reply(url,replies.isEmpty()?301:200,"/sub/fixture/?token=a%2Bb%26c",NODE);replies.add(r);return r;});
        assertEquals(NODE,result);assertEquals(2,replies.size());
        for(Reply r:replies){assertEquals("*/*",r.getRequestProperty("Accept"));assertFalse(r.getInstanceFollowRedirects());assertTrue(r.disconnected);}
        assertEquals("a%2Bb%26c",replies.get(1).getURL().getQuery().substring(6));
    }
    @Test public void redirectsCannotLeakSubscriptionToAnotherOrigin()throws Exception{
        URI original=new URI(FEED);
        assertTrue(SubscriptionHTTP.allowsRedirect(original,new URI("https://PANEL.example:443/next"),5));
        for(String uri:new String[]{"http://panel.example/next","https://other.example/next","https://panel.example:8443/next","https://user:pass@panel.example/next","https://panel.example/next#label"})
            assertFalse(SubscriptionHTTP.allowsRedirect(original,new URI(uri),1));
        assertFalse(SubscriptionHTTP.allowsRedirect(original,original,6));
        final int[] calls={0};
        try{SubscriptionHTTP.fetchHTTPS(FEED,"*/*",url->{calls[0]++;return new Reply(url,301,FEED,"");});fail("Redirect loop accepted");}catch(SubscriptionHTTP.Failure expected){assertEquals(6,calls[0]);}
    }
    @Test public void revokedFeedProducesActionableErrorWithoutSavingFakeNode()throws Exception{
        String notice=NODE.replace("vpn.example:443","127.0.0.1:1").replace("security=tls","security=none&x-durev-reissue=true");
        for(String source:new String[]{notice,notice.replace("#DE","#🔴 Ключ перевыпущен — возьмите новую ссылку в боте"),Base64.getEncoder().encodeToString(notice.getBytes(StandardCharsets.UTF_8)),Base64.getUrlEncoder().withoutPadding().encodeToString(notice.getBytes(StandardCharsets.UTF_8))}){
            try{SubscriptionFeedNotice.validate(source);fail("Reissue notice accepted");}catch(SubscriptionHTTP.Failure expected){assertTrue(expected.getMessage().contains("перевыпущен"));}
        }
        SubscriptionFeedNotice.validate(NODE);SubscriptionFeedNotice.validate(notice+"\n"+NODE);
    }
    @Test public void decompressesGzipBeforeParsingAndEnforcesDecodedLimit()throws Exception{
        java.io.ByteArrayOutputStream compressed=new java.io.ByteArrayOutputStream();
        try(java.util.zip.GZIPOutputStream zip=new java.util.zip.GZIPOutputStream(compressed)){zip.write(NODE.getBytes(StandardCharsets.UTF_8));}
        assertEquals(NODE,SubscriptionHTTP.fetchHTTPS(FEED,"*/*",url->new Reply(url,200,null,""){
            public String getContentEncoding(){return "gzip";}
            public InputStream getInputStream(){return new ByteArrayInputStream(compressed.toByteArray());}
        }));
        java.io.ByteArrayOutputStream huge=new java.io.ByteArrayOutputStream();
        try(java.util.zip.GZIPOutputStream zip=new java.util.zip.GZIPOutputStream(huge)){zip.write(new byte[1048577]);}
        try{SubscriptionHTTP.fetchHTTPS(FEED,"*/*",url->new Reply(url,200,null,""){
            public String getContentEncoding(){return "gzip";}
            public InputStream getInputStream(){return new ByteArrayInputStream(huge.toByteArray());}
        });fail("Decoded body limit ignored");}catch(SubscriptionHTTP.Failure expected){}
    }

}

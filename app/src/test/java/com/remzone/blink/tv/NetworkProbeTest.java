package com.remzone.blink.tv;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;
public class NetworkProbeTest {
 private String read(String raw)throws Exception{return NetworkProbe.readResponse(new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));}
 @Test public void readsLengthAndChunkedResponses()throws Exception{assertEquals("ip=1.2.3.4\n",read("HTTP/1.1 200 OK\r\nContent-Length: 11\r\n\r\nip=1.2.3.4\n"));assertEquals("ip=1.2.3.4\n",read("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n3\r\nip=\r\n8\r\n1.2.3.4\n\r\n0\r\n\r\n"));}
 @Test public void rejectsErrorTruncationOversizeAndBadChunks()throws Exception{for(String raw:new String[]{"HTTP/1.1 403 Forbidden\r\n\r\n","HTTP/1.1 200 OK\r\nContent-Length: 99999\r\n\r\n","HTTP/1.1 200 OK\r\nContent-Length: 3\r\n\r\nx","HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n-1\r\n","HTTP/1.1 200 OK\r\nTransfer-Encoding: gzip\r\n\r\n"})try{read(raw);fail("malformed response accepted");}catch(IOException expected){}}
}

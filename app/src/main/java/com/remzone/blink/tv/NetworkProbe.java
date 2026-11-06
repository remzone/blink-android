package com.remzone.blink.tv;

import android.net.Network;
import android.os.SystemClock;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.net.ssl.*;

/** Small verified HTTPS request, bound to the current VPN; no bandwidth estimate. */
final class NetworkProbe {
    static final class Result {
        long dnsMs,tcpMs,tlsMs,responseMs,totalMs;String ip="";
        String summary(){return String.format(Locale.ROOT,"DNS %d мс · TCP IPv4 %d мс · TLS %d мс · ответ %d мс",dnsMs,tcpMs,tlsMs,responseMs);}
    }
    static final class Failure extends IOException {
        final String phase;final Result partial;
        Failure(String phase,Result partial){super("Probe phase failed");this.phase=phase;this.partial=partial;}
        String summary(){return "Не завершён этап: "+phase+". "+partial.summary();}
    }
    static Result measure(Network network)throws Failure{return measure(network,"www.cloudflare.com","/cdn-cgi/trace",true,false);}
    static Result measure(Network network,String host,String path,boolean wantIP,boolean physical)throws Failure{
        Result r=new Result();String phase="DNS";long started=SystemClock.elapsedRealtime(),mark=started;Socket plain=null;SSLSocket tls=null;
        try{
            if(network==null)throw new IOException();mark=SystemClock.elapsedRealtime();InetAddress chosen=null;
            for(InetAddress address:network.getAllByName(host))if(address instanceof Inet4Address){chosen=address;break;}
            r.dnsMs=SystemClock.elapsedRealtime()-mark;if(chosen==null)throw new IOException();
            phase="TCP IPv4";mark=SystemClock.elapsedRealtime();plain=network.getSocketFactory().createSocket();if(physical&&!TunnelService.protectDiagnostic(plain))throw new IOException();plain.connect(new InetSocketAddress(chosen,443),12000);plain.setSoTimeout(12000);r.tcpMs=SystemClock.elapsedRealtime()-mark;
            phase="TLS";mark=SystemClock.elapsedRealtime();tls=(SSLSocket)((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(plain,host,443,true);SSLParameters parameters=tls.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");tls.setSSLParameters(parameters);tls.setSoTimeout(12000);tls.startHandshake();r.tlsMs=SystemClock.elapsedRealtime()-mark;
            phase="Ответ HTTPS";mark=SystemClock.elapsedRealtime();OutputStream out=tls.getOutputStream();out.write(("GET "+path+" HTTP/1.1\r\nHost: "+host+"\r\nAccept-Encoding: identity\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.flush();
            PushbackInputStream in=new PushbackInputStream(new BufferedInputStream(tls.getInputStream()));int first=in.read();if(first<0)throw new EOFException();r.responseMs=SystemClock.elapsedRealtime()-mark;in.unread(first);
            String body=readResponse(in,wantIP);for(String line:body.split("\n"))if(line.startsWith("ip="))r.ip=line.substring(3).trim();if(wantIP&&!r.ip.matches("[0-9a-fA-F:.]{3,64}"))throw new IOException();r.totalMs=SystemClock.elapsedRealtime()-started;return r;
        }catch(Exception ex){long elapsed=SystemClock.elapsedRealtime()-mark;r.totalMs=SystemClock.elapsedRealtime()-started;if("DNS".equals(phase))r.dnsMs=elapsed;else if("TCP IPv4".equals(phase))r.tcpMs=elapsed;else if("TLS".equals(phase))r.tlsMs=elapsed;else r.responseMs=elapsed;throw new Failure(phase,r);}finally{try{if(tls!=null)tls.close();else if(plain!=null)plain.close();}catch(IOException ignored){}}
    }
    static String readResponse(InputStream in)throws IOException{return readResponse(in,true);}
    static String readResponse(InputStream in,boolean wantIP)throws IOException{
        String status=line(in);boolean noBody=status.matches("HTTP/1\\.[01] 204(?: .*)?");if(!status.matches(wantIP?"HTTP/1\\.[01] 200(?: .*)?":"HTTP/1\\.[01] (?:200|204)(?: .*)?"))throw new IOException("HTTP status");
        long length=-1;boolean chunked=false;int headers=0;
        for(String h;(h=line(in)).length()>0;){if(++headers>64)throw new IOException("Header limit");int colon=h.indexOf(':');if(colon<1)throw new IOException("Header");String key=h.substring(0,colon).toLowerCase(Locale.ROOT),value=h.substring(colon+1).trim();
            if(key.equals("content-length")){try{length=Long.parseLong(value);}catch(NumberFormatException ex){throw new IOException("Length");}if(length<0||length>8192)throw new IOException("Body limit");}
            if(key.equals("transfer-encoding")){if(!value.equalsIgnoreCase("chunked"))throw new IOException("Encoding");chunked=true;}
            if(key.equals("content-encoding")&&!value.equalsIgnoreCase("identity"))throw new IOException("Encoding");
        }
        if(noBody)return "";
        ByteArrayOutputStream body=new ByteArrayOutputStream();
        if(chunked){while(true){String size=line(in).split(";",2)[0];int n;try{n=Integer.parseInt(size,16);}catch(NumberFormatException ex){throw new IOException("Chunk");}if(n<0||body.size()+n>8192)throw new IOException("Body limit");if(n==0){int trailers=0;while(!line(in).isEmpty())if(++trailers>32)throw new IOException("Trailer limit");break;}copyExactly(in,body,n);if(!line(in).isEmpty())throw new IOException("Chunk boundary");}}
        else if(length>=0)copyExactly(in,body,(int)length);
        else{byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1){if(body.size()+n>8192)throw new IOException("Body limit");body.write(b,0,n);}}
        return new String(body.toByteArray(),StandardCharsets.UTF_8);
    }
    private static void copyExactly(InputStream in,OutputStream out,int count)throws IOException{byte[] b=new byte[1024];while(count>0){int n=in.read(b,0,Math.min(b.length,count));if(n<0)throw new EOFException();out.write(b,0,n);count-=n;}}
    private static String line(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();while(true){int c=in.read();if(c<0)throw new EOFException();if(c=='\n'){byte[] b=out.toByteArray();if(b.length==0||b[b.length-1]!='\r')throw new IOException("Line ending");return new String(b,0,b.length-1,StandardCharsets.US_ASCII);}if(out.size()>=4096)throw new IOException("Line limit");out.write(c);}}
}

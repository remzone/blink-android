package com.remzone.blink.tv;

import android.content.Context;
import android.net.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import javax.net.ssl.HttpsURLConnection;

/** No scheduler, automatic retries or redirect of credentials. Only LogsActivity calls send. */
final class DiagnosticUpload {
    static final class Response{final int status;final JSONObject body;Response(int s,JSONObject b){status=s;body=b;}}
    private static Response request(Context context,String endpoint,String token,JSONObject value)throws Exception{
        List<Network> paths=new ArrayList<>();paths.add(null);ConnectivityManager cm=context.getSystemService(ConnectivityManager.class);
        for(Network n:cm.getAllNetworks()){NetworkCapabilities caps=cm.getNetworkCapabilities(n);if(caps!=null&&!caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))paths.add(n);}
        URI uri=new URI(endpoint);if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||token.contains("\r")||token.contains("\n"))throw new Exception();long requestDeadline=System.nanoTime()+8_000_000_000L;byte[] payload=value.toString().getBytes(StandardCharsets.UTF_8);if(payload.length>60000)throw new Exception();
        for(Network path:paths){if(Thread.currentThread().isInterrupted())throw new InterruptedException();HttpsURLConnection c=null;try{c=(HttpsURLConnection)(path==null?uri.toURL().openConnection():path.openConnection(uri.toURL()));c.setInstanceFollowRedirects(false);c.setConnectTimeout(4000);c.setReadTimeout(4000);c.setUseCaches(false);c.setRequestMethod("POST");c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Content-Type","application/json");c.setDoOutput(true);c.setFixedLengthStreamingMode(payload.length);try(OutputStream out=c.getOutputStream()){out.write(payload);}int code=c.getResponseCode();if(code!=200)return new Response(code,new JSONObject());ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){if(System.nanoTime()>requestDeadline||out.size()+n>65536)throw new IOException();out.write(b,0,n);}}return new Response(code,new JSONObject(out.toString("UTF-8")));}catch(IOException ignored){}finally{if(c!=null)c.disconnect();}}
        throw new IOException();
    }
    static String send(Context context)throws Exception{
        JSONArray snapshot=DiagnosticJournal.snapshot();int sent=0,code=0;boolean missing=false;long deadline=System.nanoTime()+90_000_000_000L;
        for(int i=0;i<snapshot.length()&&System.nanoTime()<deadline;i++){
            JSONObject session=snapshot.getJSONObject(i);String id=session.getString("id");JSONObject credentials=DiagnosticJournal.credential(id);if(credentials==null){missing=true;continue;}
            String endpoint=credentials.getString("endpoint");String upload=credentials.optString("uploadToken");
            if(upload.isEmpty()){Response enrolled=request(context,endpoint,credentials.getString("token"),new JSONObject().put("sessionId",id).put("kind",credentials.getString("kind")).put("platform","android").put("appVersion",session.getString("version")).put("build",session.getString("build")));code=enrolled.status;if(code!=200)continue;if(!id.equals(enrolled.body.getString("sessionId")))throw new IOException();upload=enrolled.body.getString("uploadToken");credentials=new JSONObject(credentials.toString()).put("uploadToken",upload);DiagnosticJournal.credential(id,credentials);}
            JSONArray events=session.getJSONArray("events");int offset=0;while(offset<events.length()&&System.nanoTime()<deadline){JSONArray batch=new JSONArray();int size=0;while(offset+size<events.length()&&size<64){batch.put(events.getJSONObject(offset+size));if(new JSONObject().put("events",batch).toString().getBytes(StandardCharsets.UTF_8).length>58000){batch.remove(batch.length()-1);break;}size++;}if(size==0)throw new IOException();Response r=request(context,endpoint+"/"+id+"/events",upload,new JSONObject().put("events",batch));code=r.status;if(code!=200)break;Set<String> accepted=new HashSet<>();JSONArray ack=r.body.getJSONArray("accepted");for(int a=0;a<ack.length();a++)accepted.add(ack.getString(a));Set<String> selected=new HashSet<>();for(int a=0;a<batch.length();a++)if(accepted.contains(batch.getJSONObject(a).getString("id")))selected.add(batch.getJSONObject(a).getString("id"));DiagnosticJournal.ack(selected);sent+=selected.size();if(selected.size()!=size)throw new IOException();offset+=size;}
        }
        return "Отправлено: "+sent+". Осталось: "+DiagnosticJournal.pending()+(code==403?". Панель отклонила приём; проверьте разрешения диагностики.":code==429?". Достигнут серверный лимит.":code!=0&&code!=200?". HTTP "+code:".")+(missing?" У части записей нет сервера: добавьте ссылку диагностики из панели.":"");
    }
}

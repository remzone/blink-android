package com.remzone.blink.tv;
import org.json.*;
import mobile.Mobile;
final class ManagedSubscription {
 static void merge(JSONObject saved,Subscription sub,String url)throws Exception{
  if(sub.vpnProfiles==null)return;
  JSONArray old=saved.optJSONArray("vpnProfiles"),next=new JSONArray();String selected="";
  try{selected=ProfilesStore.selected(saved).getString("source");}catch(Exception ignored){}
  if(old!=null)for(int i=0;i<old.length();i++)if(!old.getJSONObject(i).has("managedByPanel"))next.put(old.getJSONObject(i));
  int rejected=0;
  JSONObject meta=sub.metadata==null?new JSONObject():new JSONObject(sub.metadata.toString());meta.put("expire",sub.expiresAt).put("updated_at",System.currentTimeMillis());
  for(int i=0;i<sub.vpnProfiles.length();i++){
   try{JSONObject imported=new JSONObject(Mobile.importProfiles(sub.vpnProfiles.getString(i)));JSONArray nodes=imported.getJSONArray("profiles");rejected+=imported.getJSONArray("rejected").length();
    for(int k=0;k<nodes.length();k++)next.put(nodes.getJSONObject(k).put("managedByPanel",url).put("subscriptionURL",url).put("subscriptionMetadata",meta));
   }catch(Exception ignored){rejected++;}
  }
  if(next.length()>1024)throw new Exception("Слишком много VPN-профилей");
  int index=0;for(int i=0;i<next.length();i++)if(selected.equals(next.getJSONObject(i).optString("source"))){index=i;break;}
  saved.put("vpnProfiles",next).put("vpnSelected",index).put("vpnImportRejected",rejected);
 }
 static void remove(JSONObject saved)throws Exception{JSONArray old=saved.optJSONArray("vpnProfiles"),next=new JSONArray();String selected="";try{selected=ProfilesStore.selected(saved).getString("source");}catch(Exception ignored){}if(old!=null)for(int i=0;i<old.length();i++)if(!old.getJSONObject(i).has("managedByPanel"))next.put(old.getJSONObject(i));int index=0;for(int i=0;i<next.length();i++)if(selected.equals(next.getJSONObject(i).optString("source")))index=i;saved.put("vpnProfiles",next).put("vpnSelected",index);saved.remove("routingServer");saved.remove("routingServerURL");saved.remove("vokopUpdatedAt");}
 static String botURL(String raw){try{java.net.URI u=new java.net.URI(raw);if("https".equals(u.getScheme())&&"t.me".equals(u.getHost())&&u.getPort()==-1&&u.getUserInfo()==null&&u.getRawQuery()==null&&u.getFragment()==null&&u.getPath().matches("/[A-Za-z][A-Za-z0-9_]{4,31}"))return raw;}catch(Exception ignored){}return "";}
}

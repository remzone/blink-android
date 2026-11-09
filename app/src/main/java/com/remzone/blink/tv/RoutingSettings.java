package com.remzone.blink.tv;
import org.json.JSONObject;
import mobile.Mobile;
final class RoutingSettings {
 static JSONObject local(JSONObject saved){JSONObject v=saved.optJSONObject("routingLocal");return v==null?new JSONObject():v;}
 static JSONObject server(JSONObject saved){if(saved.optString("routingServerURL").equals(saved.optString("url"))&&saved.optJSONObject("routingServer")!=null)return saved.optJSONObject("routingServer");try{return new JSONObject(saved.getString("document")).optJSONObject("routing");}catch(Exception ignored){return null;}}
 static JSONObject effective(JSONObject saved)throws Exception{JSONObject server=server(saved);return new JSONObject(Mobile.mergeRoutingPolicy(server==null?"":server.toString(),local(saved).toString()));}
 static String summary(JSONObject saved){String mode=local(saved).optString("mode","inherit");JSONObject p=server(saved);if("inherit".equals(mode))mode=p==null?"all":p.optString("mode","all");return "selected".equals(mode)?"Выборочно · остальные сайты напрямую":"Весь трафик через VPN";}
}

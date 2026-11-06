package com.remzone.blink.tv;
import org.json.*;
final class ProfilesStore {
 static JSONObject selected(JSONObject saved)throws Exception{JSONArray a=saved.getJSONArray("vpnProfiles");int i=saved.optInt("vpnSelected",0);if(i<0||i>=a.length())throw new Exception("Выберите VPN-профиль");JSONObject p=a.getJSONObject(i);if(!"blink.profile.v1".equals(p.optString("schema")))throw new Exception("Импортируйте VPN-профиль заново");return p;}
 static boolean valid(JSONObject saved){try{selected(saved);return true;}catch(Exception e){return false;}}
}

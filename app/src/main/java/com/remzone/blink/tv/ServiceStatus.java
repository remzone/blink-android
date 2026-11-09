package com.remzone.blink.tv;
final class ServiceStatus {
 static volatile boolean maintenance;static volatile String message="";
 static org.json.JSONObject parse(String raw)throws Exception{org.json.JSONObject v=new org.json.JSONObject(raw);if(!(v.opt("maintenance") instanceof Boolean)||!(v.opt("revision") instanceof Number)||v.optLong("revision")<0||!(v.opt("message") instanceof String)||v.getString("message").length()>4000)throw new Exception("invalid status");return v;}
 static String statusURL(String subscription)throws Exception{Subscription.validateURL(subscription);int i=subscription.lastIndexOf("/api/vokop/v1/subscriptions/");return subscription.substring(0,i)+"/api/vokop/v1/status";}
 static final class MaintenanceException extends Exception {MaintenanceException(){super("На сервере технические работы. Попробуйте позже.");}}
}

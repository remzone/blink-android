package com.remzone.blink.tv;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.text.InputType;
import android.widget.*;
import org.json.*;

public final class RoutingActivity extends Activity {
 private SecureStore store;private JSONObject saved;private Spinner mode;private EditText include,exclude;private TextView message;
 @Override public void onCreate(Bundle state){super.onCreate(state);store=new SecureStore(this);try{saved=store.load();}catch(Exception e){finish();return;}
  LinearLayout root=BlinkStyle.page(this,"Маршрутизация","Ваши правила поверх настроек панели.");
  LinearLayout panel=BlinkStyle.card(this,root,"Политика панели");JSONObject policy=RoutingSettings.server(saved);
  panel.addView(BlinkStyle.text(this,policy==null?"Обновите подписку Vokop, чтобы получить правила сервера.":"Режим: "+policy.optString("mode")+" · доменов "+length(policy,"domains")+" · подсетей "+length(policy,"ips"),14,BlinkStyle.TEXT));
  panel.addView(BlinkStyle.text(this,"Только адреса из списка идут через VPN. Остальные — напрямую с IP вашего устройства. Исключения имеют приоритет, поддомены включаются автоматически.",14,BlinkStyle.MUTED));
  if(policy!=null)BlinkStyle.button(this,panel,"Посмотреть серверный список",()->showPolicy(policy));
  LinearLayout local=BlinkStyle.card(this,root,"На этом устройстве");mode=new Spinner(this);mode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Как в панели","Выборочно","Весь трафик через VPN"}));mode.setContentDescription("Режим маршрутизации");local.addView(mode);
  JSONObject options=RoutingSettings.local(saved);String choice=options.optString("mode","inherit");mode.setSelection("selected".equals(choice)?1:"all".equals(choice)?2:0);
  local.addView(BlinkStyle.text(this,"Дополнительно через VPN",16,BlinkStyle.TEXT));include=editor(local,options.optString("include"),"example.org или 203.0.113.0/24");
  local.addView(BlinkStyle.text(this,"Всегда напрямую",16,BlinkStyle.TEXT));exclude=editor(local,options.optString("exclude"),"По одному домену или IP/CIDR на строке");
  BlinkStyle.button(this,local,"Сохранить правила",this::save);BlinkStyle.button(this,local,"Вернуться к настройкам панели",()->new AlertDialog.Builder(this).setTitle("Сбросить свои правила?").setMessage("Будут использоваться актуальные настройки панели.").setNegativeButton("Отмена",null).setPositiveButton("Сбросить",(d,w)->{mode.setSelection(0);include.setText("");exclude.setText("");save();}).show());
  message=BlinkStyle.text(this,StateStore.active()?"Сначала отключите VPN для изменения правил.":"Изменения применяются при следующем подключении.",13,BlinkStyle.MUTED);local.addView(message);
  local.addView(BlinkStyle.text(this,"Выборочный режим: VLESS и стандартный WireGuard, включая TURN. Для AmneziaWG и TUIC используйте весь трафик. Cloudflare содержит общие IP CDN — это может затронуть другие сайты на тех же адресах.",12,BlinkStyle.MUTED));
  BlinkStyle.button(this,root,"Назад",this::finish);
 }
 private int length(JSONObject p,String key){JSONArray a=p.optJSONArray(key);return a==null?0:a.length();}
 private EditText editor(LinearLayout parent,String value,String hint){EditText v=new EditText(this);v.setText(value);v.setHint(hint);v.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);v.setMinLines(4);v.setMaxLines(9);BlinkStyle.input(this,v);parent.addView(v);return v;}
 private void save(){if(StateStore.active()){message.setText("Сначала отключите VPN.");return;}try{JSONObject next=new JSONObject().put("mode",mode.getSelectedItemPosition()==1?"selected":mode.getSelectedItemPosition()==2?"all":"inherit").put("include",include.getText().toString()).put("exclude",exclude.getText().toString());JSONObject draft=new JSONObject(saved.toString()).put("routingLocal",next);RoutingSettings.effective(draft);saved=store.update(current->current.put("routingLocal",next));message.setText("Правила сохранены. Подключитесь заново для применения.");StateStore.notifyChanged(this);}catch(Exception e){message.setText("Проверьте список: домены или IP/CIDR, без URL и регулярных выражений. Для выборочного режима нужен адрес VPN.");}}
 private void showPolicy(JSONObject policy){ScrollView scroll=new ScrollView(this);TextView text=BlinkStyle.text(this,lines(policy,"domains")+"\n"+lines(policy,"ips"),13,BlinkStyle.TEXT);text.setTextIsSelectable(true);scroll.setBackgroundColor(BlinkStyle.CARD);scroll.setPadding(20,20,20,20);scroll.addView(text);new AlertDialog.Builder(this).setTitle("Список панели").setView(scroll).setPositiveButton("Готово",null).show();}
 private String lines(JSONObject p,String key){JSONArray a=p.optJSONArray(key);StringBuilder s=new StringBuilder();if(a!=null)for(int i=0;i<a.length();i++)s.append(a.optString(i)).append('\n');return s.toString();}
}

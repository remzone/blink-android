package com.remzone.blink.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.concurrent.*;
import mobile.Mobile;
import org.json.*;

/** Standard profiles never require VK; credentials and feed URLs stay encrypted. */
public final class ProfilesActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private LinearLayout root;private SecureStore store;private TextView note;private boolean busy;
    @Override public void onCreate(Bundle state){super.onCreate(state);store=new SecureStore(this);render();}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
    private void render(){
        ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(32,24,32,32);scroll.addView(root);
        TextView title=new TextView(this);title.setText("VPN-профили · Xray / WireGuard / AmneziaWG");title.setTextSize(23);root.addView(title);
        note=new TextView(this);note.setText("Подключение напрямую к вашему VPN-серверу. VK и CAPTCHA не нужны. Ссылки и ключи хранятся зашифрованно.");root.addView(note);
        button("Добавить ссылку, подписку или .conf",this::paste);
        button("Импортировать файл",()->{if(!canEdit())return;try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),77);}catch(android.content.ActivityNotFoundException e){note.setText("На этом ТВ нет системного выбора файла. Вставьте конфигурацию или HTTPS-ссылку.");}});
        button("Обновить подписку VPN",this::refreshFeed);
        try{JSONObject saved=store.load();JSONArray a=saved.optJSONArray("vpnProfiles");if(a!=null)for(int i=0;i<a.length();i++){final int index=i;JSONObject p=a.getJSONObject(i);button((saved.optInt("vpnSelected",0)==i&&"profile".equals(saved.optString("mode"))?"✓ ":"")+p.optString("name","VPN")+" · "+p.optString("kind"),()->choose(index));}}
        catch(Exception e){note.setText("Не удалось прочитать защищённые профили");}
        button("Удалить выбранный профиль",this::removeSelected);button("Назад к соединению",this::finish);setContentView(scroll);
    }
    private void button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setPadding(12,12,12,12);root.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(v->action.run());}
    private boolean canEdit(){if(busy){note.setText("Дождитесь завершения импорта");return false;}if(StateStore.active()){note.setText("Сначала отключите текущий туннель для изменения профилей");return false;}return true;}
    private void choose(int index){if(!canEdit())return;try{JSONObject saved=store.load();saved.put("vpnSelected",index).put("mode","profile");store.save(saved);StateStore.mode="profile";StateStore.notifyChanged(this);render();}catch(Exception e){note.setText("Не удалось сохранить профиль");}}
    private void removeSelected(){
        if(!canEdit())return;
        new AlertDialog.Builder(this).setTitle("Удалить выбранный VPN-профиль?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{
            try{JSONObject saved=store.load();JSONArray old=saved.optJSONArray("vpnProfiles"),next=new JSONArray();int selected=saved.optInt("vpnSelected",0);if(old!=null)for(int i=0;i<old.length();i++)if(i!=selected)next.put(old.get(i));saved.put("vpnProfiles",next).put("vpnSelected",0);store.save(saved);render();StateStore.notifyChanged(this);}catch(Exception e){note.setText("Не удалось удалить профиль");}
        }).show();
    }
    private void paste(){
        if(!canEdit())return;
        EditText input=new EditText(this);input.setMinLines(3);input.setMaxLines(10);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle("Ссылка или конфигурация").setMessage("Вставьте URI, HTTPS-подписку, WireGuard или AmneziaWG .conf. Неподдерживаемые параметры будут отклонены.").setView(input).setNegativeButton("Отмена",null).setPositiveButton("Импортировать",(d,w)->importText(input.getText().toString())).show();
    }
    private void refreshFeed(){
        if(!canEdit())return;
        try{JSONArray a=store.load().optJSONArray("vpnProfiles");LinkedHashSet<String> sources=new LinkedHashSet<>();if(a!=null)for(int i=0;i<a.length();i++){String url=a.getJSONObject(i).optString("subscriptionURL");if(!url.isEmpty())sources.add(url);}String[] urls=sources.toArray(new String[0]);if(urls.length==0){note.setText("Сначала добавьте HTTPS-подписку");return;}String[] names=new String[urls.length];for(int i=0;i<urls.length;i++)names[i]=(i+1)+" · "+new java.net.URI(urls[i]).getHost();new AlertDialog.Builder(this).setTitle("Обновить VPN-подписку").setItems(names,(d,w)->importText(urls[w])).setNegativeButton("Отмена",null).show();}
        catch(Exception e){note.setText("Не удалось прочитать сохранённые ссылки");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=77||result!=RESULT_OK||data==null||!canEdit())return;busy=true;
        worker.execute(()->{try{String text;try(java.io.InputStream in=getContentResolver().openInputStream(data.getData())){text=read(in);}performImport(text,null);}catch(Exception e){error();}});
    }
    private void importText(String text){
        if(!canEdit())return;busy=true;
        worker.execute(()->{try{String source=text.trim(),url=null;if(source.startsWith("https://")){url=source;source=SubscriptionHTTP.fetchStandard(url);}performImport(source,url);}catch(Exception e){error();}});
    }
    private void performImport(String text,String url)throws Exception{
        JSONObject imported=new JSONObject(Mobile.importProfiles(text));if(StateStore.active())throw new Exception();
        JSONObject saved=store.load();JSONArray old=saved.optJSONArray("vpnProfiles"),next=new JSONArray();String previous="";
        try{previous=ProfilesStore.selected(saved).getString("source");}catch(Exception ignored){}
        if(old!=null)for(int i=0;i<old.length();i++){JSONObject p=old.getJSONObject(i);if(url==null||!url.equals(p.optString("subscriptionURL")))next.put(p);}
        int first=next.length();JSONArray added=imported.getJSONArray("profiles");for(int i=0;i<added.length();i++){JSONObject p=added.getJSONObject(i);if(url!=null)p.put("subscriptionURL",url);next.put(p);}
        if(next.length()>256)throw new Exception();int selected=first;if(url!=null)for(int i=0;i<next.length();i++)if(previous.equals(next.getJSONObject(i).optString("source"))){selected=i;break;}
        saved.put("vpnProfiles",next).put("vpnSelected",selected).put("mode","profile");if(saved.toString().getBytes(StandardCharsets.UTF_8).length>524288||StateStore.active())throw new Exception();store.save(saved);
        JSONArray rejected=imported.getJSONArray("rejected");StringBuilder result=new StringBuilder("Добавлено: "+added.length()+". Отклонено: "+rejected.length()+".");if(rejected.length()>0){result.append(" Строки: ");for(int i=0;i<Math.min(16,rejected.length());i++){if(i>0)result.append(", ");result.append(rejected.getJSONObject(i).getInt("index"));}}
        main.post(()->{busy=false;if(isDestroyed())return;render();note.setText(result.toString());StateStore.notifyChanged(this);});
    }
    private void error(){main.post(()->{busy=false;if(!isDestroyed())note.setText("Импорт не выполнен. Проверьте протокол, параметры, размер и HTTPS-ссылку. Ключи не записываются в журнал.");});}
    private static String read(java.io.InputStream in)throws Exception{if(in==null)throw new Exception();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>131072)throw new Exception();out.write(b,0,n);}return out.toString("UTF-8");}
}

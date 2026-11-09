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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.concurrent.*;
import mobile.Mobile;
import org.json.*;

/** Named nodes stay separate; feed replacement is atomic and preserves manual profiles. */
public final class ProfilesActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private SecureStore store;
    private TextView note,count,emptyTitle,emptyDetail;
    private ListView list;
    private String filter="";
    private boolean busy;
    private final ArrayList<Integer> indices=new ArrayList<>();
    private final ArrayList<String> labels=new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);store=new SecureStore(this);render();
        String initial=getIntent().getStringExtra("importSource");getIntent().removeExtra("importSource");
        if(initial!=null&&initial.length()<=1048576)main.post(()->paste(initial));
    }
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
    private void render(){
        LinearLayout page=BlinkStyle.page(this,"Мир ближе, чем кажется.","Твои серверы и подписки. Найди своё подключение.");
        boolean wide=DeviceLayout.twoColumns(getResources().getConfiguration());
        LinearLayout panels=new LinearLayout(this);panels.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);page.addView(panels);
        LinearLayout profiles=BlinkStyle.card(this,panels,"Страны и серверы");
        LinearLayout tools=BlinkStyle.card(this,panels,"Подписки и импорт");
        if(wide){LinearLayout.LayoutParams left=(LinearLayout.LayoutParams)profiles.getLayoutParams();left.width=0;left.weight=1.2f;left.rightMargin=BlinkStyle.dp(this,16);profiles.setLayoutParams(left);LinearLayout.LayoutParams right=(LinearLayout.LayoutParams)tools.getLayoutParams();right.width=0;right.weight=1;tools.setLayoutParams(right);}
        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("Страна или название сервера");BlinkStyle.input(this,search);profiles.addView(search);search.setText(filter);
        list=new ListView(this);list.setDividerHeight(BlinkStyle.dp(this,8));list.setItemsCanFocus(false);list.setSelector(BlinkStyle.surface(0xff253e4a,BlinkStyle.ACCENT,16,this));
        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){@Override public android.view.View getView(int position,android.view.View recycled,android.view.ViewGroup parent){TextView row=(TextView)super.getView(position,recycled,parent);row.setTextColor(BlinkStyle.TEXT);row.setTextSize(15);row.setCompoundDrawablePadding(BlinkStyle.dp(ProfilesActivity.this,12));BlinkIcon symbol=new BlinkIcon(labels.get(position).startsWith("✓")?"shield":"globe",BlinkStyle.ACCENT);symbol.setBounds(0,0,BlinkStyle.dp(ProfilesActivity.this,23),BlinkStyle.dp(ProfilesActivity.this,23));row.setCompoundDrawablesRelative(symbol,null,null,null);row.setMinHeight(BlinkStyle.dp(ProfilesActivity.this,56));row.setPadding(BlinkStyle.dp(ProfilesActivity.this,12),BlinkStyle.dp(ProfilesActivity.this,10),BlinkStyle.dp(ProfilesActivity.this,12),BlinkStyle.dp(ProfilesActivity.this,10));return row;}};
        list.setAdapter(adapter);profiles.addView(list,new LinearLayout.LayoutParams(-1,BlinkStyle.dp(this,DeviceLayout.isTV(this)?260:300)));
        LinearLayout empty=BlinkStyle.column(this);empty.setGravity(android.view.Gravity.CENTER);empty.setPadding(BlinkStyle.dp(this,12),BlinkStyle.dp(this,24),BlinkStyle.dp(this,12),BlinkStyle.dp(this,24));android.widget.ImageView globe=new android.widget.ImageView(this);globe.setImageDrawable(new BlinkIcon("globe",BlinkStyle.ACCENT));globe.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);empty.addView(globe,new LinearLayout.LayoutParams(BlinkStyle.dp(this,48),BlinkStyle.dp(this,48)));emptyTitle=BlinkStyle.text(this,"Твой мир начинается здесь",19,BlinkStyle.TEXT);emptyTitle.setGravity(android.view.Gravity.CENTER);empty.addView(emptyTitle);emptyDetail=BlinkStyle.text(this,"Добавь ссылку или подписку — серверы появятся здесь.",13,BlinkStyle.MUTED);emptyDetail.setGravity(android.view.Gravity.CENTER);empty.addView(emptyDetail);profiles.addView(empty);list.setEmptyView(empty);
        count=BlinkStyle.text(this,"",13,BlinkStyle.MUTED);profiles.addView(count);list.setOnItemClickListener((parent,view,position,id)->choose(indices.get(position)));
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int before,int after){}public void onTextChanged(CharSequence s,int start,int before,int after){filter=s.toString();renderProfiles();}public void afterTextChanged(android.text.Editable text){}});
        note=BlinkStyle.text(this,"URI · Xray JSON · Clash · WireGuard · AmneziaWG · TUIC · Hysteria",14,BlinkStyle.MUTED);tools.addView(note);
        BlinkStyle.button(this,tools,"Добавить ссылку, подписку или .conf",()->paste(""));
        BlinkStyle.button(this,tools,"Импортировать файл",this::openFile);
        BlinkStyle.button(this,tools,"Обновить подписку VPN",this::refreshFeed);
        BlinkStyle.button(this,tools,"Удалить выбранный профиль",this::removeSelected);
        BlinkStyle.button(this,tools,"Назад к соединению",this::finish);
        renderProfiles();if(DeviceLayout.isTV(this))list.requestFocus();
    }
    private void renderProfiles(){
        labels.clear();indices.clear();
        try{JSONObject saved=store.load();JSONArray profiles=saved.optJSONArray("vpnProfiles");int total=profiles==null?0:profiles.length();for(int i=0;i<total;i++){JSONObject p=profiles.getJSONObject(i);String name=p.optString("name","VPN"),kind=p.optString("kind");if(!(name+" "+kind).toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)))continue;indices.add(i);labels.add((saved.optInt("vpnSelected",0)==i&&"profile".equals(saved.optString("mode"))?"✓ ":"")+name+"\n"+kind);}adapter.notifyDataSetChanged();emptyTitle.setText(total==0?"Твой мир начинается здесь":"Такого сервера пока нет");emptyDetail.setText(total==0?"Добавь ссылку или подписку — серверы появятся здесь.":"Попробуй другое название страны или очисти поиск.");count.setText("Найдено: "+labels.size()+" · всего: "+total);}
        catch(Exception e){if(note!=null)note.setText("Не удалось прочитать защищённые профили");}
    }
    private boolean canEdit(){if(busy){note.setText("Дождитесь завершения импорта");return false;}if(StateStore.active()){note.setText("Сначала отключите текущий туннель для изменения профилей");return false;}return true;}
    private void choose(int index){if(!canEdit())return;try{store.update(current->{JSONArray a=current.getJSONArray("vpnProfiles");if(index<0||index>=a.length())throw new Exception();current.put("vpnSelected",index).put("mode","profile");});StateStore.mode="profile";StateStore.notifyChanged(this);renderProfiles();note.setText("Профиль выбран. Вернитесь к соединению и подключите туннель.");}catch(Exception e){note.setText("Не удалось сохранить профиль");}}
    private void removeSelected(){if(!canEdit())return;new AlertDialog.Builder(this).setTitle("Удалить выбранный VPN-профиль?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(dialog,which)->{try{store.update(current->{JSONArray old=current.optJSONArray("vpnProfiles"),next=new JSONArray();int selected=current.optInt("vpnSelected",0);if(old!=null)for(int i=0;i<old.length();i++)if(i!=selected)next.put(old.get(i));current.put("vpnProfiles",next).put("vpnSelected",0);});renderProfiles();StateStore.notifyChanged(this);}catch(Exception e){note.setText("Не удалось удалить профиль");}}).show();}
    private void paste(String initial){
        if(!canEdit())return;EditText input=new EditText(this);input.setMinLines(2);input.setMaxLines(DeviceLayout.isTV(this)?4:6);input.setTextSize(14);BlinkStyle.input(this,input);input.setVerticalScrollBarEnabled(true);input.setSaveEnabled(false);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_VARIATION_PASSWORD);input.setText(initial);
        new AlertDialog.Builder(this).setTitle("Ссылка или конфигурация").setMessage("URI, HTTPS-подписка, JSON, Clash или WireGuard/AmneziaWG .conf. Неподдерживаемые параметры отклоняются.").setView(input).setNegativeButton("Отмена",null).setPositiveButton("Импортировать",(dialog,which)->importText(input.getText().toString())).show();
    }
    private void openFile(){if(!canEdit())return;try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),77);}catch(android.content.ActivityNotFoundException e){note.setText("На этом ТВ нет системного выбора файла. Используйте «Поделиться», вставку или HTTPS-ссылку.");}}
    private void refreshFeed(){
        if(!canEdit())return;
        try{JSONArray a=store.load().optJSONArray("vpnProfiles");LinkedHashSet<String> sources=new LinkedHashSet<>();if(a!=null)for(int i=0;i<a.length();i++){String url=a.getJSONObject(i).optString("subscriptionURL");if(!url.isEmpty())sources.add(url);}String[] urls=sources.toArray(new String[0]);if(urls.length==0){note.setText("Сначала добавьте HTTPS-подписку");return;}String[] names=new String[urls.length];for(int i=0;i<urls.length;i++)names[i]=(i+1)+" · "+new java.net.URI(urls[i]).getHost();new AlertDialog.Builder(this).setTitle("Обновить VPN-подписку").setItems(names,(dialog,which)->importText(urls[which])).setNegativeButton("Отмена",null).show();}catch(Exception e){note.setText("Не удалось прочитать сохранённые ссылки");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=77||result!=RESULT_OK||data==null||!canEdit())return;busy=true;worker.execute(()->{try{String text;try(java.io.InputStream in=getContentResolver().openInputStream(data.getData())){text=read(in);}performImport(text,null);}catch(Exception e){error(e);}});}
    private void importText(String text){if(!canEdit())return;busy=true;worker.execute(()->{try{ProfileImportInput input=ProfileImportInput.parse(text);String url=input.subscriptionURL;String source=url==null?input.source:SubscriptionHTTP.fetchStandard(url);performImport(source,url);}catch(Exception e){error(e);}});}
    private void performImport(String text,String url)throws Exception{
        SubscriptionFeedNotice.validate(text);
        String parsed;try{parsed=Mobile.importProfiles(text);}catch(Exception e){throw new SafeImportException(e.getMessage());}
        JSONObject imported=new JSONObject(parsed);JSONArray added=imported.getJSONArray("profiles");if(StateStore.active())throw new Exception();
        store.update(current->{
            JSONArray old=current.optJSONArray("vpnProfiles"),next=new JSONArray();String previous="",name="",kind="";
            try{JSONObject chosen=ProfilesStore.selected(current);previous=chosen.getString("source");name=chosen.optString("name");kind=chosen.optString("kind");}catch(Exception ignored){}
            if(old!=null)for(int i=0;i<old.length();i++){JSONObject p=old.getJSONObject(i);if(url==null||!url.equals(p.optString("subscriptionURL")))next.put(p);}
            int first=next.length();for(int i=0;i<added.length();i++){JSONObject p=added.getJSONObject(i);if(url!=null)p.put("subscriptionURL",url);next.put(p);}if(next.length()>1024)throw new Exception();
            int selected=first;boolean exact=false;if(url!=null)for(int i=0;i<next.length();i++)if(previous.equals(next.getJSONObject(i).optString("source"))){selected=i;exact=true;break;}
            if(url!=null&&!exact)for(int i=first;i<next.length();i++){JSONObject p=next.getJSONObject(i);if(name.equals(p.optString("name"))&&kind.equals(p.optString("kind"))){selected=i;break;}}
            current.put("vpnProfiles",next).put("vpnSelected",selected).put("mode","profile");if(current.toString().getBytes(StandardCharsets.UTF_8).length>4194304||StateStore.active())throw new Exception();
        });
        JSONArray rejected=imported.getJSONArray("rejected");StringBuilder result=new StringBuilder("Добавлено: "+added.length()+". Отклонено: "+rejected.length()+". "+imported.optString("notice"));if(rejected.length()>0){result.append(" Позиции: ");for(int i=0;i<Math.min(16,rejected.length());i++){if(i>0)result.append(", ");result.append(rejected.getJSONObject(i).getInt("index"));}}
        StateStore.mode="profile";main.post(()->{busy=false;if(isDestroyed())return;renderProfiles();note.setText(result.toString());StateStore.notifyChanged(this);});
    }
    private static final class SafeImportException extends Exception{SafeImportException(String message){super(message);}}
    private void error(Exception error){String reason=(error instanceof SafeImportException||error instanceof SubscriptionHTTP.Failure)?error.getMessage():null;String message="Импорт не выполнен. Проверьте протокол, параметры, размер и HTTPS-ссылку.";if(reason!=null&&reason.length()<=200&&!reason.contains("://")&&!reason.contains("\n"))message+="\n"+reason;String result=message;main.post(()->{busy=false;if(!isDestroyed())note.setText(result);});}
    private static String read(java.io.InputStream in)throws Exception{if(in==null)throw new Exception();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>1048576)throw new Exception();out.write(b,0,n);}return out.toString("UTF-8");}
}

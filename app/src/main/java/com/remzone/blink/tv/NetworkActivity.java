package com.remzone.blink.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Remote-friendly settings controller. Import never connects without the user's action. */
public final class NetworkActivity extends Activity {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private SecureStore store;private JSONObject saved=new JSONObject();private Subscription subscription;
    private boolean busy,updating;
    private TextView status,detail,profile,expiry,revision,traffic,diagnostic,notice;
    private Button connect,refresh,select,probe,importLink,forgetLink,captcha,transport;private RadioButton vk,direct,ordinary;private RadioGroup modeGroup;
    private final ContentObserver observer=new ContentObserver(main){@Override public void onChange(boolean self){render();}};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);if(DeviceLayout.isTV(this))getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);store=new SecureStore(this);buildUI();loadSaved();handleIntent(getIntent());
        if(getIntent().getBooleanExtra("requestConnect",false)&&trustedCaller())connectRequested();
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}
    private boolean trustedCaller(){String caller=getCallingPackage();return caller!=null&&getPackageManager().checkSignatures(getPackageName(),caller)==android.content.pm.PackageManager.SIGNATURE_MATCH;}
    private void handleIntent(Intent intent){CharSequence shared=Intent.ACTION_SEND.equals(intent.getAction())?intent.getCharSequenceExtra(Intent.EXTRA_TEXT):null;String source=shared!=null&&shared.length()<=1048576?shared.toString():Intent.ACTION_VIEW.equals(intent.getAction())?intent.getDataString():null;if(source!=null&&source.length()<=1048576){String initial=source;main.post(()->startActivity(new Intent(this,ProfilesActivity.class).putExtra("importSource",initial)));intent.removeExtra(Intent.EXTRA_TEXT);intent.setData(null);}String url=intent.getStringExtra("subscriptionUrl");if(url!=null&&url.length()<=2048)main.post(()->importDialog(url));String screen=intent.getStringExtra("screen");if("captcha".equals(screen)&&TunnelService.hasPendingCaptcha())main.post(()->startActivity(new Intent(this,CaptchaActivity.class)));main.post(()->{if("subscription".equals(screen))refresh.requestFocus();else if("diagnostics".equals(screen))startActivity(new Intent(this,DiagnosticsActivity.class));else if("connection".equals(screen))connect.requestFocus();});}
    @Override protected void onResume(){super.onResume();getContentResolver().registerContentObserver(StateStore.URI,true,observer);loadSaved();}
    @Override protected void onPause(){getContentResolver().unregisterContentObserver(observer);super.onPause();}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
    private BlinkOrbit orbit;
    private int dp(int n){return BlinkStyle.dp(this,n);}
    private TextView text(String s,int size,int color){return BlinkStyle.text(this,s,size,color);}
    private LinearLayout column(){return BlinkStyle.column(this);}
    private Button button(LinearLayout parent,String label,Runnable action){return BlinkStyle.button(this,parent,label,action);}
    private void buildUI(){
        LinearLayout root=BlinkStyle.page(this,"Твоя сеть. Твои правила.","Один Blink — и ты на связи.");
        boolean wide=DeviceLayout.twoColumns(getResources().getConfiguration());
        LinearLayout panels=new LinearLayout(this);panels.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);panels.setClipChildren(false);root.addView(panels);
        LinearLayout connection=BlinkStyle.card(this,panels,"");LinearLayout account=BlinkStyle.card(this,panels,"Твоё подключение");
        if(wide){LinearLayout.LayoutParams left=(LinearLayout.LayoutParams)connection.getLayoutParams();left.width=0;left.weight=1.05f;left.rightMargin=dp(18);connection.setLayoutParams(left);LinearLayout.LayoutParams right=(LinearLayout.LayoutParams)account.getLayoutParams();right.width=0;right.weight=1;account.setLayoutParams(right);}
        TextView eyebrow=text("ПРОСТО БУДЬ НА СВЯЗИ",11,BlinkStyle.MUTED);eyebrow.setLetterSpacing(.12f);connection.addView(eyebrow);
        orbit=new BlinkOrbit(this);LinearLayout.LayoutParams orb=new LinearLayout.LayoutParams(-1,dp(DeviceLayout.isTV(this)?130:154));connection.addView(orbit,orb);
        status=text("Всё готово к подключению",25,BlinkStyle.TEXT);status.setGravity(Gravity.CENTER);status.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));connection.addView(status);
        detail=text("Выбери сервер — остальное возьмёт на себя Blink.",13,BlinkStyle.MUTED);detail.setGravity(Gravity.CENTER);connection.addView(detail);
        connect=button(connection,"Подключить туннель",this::connectRequested);BlinkStyle.decorate(this,connect,"power",true);connect.setId(View.generateViewId());connect.setContentDescription("Переключить туннель Blink");
        captcha=button(connection,"Пройти проверку VK",()->startActivity(new Intent(this,CaptchaActivity.class)));
        button(connection,"Проверить связь",()->startActivity(new Intent(this,DiagnosticsActivity.class)));
        profile=text("Добавь первое подключение",21,BlinkStyle.TEXT);profile.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));account.addView(profile);expiry=text("",13,BlinkStyle.MUTED);account.addView(expiry);revision=text("",12,BlinkStyle.MUTED);account.addView(revision);
        select=button(account,"Выбрать профиль",this::selectProfile);
        button(account,"Все серверы и подписки",()->startActivity(new Intent(this,ProfilesActivity.class)));
        refresh=button(account,"Обновить подписку",()->{if("profile".equals(saved.optString("mode")))openProfiles();else refresh(null);});importLink=button(account,"Добавить / заменить ссылку",()->{if("profile".equals(saved.optString("mode")))openProfiles();else importDialog("");});forgetLink=button(account,"Забыть подписку",()->{if("profile".equals(saved.optString("mode")))openProfiles();else forget();});
        notice=text("Твои ссылки и ключи остаются в защищённом хранилище устройства.",12,BlinkStyle.MUTED);account.addView(notice);
        LinearLayout routing=BlinkStyle.card(this,root,"Как подключаться");routing.addView(text("Выбери маршрут, который подходит тебе. Режим меняется при отключённом туннеле.",13,BlinkStyle.MUTED));
        modeGroup=new RadioGroup(this);modeGroup.setOrientation(LinearLayout.VERTICAL);vk=new RadioButton(this);direct=new RadioButton(this);ordinary=new RadioButton(this);RadioButton[] choices={ordinary,vk,direct};String[] labels={"Обычный VPN-профиль","Через VK TURN","Прямой UDP — тест сервера"};for(int i=0;i<choices.length;i++){RadioButton r=choices[i];r.setId(View.generateViewId());r.setText(labels[i]);r.setTextColor(BlinkStyle.TEXT);r.setTextSize(15);r.setMinHeight(dp(48));r.setPadding(dp(8),dp(6),dp(8),dp(6));modeGroup.addView(r);}routing.addView(modeGroup);
        modeGroup.setOnCheckedChangeListener((group,id)->{if(updating)return;String mode=id==ordinary.getId()?"profile":id==direct.getId()?"direct":"vk";try{saved=store.update(current->current.put("mode",mode));StateStore.mode=mode;render();}catch(Exception ex){showError(ex);}});
        transport=button(routing,"VK: TCP",this::chooseTransport);routing.addView(text("Прямой режим — отдельная проверка сервера без VK. Blink не меняет маршрут автоматически.",12,BlinkStyle.MUTED));
        LinearLayout diagnostics=BlinkStyle.card(this,root,"Связь в деталях");traffic=text("",15,BlinkStyle.TEXT);diagnostics.addView(traffic);diagnostic=text("",13,BlinkStyle.MUTED);diagnostics.addView(diagnostic);probe=button(diagnostics,"Проверить интернет через туннель",this::probeInternet);
        button(diagnostics,"Wi-Fi и Ethernet",()->{try{startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));}catch(Exception ex){Toast.makeText(this,"Настройки сети недоступны",Toast.LENGTH_SHORT).show();}});diagnostics.addView(text("Статус туннеля и доступ к сайтам — разные вещи. Проверь соединение здесь, если что-то не открывается.",12,BlinkStyle.MUTED));
        if(DeviceLayout.isTV(this))connect.requestFocus();
    }
    private void loadSaved(){try{saved=store.load();subscription=null;if(saved.has("document"))try{subscription=Subscription.parse(saved.getString("document"),System.currentTimeMillis()/1000,0);}catch(Exception ex){notice.setText("Срок подписки истёк. Обновите её или замените ссылку.");}}catch(Exception ex){showError(ex);}render();}
    private void render(){
        if(status==null)return;captcha.setVisibility("captcha_required".equals(StateStore.state)&&TunnelService.hasPendingCaptcha()?View.VISIBLE:View.GONE);boolean active=StateStore.active();status.setText("stopped".equals(StateStore.state)?"Всё готово к подключению":StateStore.label());orbit.state(StateStore.state);detail.setText("stopped".equals(StateStore.state)?"Выбери сервер — остальное возьмёт на себя Blink.":StateStore.detail);connect.setText(active?"Отключить туннель":"Подключить туннель");connect.setEnabled(!busy||active);
        updating=true;boolean isDirect="direct".equals(saved.optString("mode","vk"));boolean isOrdinary="profile".equals(saved.optString("mode"));modeGroup.check(isOrdinary?ordinary.getId():isDirect?direct.getId():vk.getId());updating=false;vk.setEnabled(!active&&!busy);direct.setEnabled(!active&&!busy);ordinary.setEnabled(!active&&!busy);
        importLink.setEnabled(!busy);forgetLink.setEnabled(!busy);
        transport.setText("VK: "+Subscription.transport(saved).toUpperCase(java.util.Locale.ROOT));transport.setEnabled(!active&&!busy&&!isDirect&&!isOrdinary);
        select.setEnabled(!active&&!busy&&(isOrdinary?ProfilesStore.valid(saved):subscription!=null&&subscription.profiles.size()>1));refresh.setText(isOrdinary?"Управлять VPN-подписками":"Обновить подписку");importLink.setText(isOrdinary?"Добавить VPN-профиль":"Добавить / заменить ссылку");forgetLink.setText(isOrdinary?"Удалить VPN-профиль":"Забыть подписку");transport.setVisibility(isOrdinary?View.GONE:View.VISIBLE);refresh.setEnabled(!busy&&(isOrdinary||saved.has("url")));probe.setEnabled(!busy&&"ready".equals(StateStore.state));
        if(subscription!=null){int selected=Math.min(Math.max(0,saved.optInt("selected",0)),subscription.profiles.size()-1);profile.setText(subscription.profiles.get(selected).name);expiry.setText("Действует до "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(subscription.expiresAt*1000)));revision.setText("Версия "+subscription.revision+" · профилей: "+subscription.profiles.size());}
        else if(!saved.has("document")){profile.setText("Подписка не добавлена");expiry.setText("Добавьте ссылку из панели Vokop");revision.setText("");}
        else{profile.setText("Подписка требует обновления");expiry.setText("Подключение недоступно до загрузки действующей подписки");revision.setText("");}
        if(isOrdinary){try{JSONObject p=ProfilesStore.selected(saved);profile.setText(p.optString("name","VPN")+" · "+p.optString("kind"));expiry.setText("Обычный VPN-профиль · срок определяется сервером");revision.setText("Подключение напрямую, без VK");}catch(Exception e){profile.setText("Выберите обычный VPN-профиль");expiry.setText("Откройте VPN-профили и импортируйте конфигурацию");revision.setText("");}}
        traffic.setText(String.format(java.util.Locale.ROOT,"Отправлено %.1f КБ   ·   Получено %.1f КБ",StateStore.tx/1024.0,StateStore.rx/1024.0));diagnostic.setText(StateStore.probe+(StateStore.egress.isEmpty()?"":"\nВнешний IP: "+StateStore.egress+" · "+StateStore.rtt+" мс")+(StateStore.timings.isEmpty()?"":"\n"+StateStore.timings));
    }
    private void connectRequested(){
        if(StateStore.active()){startService(new Intent(this,TunnelService.class).setAction(TunnelService.STOP));return;}
        if("profile".equals(saved.optString("mode"))){if(!ProfilesStore.valid(saved)){startActivity(new Intent(this,ProfilesActivity.class));return;}}else if(subscription==null){importDialog("");return;}Intent consent=VpnService.prepare(this);if(consent!=null)startActivityForResult(consent,29);else startVPN();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==29){if(result==RESULT_OK)startVPN();else Toast.makeText(this,"VPN не включён: разрешение не выдано",Toast.LENGTH_LONG).show();}}
    private void startVPN(){startForegroundService(new Intent(this,TunnelService.class).setAction(TunnelService.START));}
    private void importDialog(String initial){
        EditText input=new EditText(this);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);input.setHint("https://панель/…/api/vokop/v1/subscriptions/…");input.setText(initial);input.setTextSize(16);
        new AlertDialog.Builder(this).setTitle("Ссылка подписки Vokop").setMessage("Ссылка содержит ключ доступа. Она хранится в зашифрованном виде и не передаётся лаунчеру.").setView(input).setNegativeButton("Отмена",null).setPositiveButton("Загрузить",(dialog,which)->refresh(input.getText().toString().trim())).show();
    }
    private void refresh(String newURL){
        if(busy)return;busy=true;render();worker.execute(()->{
            try{
                String url=Subscription.validateURL(newURL==null?saved.getString("url"):newURL);long minimum=url.equals(saved.optString("url"))?saved.optLong("revision",0):0;
                String doc=SubscriptionHTTP.fetch(url);Subscription value=Subscription.parse(doc,System.currentTimeMillis()/1000,minimum);
                JSONObject updated=store.update(current->{
                    int selected=0;String previousID="";
                    if(url.equals(current.optString("url"))){try{org.json.JSONArray old=new JSONObject(current.getString("document")).getJSONArray("profiles");previousID=old.getJSONObject(current.optInt("selected",0)).getString("id");}catch(Exception ignored){}}
                    for(int i=0;i<value.profiles.size();i++)if(previousID.equals(value.profiles.get(i).id)){selected=i;break;}
                    current.put("url",url).put("document",doc).put("revision",value.revision).put("selected",selected);
                });
                main.post(()->{if(isDestroyed())return;saved=updated;subscription=value;busy=false;notice.setText("Подписка обновлена. Ссылка и ключи сохранены защищённо.");render();StateStore.notifyChanged(this);if(StateStore.active())startForegroundService(new Intent(this,TunnelService.class).setAction(TunnelService.RESTART));});
            }catch(Exception ex){main.post(()->{busy=false;showError(ex);render();});}
        });
    }
    private void chooseTransport(){new AlertDialog.Builder(this).setTitle("Транспорт до VK TURN").setSingleChoiceItems(new String[]{"Авто — UDP с резервным TCP", "UDP — меньше задержка", "TCP — совместимость"},"udp".equals(Subscription.transport(saved))?1:"tcp".equals(Subscription.transport(saved))?2:0,(dialog,which)->{try{saved=store.update(current->current.put("turnTransport",which==1?"udp":which==2?"tcp":"auto").put("turnTransportPolicy",2));render();dialog.dismiss();}catch(Exception ex){showError(ex);}}).setNegativeButton("Назад",null).show();}
    private void openProfiles(){startActivity(new Intent(this,ProfilesActivity.class));}
    private void selectProfile(){if("profile".equals(saved.optString("mode"))){startActivity(new Intent(this,ProfilesActivity.class));return;}if(subscription==null)return;String[] names=new String[subscription.profiles.size()];for(int i=0;i<names.length;i++)names[i]=subscription.profiles.get(i).name;new AlertDialog.Builder(this).setTitle("Профиль подписки").setSingleChoiceItems(names,saved.optInt("selected",0),(dialog,which)->{try{saved=store.update(current->current.put("selected",which));render();StateStore.notifyChanged(this);dialog.dismiss();}catch(Exception ex){showError(ex);}}).setNegativeButton("Назад",null).show();}
    private void forget(){new AlertDialog.Builder(this).setTitle("Забыть подписку Vokop?").setMessage("Ссылка и ключи подписки Vokop будут удалены.").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(dialog,which)->{try{if(StateStore.active()&&!"profile".equals(saved.optString("mode")))startService(new Intent(this,TunnelService.class).setAction(TunnelService.STOP));saved=store.update(current->{current.remove("url");current.remove("document");current.remove("revision");});subscription=null;notice.setText("Подписка Vokop удалена");StateStore.notifyChanged(this);render();}catch(Exception e){showError(e);}}).show();}
    private void probeInternet(){
        if(busy)return;busy=true;StateStore.probe="Выполняется HTTPS-запрос через VPN…";render();worker.execute(()->{
            try{
                ConnectivityManager cm=getSystemService(ConnectivityManager.class);NetworkCapabilities caps=cm.getNetworkCapabilities(cm.getActiveNetwork());
                if(caps==null||!caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))throw new Exception("Система не использует VPN для приложения");
                android.net.Network network=cm.getActiveNetwork();NetworkProbe.Result result=NetworkProbe.measure(network);
                caps=cm.getNetworkCapabilities(cm.getActiveNetwork());if(caps==null||!caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)||!network.equals(cm.getActiveNetwork()))throw new Exception("VPN отключился во время проверки");
                StateStore.rtt=result.totalMs;StateStore.egress=result.ip;StateStore.timings=result.summary();StateStore.probe="HTTPS-запрос через VPN выполнен успешно";
            }catch(Exception ex){StateStore.egress="";StateStore.timings=ex instanceof NetworkProbe.Failure?((NetworkProbe.Failure)ex).summary():"";StateStore.probe="Интернет через VPN не подтверждён. Проверьте режим, подписку и сервер.";}
            finally{main.post(()->{busy=false;render();StateStore.notifyChanged(this);});}
        });
    }
    private void showError(Exception ex){String msg=ex.getMessage();if(msg==null||msg.isEmpty()||Character.UnicodeScript.of(msg.charAt(0))!=Character.UnicodeScript.CYRILLIC)msg="Не удалось прочитать подписку. Проверьте ссылку и сеть.";Toast.makeText(this,msg,Toast.LENGTH_LONG).show();if(notice!=null)notice.setText(msg);}
}

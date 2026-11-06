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
    private Button connect,refresh,select,probe,importLink,forgetLink,captcha,transport;private RadioButton vk,direct;
    private final ContentObserver observer=new ContentObserver(main){@Override public void onChange(boolean self){render();}};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);if(DeviceLayout.isTV(this))getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);store=new SecureStore(this);buildUI();loadSaved();handleIntent(getIntent());
        if(getIntent().getBooleanExtra("requestConnect",false)&&trustedCaller())connectRequested();
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}
    private boolean trustedCaller(){String caller=getCallingPackage();return caller!=null&&getPackageManager().checkSignatures(getPackageName(),caller)==android.content.pm.PackageManager.SIGNATURE_MATCH;}
    private void handleIntent(Intent intent){String url=intent.getStringExtra("subscriptionUrl");if(url!=null&&url.length()<=2048)main.post(()->importDialog(url));String screen=intent.getStringExtra("screen");if("captcha".equals(screen)&&TunnelService.hasPendingCaptcha())main.post(()->startActivity(new Intent(this,CaptchaActivity.class)));main.post(()->{if("subscription".equals(screen))refresh.requestFocus();else if("diagnostics".equals(screen))probe.requestFocus();else if("connection".equals(screen))connect.requestFocus();});}
    @Override protected void onResume(){super.onResume();getContentResolver().registerContentObserver(StateStore.URI,true,observer);render();}
    @Override protected void onPause(){getContentResolver().unregisterContentObserver(observer);super.onPause();}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(3),0,dp(7));return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout card(String title){LinearLayout l=column();l.setPadding(dp(22),dp(18),dp(22),dp(18));GradientDrawable bg=new GradientDrawable();bg.setColor(0xff182535);bg.setCornerRadius(dp(14));l.setBackground(bg);TextView heading=text(title,21,0xffe3ebf5);heading.setTypeface(null,Typeface.BOLD);l.addView(heading);return l;}
    private Button button(LinearLayout parent,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(15);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);b.setPadding(dp(16),0,dp(16),0);StateListDrawable states=new StateListDrawable();GradientDrawable focused=new GradientDrawable();focused.setColor(0xff286861);focused.setCornerRadius(dp(9));focused.setStroke(dp(2),0xff91f2dc);GradientDrawable plain=new GradientDrawable();plain.setColor(0xff25394e);plain.setCornerRadius(dp(9));states.addState(new int[]{android.R.attr.state_focused},focused);states.addState(new int[]{},plain);b.setBackground(states);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(48));params.topMargin=dp(8);parent.addView(b,params);b.setOnClickListener(v->action.run());return b;}
    private void buildUI(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);LinearLayout root=column();root.setPadding(dp(DeviceLayout.isTV(this)?40:16),dp(25),dp(DeviceLayout.isTV(this)?40:16),dp(35));scroll.addView(root);
        TextView brand=text("BLINK  /  Сетевые настройки",30,Color.WHITE);brand.setTypeface(null,Typeface.BOLD);root.addView(brand);root.addView(text("Подписка Vokop · VLESS · VK TURN",15,0xff9eafc3));
        boolean wide=DeviceLayout.twoColumns(getResources().getConfiguration().screenWidthDp);LinearLayout panels=new LinearLayout(this);panels.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);LinearLayout.LayoutParams panelParams=new LinearLayout.LayoutParams(-1,-2);panelParams.topMargin=dp(12);root.addView(panels,panelParams);
        LinearLayout connection=card("Соединение");LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(wide?0:-1,-2,wide?1.1f:0);left.rightMargin=wide?dp(16):0;left.bottomMargin=wide?0:dp(16);panels.addView(connection,left);
        status=text("Отключён",26,0xff65dfc3);status.setTypeface(null,Typeface.BOLD);connection.addView(status);detail=text("",14,0xffc5d3e2);connection.addView(detail);
        connect=button(connection,"Подключить туннель",this::connectRequested);connect.setId(View.generateViewId());connect.setContentDescription("Переключить туннель Blink");
        captcha=button(connection,"Пройти проверку VK",()->startActivity(new Intent(this,CaptchaActivity.class)));
        RadioGroup modes=new RadioGroup(this);modes.setOrientation(LinearLayout.VERTICAL);vk=new RadioButton(this);vk.setId(View.generateViewId());vk.setText("Через VK TURN");vk.setTextSize(16);direct=new RadioButton(this);direct.setId(View.generateViewId());direct.setText("Прямой UDP — тест сервера");direct.setTextSize(16);modes.addView(vk);modes.addView(direct);connection.addView(modes);
        modes.setOnCheckedChangeListener((group,id)->{if(updating)return;String mode=id==direct.getId()?"direct":"vk";try{saved.put("mode",mode);store.save(saved);StateStore.mode=mode;render();}catch(Exception ex){showError(ex);}});
        transport=button(connection,"VK: TCP",this::chooseTransport);
        connection.addView(text("Прямой режим проверяет сервер без инфраструктуры VK. Автоматического переключения нет. Локальная сеть доступна напрямую.",12,0xff9eafc3));
        button(connection,"Wi-Fi и Ethernet",()->{try{startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));}catch(Exception ex){Toast.makeText(this,"Настройки сети недоступны",Toast.LENGTH_SHORT).show();}});
        LinearLayout account=card("Подписка");panels.addView(account,new LinearLayout.LayoutParams(wide?0:-1,-2,wide?1:0));profile=text("Подписка не добавлена",20,Color.WHITE);account.addView(profile);expiry=text("",15,0xffc5d3e2);account.addView(expiry);revision=text("",13,0xff9eafc3);account.addView(revision);
        select=button(account,"Выбрать профиль",this::selectProfile);refresh=button(account,"Обновить подписку",()->refresh(null));importLink=button(account,"Добавить / заменить ссылку",()->importDialog(""));forgetLink=button(account,"Забыть подписку",this::forget);
        notice=text("Вставьте HTTPS-ссылку подписки из панели. Пароль администратора здесь не нужен.",12,0xff9eafc3);account.addView(notice);
        LinearLayout diagnostics=card("Диагностика");LinearLayout.LayoutParams diagParams=new LinearLayout.LayoutParams(-1,-2);diagParams.topMargin=dp(16);root.addView(diagnostics,diagParams);traffic=text("",15,0xffc5d3e2);diagnostics.addView(traffic);diagnostic=text("",14,0xff9eafc3);diagnostics.addView(diagnostic);probe=button(diagnostics,"Проверить интернет через туннель",this::probeInternet);diagnostics.addView(text("Готовность туннеля и успешный интернет-запрос — отдельные проверки. Во время ручной проверки VK используется обычная сеть.",12,0xff9eafc3));
        setContentView(scroll);connect.requestFocus();String screen=getIntent().getStringExtra("screen");if("subscription".equals(screen))refresh.requestFocus();if("diagnostics".equals(screen))probe.requestFocus();
    }
    private void loadSaved(){try{saved=store.load();subscription=null;if(saved.has("document"))try{subscription=Subscription.parse(saved.getString("document"),System.currentTimeMillis()/1000,0);}catch(Exception ex){notice.setText("Срок подписки истёк. Обновите её или замените ссылку.");}}catch(Exception ex){showError(ex);}render();}
    private void render(){
        if(status==null)return;captcha.setVisibility("captcha_required".equals(StateStore.state)&&TunnelService.hasPendingCaptcha()?View.VISIBLE:View.GONE);boolean active=StateStore.active();status.setText(StateStore.label());detail.setText(StateStore.detail);connect.setText(active?"Отключить туннель":"Подключить туннель");connect.setEnabled(active||(!busy&&subscription!=null));
        updating=true;boolean isDirect="direct".equals(saved.optString("mode","vk"));direct.setChecked(isDirect);vk.setChecked(!isDirect);updating=false;vk.setEnabled(!active&&!busy);direct.setEnabled(!active&&!busy);
        importLink.setEnabled(!busy);forgetLink.setEnabled(!busy);
        transport.setText("VK: "+saved.optString("turnTransport","tcp").toUpperCase(java.util.Locale.ROOT));transport.setEnabled(!active&&!busy&&!isDirect);
        select.setEnabled(!active&&!busy&&subscription!=null&&subscription.profiles.size()>1);refresh.setEnabled(!busy&&saved.has("url"));probe.setEnabled(!busy&&"ready".equals(StateStore.state));
        if(subscription!=null){int selected=Math.min(Math.max(0,saved.optInt("selected",0)),subscription.profiles.size()-1);profile.setText(subscription.profiles.get(selected).name);expiry.setText("Действует до "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(subscription.expiresAt*1000)));revision.setText("Версия "+subscription.revision+" · профилей: "+subscription.profiles.size());}
        else if(!saved.has("document")){profile.setText("Подписка не добавлена");expiry.setText("Добавьте ссылку из панели Vokop");revision.setText("");}
        else{profile.setText("Подписка требует обновления");expiry.setText("Подключение недоступно до загрузки действующей подписки");revision.setText("");}
        traffic.setText(String.format(java.util.Locale.ROOT,"Отправлено %.1f КБ   ·   Получено %.1f КБ",StateStore.tx/1024.0,StateStore.rx/1024.0));diagnostic.setText(StateStore.probe+(StateStore.egress.isEmpty()?"":"\nВнешний IP: "+StateStore.egress+" · "+StateStore.rtt+" мс")+(StateStore.timings.isEmpty()?"":"\n"+StateStore.timings));
    }
    private void connectRequested(){
        if(StateStore.active()){startService(new Intent(this,TunnelService.class).setAction(TunnelService.STOP));return;}
        if(subscription==null){importDialog("");return;}Intent consent=VpnService.prepare(this);if(consent!=null)startActivityForResult(consent,29);else startVPN();
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
                JSONObject updated=new JSONObject().put("url",url).put("document",doc).put("revision",value.revision).put("mode",saved.optString("mode","vk")).put("turnTransport",saved.optString("turnTransport","tcp")).put("selected",0);store.save(updated);
                main.post(()->{if(isDestroyed())return;saved=updated;subscription=value;busy=false;notice.setText("Подписка обновлена. Ссылка и ключи сохранены защищённо.");render();StateStore.notifyChanged(this);if(StateStore.active())startForegroundService(new Intent(this,TunnelService.class).setAction(TunnelService.RESTART));});
            }catch(Exception ex){main.post(()->{busy=false;showError(ex);render();});}
        });
    }
    private void chooseTransport(){new AlertDialog.Builder(this).setTitle("Транспорт до VK TURN").setSingleChoiceItems(new String[]{"TCP — совместимость", "UDP — сравнить задержки"},"udp".equals(saved.optString("turnTransport","tcp"))?1:0,(dialog,which)->{try{saved.put("turnTransport",which==1?"udp":"tcp");store.save(saved);render();dialog.dismiss();}catch(Exception ex){showError(ex);}}).setNegativeButton("Назад",null).show();}
    private void selectProfile(){if(subscription==null)return;String[] names=new String[subscription.profiles.size()];for(int i=0;i<names.length;i++)names[i]=subscription.profiles.get(i).name;new AlertDialog.Builder(this).setTitle("Профиль подписки").setSingleChoiceItems(names,saved.optInt("selected",0),(dialog,which)->{try{saved.put("selected",which);store.save(saved);render();StateStore.notifyChanged(this);dialog.dismiss();}catch(Exception ex){showError(ex);}}).setNegativeButton("Назад",null).show();}
    private void forget(){new AlertDialog.Builder(this).setTitle("Забыть подписку?").setMessage("Туннель будет отключён, ссылка и ключи удалены с телевизора.").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(dialog,which)->{if(StateStore.active())startService(new Intent(this,TunnelService.class).setAction(TunnelService.STOP));store.clear();saved=new JSONObject();subscription=null;notice.setText("Подписка удалена");StateStore.notifyChanged(this);render();}).show();}
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

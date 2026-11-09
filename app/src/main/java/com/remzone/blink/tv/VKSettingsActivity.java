package com.remzone.blink.tv;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;

public final class VKSettingsActivity extends Activity {
    private Spinner transport,preset,connections,pace;
    private EditText relay,dns,mtu,window,interval;
    private CheckBox congestion,serverDefaults;
    private TextView note;
    private Spinner choice(LinearLayout parent,String title,String[] entries,int selected){parent.addView(BlinkStyle.text(this,title,14,BlinkStyle.MUTED));Spinner v=new Spinner(this);v.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,entries));v.setSelection(Math.max(0,selected));parent.addView(v);return v;}
    private EditText field(LinearLayout parent,String title,String value,boolean number){parent.addView(BlinkStyle.text(this,title,14,BlinkStyle.MUTED));EditText v=new EditText(this);v.setSingleLine();v.setText(value);v.setInputType(number?android.text.InputType.TYPE_CLASS_NUMBER:android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);BlinkStyle.input(this,v);parent.addView(v);return v;}
    @Override public void onCreate(Bundle state){super.onCreate(state);try{
        VKOptions v=VKOptions.from(new SecureStore(this).load());LinearLayout page=BlinkStyle.page(this,"Параметры TURN","Применяются при следующем подключении. Исходный серверный протокол сохраняется.");
        LinearLayout path=BlinkStyle.card(this,page,"Путь до релея");transport=choice(path,"Транспорт",new String[]{"Авто UDP / TCP","UDP","TCP"},"udp".equals(v.transport)?1:"tcp".equals(v.transport)?2:0);relay=field(path,"TURN: host:port или [IPv6]:port; пусто — от VK",v.relay,false);dns=field(path,"DNS до VPN, IP-адрес",v.dns,false);
        path.addView(BlinkStyle.text(this,"Ручной адрес должен принимать учётные данные VK. Он не включает авторизацию Яндекса.",12,BlinkStyle.MUTED));
        LinearLayout tuning=BlinkStyle.card(this,page,"Соединения и KCP");String[] counts=new String[17];counts[0]="Как в подписке";for(int i=1;i<=16;i++)counts[i]="Соединений: "+i;connections=choice(tuning,"По умолчанию 1 для стабильности",counts,v.connections);
        preset=choice(tuning,"Профиль KCP",new String[]{"Совместимость · окно 256 / 20 мс","Сбалансированный · окно 64 / 20 мс","Бережный · окно 32 / 40 мс","Вручную"},java.util.Arrays.asList("compatibility","balanced","conservative","custom").indexOf(v.preset));
        serverDefaults=new CheckBox(this);serverDefaults.setText("Настройки панели (вместо локальных значений)");serverDefaults.setTextColor(BlinkStyle.TEXT);serverDefaults.setChecked(v.serverDefaults);tuning.addView(serverDefaults);
        mtu=field(tuning,"MTU KCP 576–1200 (интерфейс VPN: 1280)",String.valueOf(v.mtu),true);window=field(tuning,"Ручное окно 16–256",String.valueOf(v.window),true);interval=field(tuning,"Ручной интервал 10–100 мс",String.valueOf(v.interval),true);congestion=new CheckBox(this);congestion.setText("Контроль перегрузки в ручном профиле");congestion.setTextColor(BlinkStyle.TEXT);congestion.setChecked(v.congestion);tuning.addView(congestion);
        int[] rates={0,64,128,256,512,1024};int index=0;for(int i=0;i<rates.length;i++)if(rates[i]==v.pace)index=i;pace=choice(tuning,"Сглаживание исходящих на одно соединение",new String[]{"Выключено","64 КиБ/с","128 КиБ/с","256 КиБ/с","512 КиБ/с","1024 КиБ/с"},index);tuning.addView(BlinkStyle.text(this,"Меньшие окна и сглаживание помогают проверить потери, но могут снизить скорость. WG использует DTLS/SRTP/WRAP из подписки. WRAP-A, WRAP-S и csqtt пока не поддерживаются. CAPTCHA проходит вручную.",12,BlinkStyle.MUTED));
        note=BlinkStyle.text(this,"",14,BlinkStyle.MUTED);page.addView(note);
        BlinkStyle.button(this,page,"Сохранить",()->{try{if(StateStore.active())throw new Exception();VKOptions n=new VKOptions();n.transport=new String[]{"auto","udp","tcp"}[transport.getSelectedItemPosition()];n.preset=new String[]{"compatibility","balanced","conservative","custom"}[preset.getSelectedItemPosition()];n.relay=relay.getText().toString().trim();n.dns=dns.getText().toString().trim();n.connections=connections.getSelectedItemPosition();n.mtu=Integer.parseInt(mtu.getText().toString());n.window=Integer.parseInt(window.getText().toString());n.interval=Integer.parseInt(interval.getText().toString());n.serverDefaults=serverDefaults.isChecked();n.congestion=congestion.isChecked();n.pace=rates[pace.getSelectedItemPosition()];if(!n.valid())throw new Exception();new SecureStore(this).update(o->o.put("vkOptions",n.json()));note.setText("Сохранено. Настройки применятся при следующем подключении.");}catch(Exception e){note.setText("Отключите VPN и проверьте адреса и диапазоны значений.");}});
        BlinkStyle.button(this,page,"Восстановить исходные настройки",()->{try{if(StateStore.active())return;new SecureStore(this).update(o->o.put("vkOptions",new VKOptions().json()));recreate();}catch(Exception e){note.setText("Не удалось сохранить настройки");}});BlinkStyle.button(this,page,"Назад",this::finish);
    }catch(Exception e){finish();}}
}

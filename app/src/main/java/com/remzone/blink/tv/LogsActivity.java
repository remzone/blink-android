package com.remzone.blink.tv;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.util.concurrent.*;

public final class LogsActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());
    private TextView status,events;private Button send,reset;private boolean busy;
    @Override public void onCreate(Bundle state){super.onCreate(state);DiagnosticJournal.init(this);LinearLayout page=BlinkStyle.page(this,"Журнал диагностики","Записи остаются на устройстве. Отправка на панель — только по кнопке, без фоновой передачи.");LinearLayout recording=BlinkStyle.card(this,page,"Запись журнала");Switch collect=new Switch(this);collect.setText("Собирать диагностический журнал");collect.setTextColor(BlinkStyle.TEXT);collect.setChecked(DiagnosticJournal.enabled());recording.addView(collect);recording.addView(BlinkStyle.text(this,"Выключите, чтобы не записывать новые события приложения и ядра. Ранее сохранённые записи можно удалить или отправить вручную.",12,BlinkStyle.MUTED));collect.setOnCheckedChangeListener((v,on)->{DiagnosticJournal.setEnabled(this,on);refresh();});LinearLayout delivery=BlinkStyle.card(this,page,"Передача в панель");status=BlinkStyle.text(this,"",14,BlinkStyle.MUTED);delivery.addView(status);send=BlinkStyle.button(this,delivery,"Отправить накопленный журнал",()->run(()->DiagnosticUpload.send(this)));
        EditText link=new EditText(this);link.setSingleLine();link.setHint("HTTPS-ссылка диагностики из панели");link.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);BlinkStyle.input(this,link);delivery.addView(link);BlinkStyle.button(this,delivery,"Сохранить сервер диагностики",()->{if(busy)return;try{DiagnosticJournal.configure(this,link.getText().toString().trim());link.setText("");status.setText("Сервер сохранён. Нажмите кнопку отправки, когда будете готовы.");}catch(Exception e){status.setText("Нужна диагностическая ссылка из панели → Диагностика.");}});
        delivery.addView(BlinkStyle.text(this,"При ручной отправке возможна попытка через обычную сеть, если VPN не пропускает запрос. Ключи, ссылки конфигураций и содержимое трафика не записываются.",12,BlinkStyle.MUTED));
        reset=BlinkStyle.button(this,page,"Сбросить локальный журнал",()->{if(StateStore.active()||TunnelService.hasLiveTunnel()){status.setText("Для сброса отключите VPN.");return;}new AlertDialog.Builder(this).setTitle("Удалить локальные записи и очередь?").setMessage("Серверные копии удаляются отдельно в панели. Адрес сервера сохранится.").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->run(()->{DiagnosticJournal.reset(this);return "Локальный журнал очищен.";})).show();});
        LinearLayout journal=BlinkStyle.card(this,page,"Последние события");events=BlinkStyle.text(this,"",12,BlinkStyle.MUTED);events.setTextIsSelectable(true);journal.addView(events);BlinkStyle.button(this,page,"Обновить журнал",this::refresh);BlinkStyle.button(this,page,"Назад",this::finish);refresh();}
    private interface Operation{String run()throws Exception;}
    private void run(Operation operation){if(busy)return;busy=true;send.setEnabled(false);reset.setEnabled(false);status.setText("Выполняется…");worker.execute(()->{String result;try{result=operation.run();}catch(Exception e){result="Не удалось завершить действие. Записи сохранены; попробуйте ещё раз вручную.";}String text=result;main.post(()->{busy=false;if(isDestroyed())return;send.setEnabled(true);reset.setEnabled(true);refresh();status.setText(text);});});}
    private void refresh(){DiagnosticJournal.drain();status.setText("Ожидают отправки: "+DiagnosticJournal.pending());events.setText(DiagnosticJournal.preview());}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}

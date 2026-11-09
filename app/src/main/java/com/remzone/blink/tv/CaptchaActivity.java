package com.remzone.blink.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.vk.id.captcha.api.VKCaptcha;
import com.vk.id.captcha.api.data.VKCaptchaResult;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

/** Private, short-lived UI; challenge URLs/results never enter intents or persistent storage. */
public final class CaptchaActivity extends Activity {
    private final Handler main=new Handler(Looper.getMainLooper());private String id="",url="",image="";private TextView message;private LinearLayout root;private boolean opening;
    @Override public void onCreate(Bundle state){super.onCreate(state);if(DeviceLayout.isTV(this))getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LinearLayout page=BlinkStyle.page(this,"Один небольшой шаг.","Пройди проверку TURN — Blink продолжит подключение.");root=BlinkStyle.card(this,page,"Авторизация VK");message=BlinkStyle.text(this,"",18,BlinkStyle.TEXT);root.addView(message);
        JSONObject challenge=TunnelService.captcha();id=challenge.optString("id");url=challenge.optString("url");image=challenge.optString("image");if(id.isEmpty()){message.setText("Проверка уже закрыта. Вернитесь и подключитесь заново.");button("Назад",this::finish);return;}
        message.setText("Ручная проверка TURN\nВо время проверки используется обычная сеть. После успешной проверки Blink продолжит подключение.");
        if(!url.isEmpty()){button("Открыть проверку TURN",this::openSDK);main.post(this::openSDK);}else legacyImage();
        button("Отменить подключение",this::cancel);main.postDelayed(expiry,1000);
    }
    private void button(String text,Runnable action){BlinkStyle.button(this,root,text,action);}
    private void openSDK(){if(opening||isFinishing())return;try{String origin=CaptchaPolicy.origin(url);opening=true;VKCaptcha.INSTANCE.openCaptcha(origin,url,result->runOnUiThread(()->{
            opening=false;if(isDestroyed())return;
            if(result instanceof VKCaptchaResult.Success){VKCaptchaResult.Success success=(VKCaptchaResult.Success)result;if(origin.equals(success.getDomain())&&TunnelService.answerCaptcha(id,"token",success.getToken())){finish();return;}message.setText("Проверка устарела. Подключитесь заново.");}
            else message.setText("Проверка TURN не завершена. Можно повторить её или отменить подключение.");
        }));}catch(Exception ex){opening=false;message.setText("Не удалось открыть проверку TURN. Проверьте WebView и обычное интернет-соединение.");}}
    private void legacyImage(){
        ImageView picture=new ImageView(this);picture.setAdjustViewBounds(true);root.addView(picture,new LinearLayout.LayoutParams(-1,-2));EditText answer=new EditText(this);BlinkStyle.input(this,answer);answer.setSingleLine(true);answer.setHint("Код с изображения");answer.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256)});root.addView(answer);button("Продолжить",()->{if(TunnelService.answerCaptcha(id,"key",answer.getText().toString().trim()))finish();else message.setText("Проверка устарела или код пуст. Подключитесь заново.");});
        new Thread(()->{HttpsURLConnection c=null;try{CaptchaPolicy.origin(image);c=(HttpsURLConnection)new URL(image).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);if(c.getResponseCode()!=200)throw new Exception();byte[] bytes;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>512*1024)throw new Exception();out.write(b,0,n);}bytes=out.toByteArray();}android.graphics.Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bitmap==null)throw new Exception();main.post(()->{if(!isDestroyed())picture.setImageBitmap(bitmap);});}catch(Exception ex){main.post(()->{if(!isDestroyed())message.setText("Не удалось загрузить изображение CAPTCHA. Проверьте сеть и повторите подключение.");});}finally{if(c!=null)c.disconnect();}},"Blink-Captcha-Image").start();
    }
    private final Runnable expiry=new Runnable(){@Override public void run(){JSONObject current=TunnelService.captcha();if(!id.equals(current.optString("id"))||current.optLong("expires_at")<=System.currentTimeMillis()/1000){if(opening)VKCaptcha.INSTANCE.closeCaptcha();finish();return;}main.postDelayed(this,1000);}};
    private void cancel(){if(opening)VKCaptcha.INSTANCE.closeCaptcha();startService(new Intent(this,TunnelService.class).setAction(TunnelService.STOP));finish();}
    @Override public void onBackPressed(){cancel();}
    @Override public void onDestroy(){main.removeCallbacks(expiry);super.onDestroy();}
}

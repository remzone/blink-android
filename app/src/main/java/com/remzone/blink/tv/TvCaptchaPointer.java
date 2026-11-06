package com.remzone.blink.tv;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

/** A human-controlled pointer inside our own VK SDK window; no global input injection. */
final class TvCaptchaPointer extends View {
    private final WebView web;private final Activity activity;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private float x,y;private boolean dragging;private long downTime;
    TvCaptchaPointer(Activity activity,WebView web){super(activity);this.activity=activity;this.web=web;setFocusable(true);setFocusableInTouchMode(true);}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){x=w/2f;y=h/2f;}
    private void send(int action){long now=SystemClock.uptimeMillis();if(action==MotionEvent.ACTION_DOWN)downTime=now;int[] a=new int[2],b=new int[2];getLocationOnScreen(a);web.getLocationOnScreen(b);MotionEvent e=MotionEvent.obtain(downTime,now,action,x+a[0]-b[0],y+a[1]-b[1],0);web.dispatchTouchEvent(e);e.recycle();}
    @Override public boolean onKeyDown(int key,KeyEvent event){
        float step=getResources().getDisplayMetrics().density*(event.getRepeatCount()>6?15:7);boolean moved=true;
        switch(key){case KeyEvent.KEYCODE_DPAD_LEFT:x-=step;break;case KeyEvent.KEYCODE_DPAD_RIGHT:x+=step;break;case KeyEvent.KEYCODE_DPAD_UP:y-=step;break;case KeyEvent.KEYCODE_DPAD_DOWN:y+=step;break;
            case KeyEvent.KEYCODE_DPAD_CENTER:case KeyEvent.KEYCODE_ENTER:if(event.getRepeatCount()==0){if(dragging){send(MotionEvent.ACTION_UP);dragging=false;}else{send(MotionEvent.ACTION_DOWN);send(MotionEvent.ACTION_UP);}}invalidate();return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:case KeyEvent.KEYCODE_MENU:if(event.getRepeatCount()==0){if(dragging)send(MotionEvent.ACTION_UP);else send(MotionEvent.ACTION_DOWN);dragging=!dragging;}invalidate();return true;
            case KeyEvent.KEYCODE_BACK:if(dragging){send(MotionEvent.ACTION_CANCEL);dragging=false;}activity.onBackPressed();return true;
            default:moved=false;
        }
        if(moved){x=Math.max(2,Math.min(getWidth()-2,x));y=Math.max(2,Math.min(getHeight()-2,y));if(dragging)send(MotionEvent.ACTION_MOVE);invalidate();return true;}return super.onKeyDown(key,event);
    }
    @Override protected void onDetachedFromWindow(){if(dragging)send(MotionEvent.ACTION_CANCEL);super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float density=getResources().getDisplayMetrics().density;paint.setColor(dragging?0xffffb74d:0xff00bfa5);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3*density);c.drawCircle(x,y,10*density,paint);c.drawLine(x-15*density,y,x+15*density,y,paint);c.drawLine(x,y-15*density,x,y+15*density,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(0xdd0d1520);c.drawRect(0,getHeight()-45*density,getWidth(),getHeight(),paint);paint.setColor(Color.WHITE);paint.setTextSize(13*density);c.drawText("Стрелки — курсор · OK — нажать · ▶ / Меню — удерживать / отпустить · Назад — выйти",15*density,getHeight()-17*density,paint);}
}

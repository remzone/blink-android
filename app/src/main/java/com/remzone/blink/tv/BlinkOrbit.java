package com.remzone.blink.tv;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Decorative state illustration; the real action remains an accessible Button. */
final class BlinkOrbit extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private String state="stopped";private float turn;private ValueAnimator animator;
    BlinkOrbit(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void state(String value){if(state.equals(value))return;state=value;updateMotion();invalidate();}
    private void updateMotion(){if(animator!=null){animator.cancel();animator=null;}if(isAttachedToWindow()&&getWindowVisibility()==VISIBLE&&"connecting".equals(state)&&BlinkStyle.motion()){animator=ValueAnimator.ofFloat(0,360);animator.setDuration(2400);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new android.view.animation.LinearInterpolator());animator.addUpdateListener(v->{turn=(float)v.getAnimatedValue();invalidate();});animator.start();}}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();updateMotion();}
    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int v){super.onWindowVisibilityChanged(v);updateMotion();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.82f;int tint="ready".equals(state)?BlinkStyle.ACCENT:BlinkStyle.BLUE;
        paint.setStyle(Paint.Style.FILL);paint.setShader(new RadialGradient(x,y,r,new int[]{0x50407d88,0x00131d2c},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r,paint);paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f*getResources().getDisplayMetrics().density);paint.setColor(0xff314459);c.drawCircle(x,y,r*.88f,paint);paint.setColor(0xff26374a);c.drawCircle(x,y,r,paint);paint.setColor(tint);paint.setStrokeCap(Paint.Cap.ROUND);c.drawArc(x-r*.88f,y-r*.88f,x+r*.88f,y+r*.88f,-85+turn,"connecting".equals(state)?85:235,false,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(0xff1b3043);c.drawCircle(x,y,r*.65f,paint);BlinkIcon icon=new BlinkIcon("ready".equals(state)?"shield":"power",tint);int s=(int)(r*.66);icon.setBounds((int)x-s/2,(int)y-s/2,(int)x+s/2,(int)y+s/2);icon.draw(c);
    }
}

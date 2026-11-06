package com.remzone.blink.tv;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original, resolution-independent line icons. No emoji or font dependencies. */
final class BlinkIcon extends Drawable {
    private final String name; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final int color;
    BlinkIcon(String name,int color){this.name=name;this.color=color;}
    @Override public void draw(Canvas canvas){Rect b=getBounds();int save=canvas.save();canvas.translate(b.left,b.top);canvas.scale(b.width()/24f,b.height()/24f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Path s=new Path();
        switch(name){
        case "power":canvas.drawArc(4,4,20,20,-45,270,false,p);canvas.drawLine(12,2,12,11,p);break;
        case "globe":canvas.drawCircle(12,12,9,p);canvas.drawOval(8,3,16,21,p);canvas.drawLine(3,12,21,12,p);canvas.drawArc(4,5,20,15,15,150,false,p);break;
        case "pulse":s.moveTo(2,12);s.lineTo(7,12);s.lineTo(10,5);s.lineTo(14,19);s.lineTo(17,12);s.lineTo(22,12);canvas.drawPath(s,p);break;
        case "plus":canvas.drawLine(12,5,12,19,p);canvas.drawLine(5,12,19,12,p);break;
        case "refresh":canvas.drawArc(4,4,20,20,30,300,false,p);s.moveTo(20,3);s.lineTo(20,9);s.lineTo(14,9);canvas.drawPath(s,p);break;
        case "folder":s.moveTo(3,6);s.lineTo(9,6);s.lineTo(11,8);s.lineTo(21,8);s.lineTo(21,20);s.lineTo(3,20);s.close();canvas.drawPath(s,p);break;
        case "trash":canvas.drawLine(4,6,20,6,p);canvas.drawLine(9,3,15,3,p);s.moveTo(6,6);s.lineTo(7,21);s.lineTo(17,21);s.lineTo(18,6);canvas.drawPath(s,p);canvas.drawLine(10,10,10,17,p);canvas.drawLine(14,10,14,17,p);break;
        case "back":s.moveTo(14,5);s.lineTo(7,12);s.lineTo(14,19);canvas.drawPath(s,p);break;
        case "wifi":canvas.drawArc(2,5,22,22,225,90,false,p);canvas.drawArc(6,10,18,22,225,90,false,p);canvas.drawCircle(12,19,1,p);break;
        case "play":s.moveTo(9,6);s.lineTo(19,12);s.lineTo(9,18);s.close();canvas.drawPath(s,p);break;
        case "chevron":s.moveTo(9,6);s.lineTo(15,12);s.lineTo(9,18);canvas.drawPath(s,p);break;
        case "shield":s.moveTo(12,2);s.lineTo(21,6);s.lineTo(20,14);s.quadTo(18,20,12,23);s.quadTo(6,20,4,14);s.lineTo(3,6);s.close();canvas.drawPath(s,p);s.reset();s.moveTo(8,12);s.lineTo(11,15);s.lineTo(17,9);canvas.drawPath(s,p);break;
        default:canvas.drawRoundRect(3,4,21,20,4,4,p);canvas.drawLine(7,9,17,9,p);canvas.drawLine(7,15,14,15,p);
        }canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha){p.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}

package com.remzone.blink.tv;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.widget.*;

/** Shared cards, typography and visible remote focus for every client screen. */
final class BlinkStyle {
    static final int BACKGROUND=0xff0d1520, CARD=0xff182535, TEXT=0xffe3ebf5, MUTED=0xff9eafc3, ACCENT=0xff65dfc3;
    static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    static LinearLayout column(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);return l;}
    static TextView text(Activity a,String value,int size,int color){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(a,3),0,dp(a,7));return t;}
    static LinearLayout page(Activity a,String title,String subtitle){
        if(DeviceLayout.isTV(a))a.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setBackgroundColor(BACKGROUND);LinearLayout root=column(a);int pad=dp(a,DeviceLayout.isTV(a)?40:16);root.setPadding(pad,dp(a,25),pad,dp(a,35));scroll.addView(root);
        TextView heading=text(a,title,30,Color.WHITE);heading.setTypeface(null,Typeface.BOLD);root.addView(heading);root.addView(text(a,subtitle,15,MUTED));a.setContentView(scroll);return root;
    }
    static LinearLayout card(Activity a,LinearLayout parent,String title){LinearLayout l=column(a);l.setPadding(dp(a,22),dp(a,18),dp(a,22),dp(a,18));GradientDrawable bg=new GradientDrawable();bg.setColor(CARD);bg.setCornerRadius(dp(a,14));l.setBackground(bg);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(a,16);parent.addView(l,p);if(!title.isEmpty()){TextView h=text(a,title,21,TEXT);h.setTypeface(null,Typeface.BOLD);l.addView(h);}return l;}
    static Button button(Activity a,LinearLayout parent,String label,Runnable action){Button b=new Button(a);b.setText(label);b.setTextSize(15);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);b.setPadding(dp(a,16),dp(a,10),dp(a,16),dp(a,10));b.setMinHeight(dp(a,48));b.setSingleLine(false);
        StateListDrawable states=new StateListDrawable();GradientDrawable focus=new GradientDrawable();focus.setColor(0xff286861);focus.setCornerRadius(dp(a,9));focus.setStroke(dp(a,2),0xff91f2dc);GradientDrawable plain=new GradientDrawable();plain.setColor(0xff25394e);plain.setCornerRadius(dp(a,9));states.addState(new int[]{android.R.attr.state_focused},focus);states.addState(new int[]{android.R.attr.state_pressed},focus);states.addState(new int[]{},plain);b.setBackground(states);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(a,8);parent.addView(b,p);b.setOnClickListener(v->action.run());return b;}
}

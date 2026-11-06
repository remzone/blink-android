package com.remzone.blink.tv;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.os.Build;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;

/** Shared adaptive visual language: quiet surfaces, original icons and remote focus. */
final class BlinkStyle {
    static final int BACKGROUND=0xff080d18,CARD=0xff131d2c,TEXT=0xfff2f6ff,MUTED=0xffa6b3ca,ACCENT=0xff93e8d1,BLUE=0xffa8c7ff;
    static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    static boolean motion(){return ValueAnimator.areAnimatorsEnabled();}
    static LinearLayout column(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);l.setClipChildren(false);l.setClipToPadding(false);return l;}
    static TextView text(Activity a,String value,int size,int color){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setLineSpacing(dp(a,2),1);t.setPadding(0,dp(a,3),0,dp(a,7));return t;}
    static GradientDrawable surface(int fill,int stroke,int radius,Activity a){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(a,radius));d.setStroke(dp(a,1),stroke);return d;}
    static LinearLayout page(Activity a,String title,String subtitle){
        if(DeviceLayout.isTV(a))a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        a.getWindow().setStatusBarColor(BACKGROUND);a.getWindow().setNavigationBarColor(BACKGROUND);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff17283a,BACKGROUND,0xff111629}));
        LinearLayout outer=column(a);outer.setGravity(Gravity.CENTER_HORIZONTAL);scroll.addView(outer);LinearLayout root=column(a);int width=a.getResources().getConfiguration().screenWidthDp;int pad=dp(a,width<500?16:DeviceLayout.isTV(a)?40:24);root.setPadding(pad,dp(a,DeviceLayout.isTV(a)?24:20),pad,dp(a,40));outer.addView(root,new LinearLayout.LayoutParams(Math.min(a.getResources().getDisplayMetrics().widthPixels,dp(a,1260)),-2));
        LinearLayout brand=new LinearLayout(a);brand.setGravity(Gravity.CENTER_VERTICAL);ImageView logo=new ImageView(a);logo.setImageDrawable(new BlinkIcon("shield",ACCENT));logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);brand.addView(logo,new LinearLayout.LayoutParams(dp(a,24),dp(a,24)));TextView word=text(a,"  B L I N K",13,TEXT);word.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));brand.addView(word);root.addView(brand);
        String clean=title.replace("BLINK  /  ","");TextView heading=text(a,clean,width<360?28:34,TEXT);heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));heading.setPadding(0,dp(a,20),0,dp(a,4));root.addView(heading);root.addView(text(a,subtitle,14,MUTED));a.setContentView(scroll);return root;
    }
    static void reveal(View v){if(!motion())return;v.setAlpha(0);v.setTranslationY(16*v.getResources().getDisplayMetrics().density);v.animate().alpha(1).translationY(0).setDuration(360).setInterpolator(new DecelerateInterpolator()).start();}
    static LinearLayout card(Activity a,LinearLayout parent,String title){LinearLayout l=column(a);l.setPadding(dp(a,20),dp(a,18),dp(a,20),dp(a,20));l.setBackground(surface(CARD,0xff2a3548,24,a));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(a,16);parent.addView(l,p);if(!title.isEmpty()){TextView h=text(a,title,18,TEXT);h.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));l.addView(h);}reveal(l);return l;}
    static String iconFor(String label){String s=label.toLowerCase(java.util.Locale.ROOT);if(s.contains("отключ")||s.contains("подключить туннель"))return "power";if(s.contains("удал")||s.contains("забыть"))return "trash";if(s.contains("добав")||s.contains("замен"))return "plus";if(s.contains("обнов"))return "refresh";if(s.contains("назад"))return "back";if(s.contains("youtube"))return "play";if(s.contains("google")||s.contains("интернет")||s.contains("диагност")||s.contains("связь"))return "pulse";if(s.contains("wi-fi")||s.contains("сеть"))return "wifi";if(s.contains("профил")||s.contains("ip")||s.contains("сервер"))return "globe";if(s.contains("файл"))return "folder";return "shield";}
    static void decorate(Activity a,Button b,String icon,boolean prominent){
        b.setAllCaps(false);b.setTextSize(14);b.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{MUTED,prominent?BACKGROUND:TEXT}));b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);b.setPadding(dp(a,15),dp(a,13),dp(a,15),dp(a,13));b.setMinHeight(dp(a,52));b.setSingleLine(false);b.setElevation(0);
        StateListDrawable states=new StateListDrawable();states.addState(new int[]{-android.R.attr.state_enabled},surface(0xff192231,0xff2a3548,15,a));states.addState(new int[]{android.R.attr.state_focused},surface(prominent?0xffc9fff0:0xff263e50,ACCENT,15,a));states.addState(new int[]{android.R.attr.state_pressed},surface(prominent?0xffc9fff0:0xff263e50,ACCENT,15,a));states.addState(new int[]{},surface(prominent?ACCENT:0xff1c293b,prominent?ACCENT:0xff304057,15,a));b.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x3093e8d1),states,null));
        BlinkIcon d=new BlinkIcon(icon,prominent?BACKGROUND:ACCENT);d.setBounds(0,0,dp(a,21),dp(a,21));b.setCompoundDrawablesRelative(d,null,null,null);b.setCompoundDrawablePadding(dp(a,12));b.setOnFocusChangeListener((v,focus)->{v.animate().cancel();if(motion())v.animate().scaleX(focus?1.018f:1).scaleY(focus?1.018f:1).setDuration(180).start();else{v.setScaleX(1);v.setScaleY(1);}});
    }
    static Button button(Activity a,LinearLayout parent,String label,Runnable action){Button b=new Button(a);b.setText(label);decorate(a,b,iconFor(label),false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(a,9);parent.addView(b,p);b.setOnClickListener(v->action.run());return b;}
    static void input(Activity a,EditText input){input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setTextSize(15);input.setPadding(dp(a,14),dp(a,12),dp(a,14),dp(a,12));input.setMinHeight(dp(a,52));input.setBackground(surface(0xff0e1725,0xff3a5069,14,a));}
}

package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Локальная строка Watch Next: запрос при возврате Home и изменении списка, без сети и опроса. */
final class WatchNextRow {
    private static final Uri URI = Uri.parse("content://android.media.tv/watch_next_program");
    private static final String[] COLUMNS = {"package_name", "title", "episode_title", "intent_uri",
            "watch_next_type", "last_engagement_time_utc_millis", "last_playback_position_millis", "duration_millis", "poster_art_uri"};
    private final Activity activity;
    private final boolean recommendations;
    private final LinearLayout panel, cards;
    private final HorizontalScrollView scroll;
    private final Runnable up, down, onFocus;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<View> buttons = new ArrayList<>();
    private final PosterLoader posters;
    private int generation, selected;
    private volatile boolean active;
    private boolean visible = true, watching;
    private final ContentObserver observer = new ContentObserver(main) {
        @Override public void onChange(boolean self) { refresh(); }
    };
    private static final class Entry {
        String pkg, title, episode, link, poster, group; long engaged, position, duration; boolean history;
    }
    WatchNextRow(Activity a, FrameLayout root, Runnable up, Runnable down, Runnable onFocus) {
        this(a, root, up, down, onFocus, false);
    }
    WatchNextRow(Activity a, FrameLayout root, Runnable up, Runnable down, Runnable onFocus, boolean recommendations) {
        this.recommendations = recommendations;
        activity = a; this.up = up; this.down = down; this.onFocus = onFocus; posters = new PosterLoader(a);
        panel = new LinearLayout(a); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setVisibility(View.GONE); panel.setClipChildren(false);
        FrameLayout.LayoutParams frame = new FrameLayout.LayoutParams(-1, -2);
        frame.leftMargin = dp(recommendations ? 12 : 62); frame.rightMargin = dp(recommendations ? 12 : 62); frame.topMargin = dp(recommendations ? 16 : 140);
        if (recommendations) frame.bottomMargin = dp(28);
        root.addView(panel, frame);
        TextView heading = text(recommendations ? "Новинки и рекомендации" : "Продолжить просмотр", 17, Color.WHITE);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        panel.addView(heading);
        scroll = new HorizontalScrollView(a); scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipChildren(false); scroll.setClipToPadding(false); scroll.setFillViewport(false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(126)); sp.topMargin = dp(8);
        panel.addView(scroll, sp);
        cards = new LinearLayout(a); cards.setOrientation(LinearLayout.HORIZONTAL);
        cards.setPadding(dp(3), dp(3), dp(3), dp(3)); cards.setClipChildren(false);
        scroll.addView(cards, new HorizontalScrollView.LayoutParams(-2, -1));
    }
    private int dp(float n) { return Math.round(n * activity.getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(activity); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t;
    }
    void start() {
        active = true; posters.start();
        try { activity.getContentResolver().registerContentObserver(recommendations ? Uri.parse("content://android.media.tv/preview_program") : URI, true, observer); watching = true; }
        catch (SecurityException ignored) { }
        refresh();
    }
    void stop() {
        active = false; generation++; posters.pause();
        if (watching) { activity.getContentResolver().unregisterContentObserver(observer); watching = false; }
    }
    void close() { stop(); io.shutdownNow(); posters.close(); }
    void setVisible(boolean value) { visible = value; panel.setVisibility(visible && !buttons.isEmpty() ? View.VISIBLE : View.GONE); }
    boolean focusLast() {
        if (buttons.isEmpty() || !visible) return false;
        return buttons.get(Math.min(selected, buttons.size() - 1)).requestFocus();
    }
    void refresh() {
        if (!active) return;
        int request = ++generation;
        io.execute(() -> {
            List<Entry> entries = new ArrayList<>();
            if (!recommendations) try (Cursor c = activity.getContentResolver().query(URI, COLUMNS, null, null, null)) {
                if (c != null) while (c.moveToNext()) {
                    Entry e = new Entry(); e.pkg = c.getString(0); e.title = c.getString(1);
                    e.episode = c.getString(2); e.link = c.getString(3);
                    e.engaged = c.getLong(5); e.position = c.getLong(6); e.duration = c.getLong(7); e.poster = c.getString(8);
                    if (ContinueRules.include(c.getInt(4), e.position, e.duration, e.title, e.link)) entries.add(e);
                }
            } catch (Exception error) { Log.w("TvHomeContinue", "История недоступна: " + error.getClass().getSimpleName()); }
            if (!recommendations) try {
                Set<String> historyChannels = new HashSet<>();
                try (Cursor channels = activity.getContentResolver().query(Uri.parse("content://android.media.tv/channel"), new String[]{"_id", "package_name", "display_name"}, null, null, null)) {
                    if (channels != null) while (channels.moveToNext()) {
                        String name = channels.getString(2);
                        if ("com.lazycatsoftware.lmd".equals(channels.getString(1)) && name != null && name.toLowerCase(java.util.Locale.ROOT).contains("история")) historyChannels.add(channels.getString(0));
                    }
                }
                if (!historyChannels.isEmpty()) try (Cursor c = activity.getContentResolver().query(Uri.parse("content://android.media.tv/preview_program"), new String[]{"package_name", "channel_id", "title", "intent_uri", "poster_art_uri", "last_playback_position_millis", "duration_millis", "weight"}, null, null, null)) {
                    if (c != null) while (c.moveToNext()) {
                        if (!"com.lazycatsoftware.lmd".equals(c.getString(0)) || !historyChannels.contains(c.getString(1))) continue;
                        Entry e = new Entry(); e.pkg = c.getString(0); e.title = c.getString(2); e.link = c.getString(3); e.poster = c.getString(4);
                        e.position = c.getLong(5); e.duration = c.getLong(6); e.engaged = c.getLong(7); e.history = true;
                        if (ContinueRules.include(1, e.position, e.duration, e.title, e.link)) entries.add(e);
                    }
                }
            } catch (Exception error) { Log.w("TvHomeContinue", "История приложения недоступна: " + error.getClass().getSimpleName()); }
            if (recommendations) readRecommendations(entries);
            entries.sort((a, b) -> Long.compare(b.engaged, a.engaged));
            main.post(() -> { if (active && request == generation && !activity.isDestroyed()) show(entries); });
        });
    }
    private GradientDrawable background(int color, boolean focused) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(12));
        d.setStroke(dp(focused ? 2 : 1), focused ? Color.WHITE : 0x44ffffff); return d;
    }
    private void show(List<Entry> entries) {
        // Не перестраивать сфокусированную строку из-за фонового изменения списка.
        if (cards.hasFocus()) return;
        cards.removeAllViews(); buttons.clear(); Set<String> seen = new HashSet<>();
        for (Entry e : ContinueRules.balance(entries, entry -> recommendations ? entry.group : entry.pkg)) {
            if (buttons.size() >= (recommendations ? 24 : 12)) break;
            if (!seen.add(e.pkg + "\n" + e.link)) continue;
            Intent target = target(e); if (target == null) continue;
            int index = buttons.size();
            LinearLayout card = new LinearLayout(activity); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(13), dp(10), dp(13), dp(10)); card.setFocusable(true); card.setClickable(true);
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_focused}, background(0xef35445b, true));
            states.addState(new int[]{}, background(0xd9101620, false)); card.setBackground(states);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(195), -1); cp.rightMargin = dp(12);
            cards.addView(card, cp); buttons.add(card);
            String app;
            try { app = String.valueOf(activity.getPackageManager().getApplicationLabel(activity.getPackageManager().getApplicationInfo(e.pkg, 0))); }
            catch (Exception ignored) { app = e.pkg; }
            if ("com.yandex.tv.home".equals(e.pkg)) app = "Яндекс ТВ";
            if ("com.lazycatsoftware.lmd".equals(e.pkg)) app = "Ленивая Медиатека";
            TextView source = text(app, 10, 0xffa6d9ff); source.setMaxLines(1); source.setEllipsize(TextUtils.TruncateAt.END); card.addView(source);
            TextView title = text(e.title, 15, Color.WHITE); title.setMaxLines(2); title.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, 0, 1); tp.topMargin = dp(3); card.addView(title, tp);
            String detail = e.history ? "История приложения" : e.episode == null ? "" : e.episode;
            if (e.duration > 0) detail = (detail.isEmpty() ? "" : detail + " · ") + e.position / 60000 + " / " + e.duration / 60000 + " мин";
            TextView subtitle = text(detail, 10, 0xffced5df); subtitle.setMaxLines(1); subtitle.setEllipsize(TextUtils.TruncateAt.END); card.addView(subtitle);
            ProgressBar progress = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100); progress.setProgress(ContinueRules.percent(e.position, e.duration));
            if (e.duration <= 0) progress.setVisibility(View.GONE);
            progress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff7dcfff));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(3)); pp.topMargin = dp(6); card.addView(progress, pp);
            int imageGeneration = generation;
            posters.load(e.poster, bitmap -> {
                if (active && imageGeneration == generation && card.getParent() == cards) card.setBackground(new PosterBackdrop(bitmap, activity.getResources().getDisplayMetrics().density));
            });
            card.setContentDescription(e.title + ", " + app + ", " + detail);
            card.setOnClickListener(v -> {
                try { activity.startActivity(new Intent(target)); }
                catch (Exception error) { Toast.makeText(activity, "Приложение не смогло открыть этот просмотр", Toast.LENGTH_LONG).show(); }
            });
            card.setOnFocusChangeListener((v, focused) -> {
                if (focused) { selected = index; onFocus.run(); scroll.smoothScrollTo(Math.max(0, card.getLeft() - dp(3)), 0); }
            });
            card.setOnKeyListener((v, key, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (key == KeyEvent.KEYCODE_DPAD_UP) { up.run(); return true; }
                if (key == KeyEvent.KEYCODE_BACK) { if (recommendations) up.run(); else down.run(); return true; }
                if (key == KeyEvent.KEYCODE_DPAD_DOWN) { down.run(); return true; }
                if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    int next = selected + (key == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                    if (next >= 0 && next < buttons.size()) buttons.get(next).requestFocus(); return true;
                }
                return false;
            });
        }
        panel.setVisibility(visible && !buttons.isEmpty() ? View.VISIBLE : View.GONE);
        Log.i("TvHomeContinue", (recommendations ? "Рекомендаций: " : "Карточек истории: ") + buttons.size());
    }
    private void readRecommendations(List<Entry> entries) {
        java.util.Map<String, String> channels = new java.util.LinkedHashMap<>();
        try {
            try (Cursor c = activity.getContentResolver().query(Uri.parse("content://android.media.tv/channel"), new String[]{"_id", "display_name"}, null, null, null)) {
                if (c != null) while (c.moveToNext()) {
                    String name = c.getString(1);
                    if (RecommendationRules.channel(name)) channels.put(c.getString(0), name);
                }
            }
            try (Cursor c = activity.getContentResolver().query(Uri.parse("content://android.media.tv/preview_program"), new String[]{"package_name", "channel_id", "title", "intent_uri", "poster_art_uri", "weight"}, null, null, null)) {
                if (c != null) while (c.moveToNext()) {
                    String channel = c.getString(1), title = c.getString(2), link = c.getString(3);
                    if (!channels.containsKey(channel) || !RecommendationRules.program(title, link)) continue;
                    Entry e = new Entry(); e.pkg = c.getString(0); e.group = channel; e.title = title; e.link = link;
                    e.poster = c.getString(4); e.episode = channels.get(channel); e.engaged = c.getLong(5);
                    entries.add(e);
                }
            }
        } catch (Exception error) { Log.w("TvHomeContinue", "Рекомендации недоступны: " + error.getClass().getSimpleName()); }
    }
    private Intent target(Entry e) {
        try {
            Intent i = Intent.parseUri(e.link, Intent.URI_INTENT_SCHEME);
            if (i.getComponent() != null && !e.pkg.equals(i.getComponent().getPackageName())) return null;
            i.setSelector(null); i.setClipData(null); i.setPackage(e.pkg);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return i.resolveActivity(activity.getPackageManager()) == null ? null : i;
        } catch (Exception ignored) { return null; }
    }
}

package com.gathin.tvhome;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/** Погодная строка под часами; сеть и кэш находятся во вспомогательном модуле. */
final class HomeWeatherLine {
    private final Activity activity;
    private final TextView text;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private int generation;
    HomeWeatherLine(Activity a, LinearLayout header) {
        activity = a; text = new TextView(a); text.setTextSize(13); text.setTextColor(0xffeeeeee);
        text.setSingleLine(true); text.setEllipsize(TextUtils.TruncateAt.END);
        text.setText("Жигулёвск · погода…");
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.leftMargin = Math.round(4 * a.getResources().getDisplayMetrics().density);
        p.topMargin = Math.round(7 * a.getResources().getDisplayMetrics().density);
        header.addView(text, p);
    }
    void start() {
        int request = ++generation;
        io.execute(() -> {
            String label = "Жигулёвск · погода недоступна", description = label;
            try (Cursor c = activity.getContentResolver().query(Uri.parse("content://io.github.sumyabazar1.weather.home/weather"), null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int temp = c.getInt(1); long updated = c.getLong(3);
                    if (System.currentTimeMillis() - updated < 2 * 60 * 60_000L) {
                        label = c.getString(0) + " · " + (temp > 0 ? "+" : "") + temp + "° · " + c.getString(2);
                        if (System.currentTimeMillis() - updated > 35 * 60_000L) label += " · ранее";
                        description = label + ", источник: " + c.getString(4);
                    }
                }
            } catch (Exception error) { android.util.Log.w("TvHomeWeather", "Строка погоды: " + error.getClass().getSimpleName(), error); }
            String shown = label, spoken = description;
            main.post(() -> { if (request == generation && !activity.isDestroyed()) { text.setText(shown); text.setContentDescription(spoken); } });
        });
    }
    void stop() { generation++; }
    void close() { stop(); io.shutdownNow(); }
}

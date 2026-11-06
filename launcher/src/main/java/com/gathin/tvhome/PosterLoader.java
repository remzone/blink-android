package com.gathin.tvhome;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
/** Превью через закрытый ContentProvider: сам Home не имеет INTERNET. */
final class PosterLoader {
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor io = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    private volatile boolean active;
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getAllocationByteCount(); }
    };
    PosterLoader(Context c) { context = c.getApplicationContext(); }
    void start() { active = true; }
    void pause() { active = false; io.getQueue().clear(); }
    void close() { pause(); io.shutdownNow(); cache.evictAll(); }
    void load(String address, Consumer<Bitmap> result) {
        if (address == null || address.isEmpty() || !active) return;
        Bitmap found = cache.get(address); if (found != null) { result.accept(found); return; }
        io.execute(() -> {
            if (!active) return;
            try {
                Uri uri = Uri.parse(address);
                if ("http".equals(uri.getScheme())) uri = uri.buildUpon().scheme("https").build();
                if ("https".equals(uri.getScheme())) uri = new Uri.Builder().scheme("content").authority("io.github.sumyabazar1.weather.home").appendPath("poster").appendQueryParameter("url", uri.toString()).build();
                if (!"content".equals(uri.getScheme()) && !"android.resource".equals(uri.getScheme())) return;
                BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
                try (InputStream input = context.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(input, null, options); }
                if (options.outWidth <= 0 || options.outHeight <= 0 || (long) options.outWidth * options.outHeight > 36_000_000L) return;
                options.inSampleSize = 1;
                while (options.outWidth / options.inSampleSize > 600 || options.outHeight / options.inSampleSize > 400) options.inSampleSize *= 2;
                options.inJustDecodeBounds = false; options.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap bitmap;
                try (InputStream input = context.getContentResolver().openInputStream(uri)) { bitmap = BitmapFactory.decodeStream(input, null, options); }
                if (bitmap == null) return;
                cache.put(address, bitmap);
                main.post(() -> { if (active) result.accept(bitmap); });
            } catch (Exception ignored) { }
        });
    }
}

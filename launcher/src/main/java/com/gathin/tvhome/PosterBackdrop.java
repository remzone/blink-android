package com.gathin.tvhome;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
/** Постер с кадрированием и затемнением для читаемого названия, без размытия. */
final class PosterBackdrop extends Drawable {
    private final Bitmap bitmap;
    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private boolean focused;
    PosterBackdrop(Bitmap bitmap, float density) { this.bitmap = bitmap; this.density = density; }
    @Override public boolean isStateful() { return true; }
    @Override protected boolean onStateChange(int[] state) {
        boolean value = false; for (int s : state) if (s == android.R.attr.state_focused) value = true;
        if (focused == value) return false; focused = value; invalidateSelf(); return true;
    }
    @Override public void draw(Canvas canvas) {
        RectF r = new RectF(getBounds()); if (r.isEmpty()) return;
        BitmapShader image = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        float scale = Math.max(r.width() / bitmap.getWidth(), r.height() / bitmap.getHeight());
        Matrix matrix = new Matrix(); matrix.setScale(scale, scale);
        matrix.postTranslate(r.left + (r.width() - bitmap.getWidth() * scale) / 2, r.top + (r.height() - bitmap.getHeight() * scale) / 2); image.setLocalMatrix(matrix);
        paint.setStyle(Paint.Style.FILL); paint.setShader(image); canvas.drawRoundRect(r, 12 * density, 12 * density, paint);
        paint.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0x99050a10, 0xef050a10, Shader.TileMode.CLAMP)); canvas.drawRoundRect(r, 12 * density, 12 * density, paint);
        paint.setShader(null); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth((focused ? 2 : 1) * density); paint.setColor(focused ? Color.WHITE : 0x66ffffff);
        r.inset(density, density); canvas.drawRoundRect(r, 11 * density, 11 * density, paint);
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @SuppressWarnings("deprecation") @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

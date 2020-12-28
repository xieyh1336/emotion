package com.example.emotionkeyboardbuilder.util.gif;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.style.DynamicDrawableSpan;

public class AnimatedImageSpan extends DynamicDrawableSpan {

    private Drawable mDrawable;

    public AnimatedImageSpan(Drawable d) {
        super();
        mDrawable = d;
        // Use handler for 'ticks' to proceed to next frame 
        final Handler mHandler = new Handler();
        mHandler.post(new Runnable() {
            public void run() {
                ((AnimatedGifDrawable)mDrawable).nextFrame();

                //根据此帧的持续时间设置下一个延迟
//                int i = ((AnimatedGifDrawable) mDrawable).getFrameDuration();
//                mHandler.postDelayed(this, ((AnimatedGifDrawable)mDrawable).getFrameDuration());
                //定死0.1s刷新一次
                mHandler.postDelayed(this, 100);
            }
        });
    }

    /**
     * 从可绘制动画返回当前帧,还可以替代super.getCachedDrawable（），
     * 因为我们无法缓存动画图像的“图像”。
     */
    @Override
    public Drawable getDrawable() {
        return ((AnimatedGifDrawable)mDrawable).getDrawable();
    }

    /**
     * 使用getDrawable（）来获取图像/帧以计算尺寸，而不是缓存的drawable。
     */
    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        Drawable d = getDrawable();
        Rect rect = d.getBounds();

        if (fm != null) {
            fm.ascent = -rect.bottom; 
            fm.descent = 0; 

            fm.top = fm.ascent;
            fm.bottom = 0;
        }

        return rect.right;
    }

    /**
     * 使用getDrawable（）来获取要绘制的图像/帧，而不是缓存的drawable。
     */
    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        Drawable b = getDrawable();
        canvas.save();
        int transY = bottom - b.getBounds().bottom;
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY -= paint.getFontMetricsInt().descent;
        }
        canvas.translate(x, transY);
        b.draw(canvas);
        canvas.restore();
    }
}


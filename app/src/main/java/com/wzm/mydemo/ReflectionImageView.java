package com.wzm.mydemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * 自定义View（倒影）
 */
public class ReflectionImageView extends View {

    private Bitmap mSourceBitmap;//被反射的源位图
    private final Paint mReflectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);//初始化一个反射绘制画笔Paint，并传入抗锯齿和位图过滤标志位
    private final Paint mGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);//初始化一个渐变遮罩画笔，并传入抗锯齿标志位

    private static final float REFLECTION_RATIO = 0.45f;// 倒影高度 = 源高度的 45%
    private static final int GAP_DP = 2;// 源图与倒影之间的间距

    public ReflectionImageView(Context context) {
        super(context);
    }

    public ReflectionImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReflectionImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    // 设置要反射的源 View
    public void setSourceView(View view) {
        if (view.getWidth() > 0 && view.getHeight() > 0) {
            captureView(view);
        } else {
            view.post(() -> {
                if (view.getWidth() > 0 && view.getHeight() > 0) {
                    captureView(view);
                }
            });
        }
    }

    // 将源 View 绘制到一个 Bitmap 中
    private void captureView(View view) {
        mSourceBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(mSourceBitmap);// 创建画布c绑定到位图
        view.draw(c);
        requestLayout();
        invalidate();
    }

    // 测量此 View 的大小
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mSourceBitmap != null && mSourceBitmap.getWidth() > 0) {
            int w = mSourceBitmap.getWidth();
            int gap = dp(GAP_DP);
            int refH = (int) (mSourceBitmap.getHeight() * REFLECTION_RATIO);
            setMeasuredDimension(w, gap + refH);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    // 绘制倒影
    @Override
    protected void onDraw(Canvas canvas) {
        if (mSourceBitmap == null || getWidth() == 0) return;

        int srcW = mSourceBitmap.getWidth();
        int srcH = mSourceBitmap.getHeight();
        int gap = dp(GAP_DP);
        int refH = (int) (srcH * REFLECTION_RATIO);

        canvas.save();
        canvas.translate(0, gap + refH);
        canvas.scale(1f, -1f);
        mReflectionPaint.setAlpha(120);
        Rect src = new Rect(0, 0, srcW, srcH);
        Rect dst = new Rect(0, 0, srcW, refH);
        canvas.drawBitmap(mSourceBitmap, src, dst, mReflectionPaint);
        canvas.restore();

        LinearGradient gradient = new LinearGradient(
                0, gap, 0, gap + refH,
                0x001A1A2E, 0xFF1A1A2E, Shader.TileMode.CLAMP);
        mGradientPaint.setShader(gradient);
        canvas.drawRect(0, gap, srcW, gap + refH, mGradientPaint);
    }

    // 工具方法：dp 转像素
    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}

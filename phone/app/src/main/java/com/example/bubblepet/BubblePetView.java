package com.example.bubblepet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class BubblePetView extends View {

    private static final int PET_SIZE_DP = 90;
    private static final int WANDER_INTERVAL_MS = 5000;
    private static final int WANDER_DURATION_MS = 3000;
    private static final int SNAP_DURATION_MS = 400;
    private static final float BREATH_SCALE_MAX = 1.08f;
    private static final int CLICK_THRESHOLD_PX = 10;

    private final Paint fillPaint;
    private final Paint strokePaint;
    private final Paint shinePaint;
    private final Paint eyeWhitePaint;
    private final Paint eyePupilPaint;
    private final Paint blushPaint;
    private final Paint mouthPaint;

    private float petSizePx;
    private float breathScale = 1.0f;
    private ValueAnimator breathAnimator;
    private ValueAnimator wanderAnimator;
    private ValueAnimator snapAnimator;
    private Runnable wanderRunnable;
    private final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());

    private int screenWidth;
    private int screenHeight;
    private int bottomMargin = 0;
    private boolean isDragging = false;
    private float downRawX;
    private float downRawY;
    private boolean hasMoved;
    private int currentX;
    private int currentY;
    private String expression = "neutral";

    public interface OnPetClickListener {
        void onPetClick(BubblePetView view);
    }

    public interface OnPositionChangedListener {
        void onPositionChanged(int x, int y);
    }

    private OnPetClickListener clickListener;
    private OnPositionChangedListener positionListener;

    public BubblePetView(Context context) {
        this(context, null);
    }

    public BubblePetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        petSizePx = dpToPx(PET_SIZE_DP);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor("#6EC6FF"));
        fillPaint.setStyle(Paint.Style.FILL);

        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.parseColor("#2196F3"));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dpToPx(3));

        shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shinePaint.setColor(Color.parseColor("#B3E5FC"));
        shinePaint.setStyle(Paint.Style.FILL);

        eyeWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyeWhitePaint.setColor(Color.WHITE);

        eyePupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyePupilPaint.setColor(Color.parseColor("#333333"));

        blushPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blushPaint.setColor(Color.parseColor("#80FF8A95"));
        blushPaint.setStyle(Paint.Style.FILL);

        mouthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mouthPaint.setColor(Color.parseColor("#333333"));
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeWidth(dpToPx(2));
        mouthPaint.setStrokeCap(Paint.Cap.ROUND);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = (int) (petSizePx + dpToPx(10));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = petSizePx / 2f;

        canvas.save();
        canvas.scale(breathScale, breathScale, cx, cy);

        canvas.drawCircle(cx, cy, radius, fillPaint);
        canvas.drawCircle(cx, cy, radius, strokePaint);

        float shineX = cx - radius * 0.3f;
        float shineY = cy - radius * 0.3f;
        float shineRadius = radius * 0.2f;
        canvas.drawCircle(shineX, shineY, shineRadius, shinePaint);

        float eyeSize = radius * 0.14f;
        float pupilSize = radius * 0.08f;
        float eyeY = cy - radius * 0.1f;
        float leftEyeX = cx - radius * 0.25f;
        float rightEyeX = cx + radius * 0.25f;

        drawExpression(canvas, cx, cy, radius, leftEyeX, rightEyeX, eyeY,
                eyeSize, pupilSize);

        canvas.restore();
    }

    private void drawExpression(Canvas canvas, float cx, float cy, float radius,
                                float leftEyeX, float rightEyeX, float eyeY,
                                float eyeSize, float pupilSize) {
        switch (expression) {
            case "happy":
                drawHappyEyes(canvas, leftEyeX, rightEyeX, eyeY, eyeSize);
                drawSmile(canvas, cx, cy + radius * 0.25f, radius * 0.18f, true);
                break;
            case "excited":
                drawRoundEyes(canvas, leftEyeX, rightEyeX, eyeY, eyeSize * 1.3f, pupilSize * 1.3f, 0, -eyeSize * 0.2f);
                drawBlush(canvas, leftEyeX, rightEyeX, eyeY + eyeSize * 1.6f, eyeSize * 0.7f);
                drawOpenMouth(canvas, cx, cy + radius * 0.28f, radius * 0.1f);
                break;
            case "shy":
                drawRoundEyes(canvas, leftEyeX, rightEyeX, eyeY, eyeSize, pupilSize, 0, eyeSize * 0.4f);
                drawBlush(canvas, leftEyeX, rightEyeX, eyeY + eyeSize * 1.6f, eyeSize * 0.8f);
                drawSmile(canvas, cx, cy + radius * 0.25f, radius * 0.14f, true);
                break;
            case "sleepy":
                drawLineEyes(canvas, leftEyeX, rightEyeX, eyeY, eyeSize * 1.4f, eyeSize * 0.25f);
                drawMouthFlat(canvas, cx, cy + radius * 0.3f, radius * 0.12f);
                break;
            case "sad":
                drawSadBrows(canvas, leftEyeX, rightEyeX, eyeY - eyeSize * 1.8f, eyeSize);
                drawRoundEyes(canvas, leftEyeX, rightEyeX, eyeY, eyeSize * 0.9f, pupilSize, 0, eyeSize * 0.3f);
                drawSmile(canvas, cx, cy + radius * 0.3f, radius * 0.14f, false);
                break;
            case "proud":
                drawLineEyes(canvas, leftEyeX, rightEyeX, eyeY, eyeSize * 1.6f, eyeSize * 0.2f);
                drawSmirk(canvas, cx, cy + radius * 0.25f, radius * 0.16f);
                break;
            case "surprised":
                drawRoundEyes(canvas, leftEyeX, rightEyeX, eyeY, eyeSize * 1.5f, pupilSize * 1.2f, 0, 0);
                drawOpenMouth(canvas, cx, cy + radius * 0.3f, radius * 0.12f);
                break;
            case "thinking":
                drawRoundEyes(canvas, leftEyeX, rightEyeX, eyeY, eyeSize, pupilSize, 0, -eyeSize * 0.5f);
                drawMouthFlat(canvas, cx, cy + radius * 0.28f, radius * 0.1f);
                break;
            default: // neutral
                drawRoundEyes(canvas, leftEyeX, rightEyeX, eyeY, eyeSize, pupilSize, eyeSize * 0.15f, 0);
                break;
        }
    }

    private void drawRoundEyes(Canvas canvas, float leftX, float rightX, float eyeY,
                               float eyeSize, float pupilSize,
                               float pupilDx, float pupilDy) {
        canvas.drawCircle(leftX, eyeY, eyeSize, eyeWhitePaint);
        canvas.drawCircle(leftX + pupilDx, eyeY + pupilDy, pupilSize, eyePupilPaint);
        canvas.drawCircle(rightX, eyeY, eyeSize, eyeWhitePaint);
        canvas.drawCircle(rightX + pupilDx, eyeY + pupilDy, pupilSize, eyePupilPaint);
    }

    private void drawHappyEyes(Canvas canvas, float leftX, float rightX, float eyeY, float eyeSize) {
        // 向上弯的弧 = ^.^
        Path left = new Path();
        Path right = new Path();
        RectF leftRect = new RectF(leftX - eyeSize, eyeY - eyeSize,
                leftX + eyeSize, eyeY + eyeSize);
        RectF rightRect = new RectF(rightX - eyeSize, eyeY - eyeSize,
                rightX + eyeSize, eyeY + eyeSize);
        left.addArc(leftRect, 200, 140);
        right.addArc(rightRect, 200, 140);
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeWidth(dpToPx(2.5f));
        canvas.drawPath(left, mouthPaint);
        canvas.drawPath(right, mouthPaint);
    }

    private void drawLineEyes(Canvas canvas, float leftX, float rightX, float eyeY,
                              float halfW, float halfH) {
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeWidth(dpToPx(2.5f));
        canvas.drawRoundRect(new RectF(leftX - halfW, eyeY - halfH, leftX + halfW, eyeY + halfH),
                halfH, halfH, mouthPaint);
        canvas.drawRoundRect(new RectF(rightX - halfW, eyeY - halfH, rightX + halfW, eyeY + halfH),
                halfH, halfH, mouthPaint);
    }

    private void drawSadBrows(Canvas canvas, float leftX, float rightX, float browY, float eyeSize) {
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeWidth(dpToPx(2));
        // 左眉向右下倾斜 \(   右眉向左下倾斜 /
        Path lb = new Path();
        lb.moveTo(leftX - eyeSize, browY - eyeSize * 0.4f);
        lb.lineTo(leftX + eyeSize, browY + eyeSize * 0.4f);
        canvas.drawPath(lb, mouthPaint);
        Path rb = new Path();
        rb.moveTo(rightX - eyeSize, browY + eyeSize * 0.4f);
        rb.lineTo(rightX + eyeSize, browY - eyeSize * 0.4f);
        canvas.drawPath(rb, mouthPaint);
    }

    private void drawBlush(Canvas canvas, float leftX, float rightX, float y, float r) {
        canvas.drawCircle(leftX, y, r, blushPaint);
        canvas.drawCircle(rightX, y, r, blushPaint);
    }

    private void drawSmile(Canvas canvas, float cx, float cy, float halfW, boolean happy) {
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeWidth(dpToPx(2.5f));
        RectF rect = new RectF(cx - halfW, cy - halfW * 0.6f, cx + halfW, cy + halfW * 0.8f);
        canvas.drawArc(rect, happy ? 20f : 200f, 140f, false, mouthPaint);
    }

    private void drawSmirk(Canvas canvas, float cx, float cy, float halfW) {
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeWidth(dpToPx(2.5f));
        Path p = new Path();
        p.moveTo(cx - halfW, cy);
        p.quadTo(cx, cy - halfW * 0.5f, cx + halfW, cy + halfW * 0.1f);
        canvas.drawPath(p, mouthPaint);
    }

    private void drawMouthFlat(Canvas canvas, float cx, float cy, float halfW) {
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeWidth(dpToPx(2.5f));
        canvas.drawLine(cx - halfW, cy, cx + halfW, cy, mouthPaint);
    }

    private void drawOpenMouth(Canvas canvas, float cx, float cy, float r) {
        mouthPaint.setStyle(Paint.Style.FILL);
        mouthPaint.setColor(Color.parseColor("#5D4037"));
        canvas.drawCircle(cx, cy, r, mouthPaint);
        // 恢复
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setColor(Color.parseColor("#333333"));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                cancelWander();
                cancelBreath();
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                hasMoved = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    if (Math.abs(event.getRawX() - downRawX) > CLICK_THRESHOLD_PX
                            || Math.abs(event.getRawY() - downRawY) > CLICK_THRESHOLD_PX) {
                        hasMoved = true;
                    }
                    int newX = clampX((int) (event.getRawX() - getWidth() / 2f));
                    int newY = clampY((int) (event.getRawY() - getHeight() / 2f));
                    updatePosition(newX, newY);
                }
                return true;
            case MotionEvent.ACTION_UP:
                isDragging = false;
                if (!hasMoved) {
                    performClick();
                }
                startBreath();
                scheduleWander();
                snapToEdge();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private int clampX(int x) {
        if (x < 0) return 0;
        if (x + getWidth() > screenWidth) return screenWidth - getWidth();
        return x;
    }

    private int clampY(int y) {
        if (y < 0) return 0;
        int maxY = screenHeight - bottomMargin - getHeight();
        if (y > maxY) return Math.max(0, maxY);
        return y;
    }

    private void updatePosition(int x, int y) {
        currentX = x;
        currentY = y;
        if (positionListener != null) {
            positionListener.onPositionChanged(x, y);
        }
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void setBottomMargin(int margin) {
        if (margin < 0) margin = 0;
        if (margin != bottomMargin) {
            bottomMargin = margin;
            // 宠物当前位置若已超出新边界，拉回
            int maxY = screenHeight - bottomMargin - getHeight();
            if (currentY > maxY && maxY >= 0) {
                updatePosition(currentX, maxY);
            }
        }
    }

    public void setExpression(String expression) {
        String expr = expression == null ? "neutral" : expression;
        if (!expr.equals(this.expression)) {
            this.expression = expr;
            invalidate();
            // 4 秒后自动回到中性,避免长期挂着夸张表情
            handler.removeCallbacks(resetExpressionRunnable);
            handler.postDelayed(resetExpressionRunnable, 4000);
        }
    }

    private final Runnable resetExpressionRunnable = () -> {
        if (!"neutral".equals(expression)) {
            expression = "neutral";
            invalidate();
        }
    };

    public void setInitialPosition(int x, int y) {
        currentX = x;
        currentY = y;
    }

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    public void startBreath() {
        if (breathAnimator != null && breathAnimator.isRunning()) return;
        breathAnimator = ValueAnimator.ofFloat(1.0f, BREATH_SCALE_MAX);
        breathAnimator.setDuration(1200);
        breathAnimator.setRepeatMode(ValueAnimator.REVERSE);
        breathAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        breathAnimator.addUpdateListener(anim -> {
            breathScale = (float) anim.getAnimatedValue();
            invalidate();
        });
        breathAnimator.start();
    }

    private void cancelBreath() {
        if (breathAnimator != null) {
            breathAnimator.cancel();
            breathScale = 1.0f;
            invalidate();
        }
    }

    public void scheduleWander() {
        if (wanderRunnable != null) {
            handler.removeCallbacks(wanderRunnable);
        }
        wanderRunnable = this::startWander;
        handler.postDelayed(wanderRunnable, WANDER_INTERVAL_MS);
    }

    public void cancelWander() {
        if (wanderAnimator != null && wanderAnimator.isRunning()) {
            wanderAnimator.cancel();
        }
        if (wanderRunnable != null) {
            handler.removeCallbacks(wanderRunnable);
        }
    }

    private void startWander() {
        if (isDragging) return;
        final int startX = currentX;
        final int startY = currentY;
        final int targetX = clampX((int) (Math.random() * (screenWidth - getWidth())));
        final int targetY = clampY((int) (Math.random() * (screenHeight - getHeight())));

        wanderAnimator = ValueAnimator.ofFloat(0f, 1f);
        wanderAnimator.setDuration(WANDER_DURATION_MS);
        wanderAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        wanderAnimator.addUpdateListener(anim -> {
            float fraction = (float) anim.getAnimatedValue();
            int x = clampX((int) (startX + (targetX - startX) * fraction));
            int y = clampY((int) (startY + (targetY - startY) * fraction));
            updatePosition(x, y);
        });
        wanderAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scheduleWander();
            }
        });
        wanderAnimator.start();
    }

    private void snapToEdge() {
        int startX = currentX;
        int targetX;
        float centerX = currentX + getWidth() / 2f;
        if (centerX < screenWidth / 2f) {
            targetX = 0;
        } else {
            targetX = screenWidth - getWidth();
        }

        snapAnimator = ValueAnimator.ofFloat(0f, 1f);
        snapAnimator.setDuration(SNAP_DURATION_MS);
        snapAnimator.setInterpolator(new OvershootInterpolator(0.5f));
        snapAnimator.addUpdateListener(anim -> {
            float fraction = (float) anim.getAnimatedValue();
            int x = clampX((int) (startX + (targetX - startX) * fraction));
            updatePosition(x, currentY);
        });
        snapAnimator.start();
    }

    public void setOnPetClickListener(OnPetClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnPositionChangedListener(OnPositionChangedListener listener) {
        this.positionListener = listener;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (clickListener != null) {
            clickListener.onPetClick(this);
        }
        return true;
    }

    public void destroy() {
        cancelWander();
        cancelBreath();
        if (snapAnimator != null && snapAnimator.isRunning()) {
            snapAnimator.cancel();
        }
        handler.removeCallbacksAndMessages(null);
    }
}

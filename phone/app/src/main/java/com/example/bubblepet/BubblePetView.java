package com.example.bubblepet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    private float petSizePx;
    private float breathScale = 1.0f;
    private ValueAnimator breathAnimator;
    private ValueAnimator wanderAnimator;
    private ValueAnimator snapAnimator;
    private Runnable wanderRunnable;
    private final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());

    private int screenWidth;
    private int screenHeight;
    private boolean isDragging = false;
    private float downRawX;
    private float downRawY;
    private boolean hasMoved;
    private int currentX;
    private int currentY;

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

        canvas.drawCircle(leftEyeX, eyeY, eyeSize, eyeWhitePaint);
        canvas.drawCircle(leftEyeX + eyeSize * 0.15f, eyeY, pupilSize, eyePupilPaint);

        canvas.drawCircle(rightEyeX, eyeY, eyeSize, eyeWhitePaint);
        canvas.drawCircle(rightEyeX + eyeSize * 0.15f, eyeY, pupilSize, eyePupilPaint);

        canvas.restore();
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
        if (y + getHeight() > screenHeight) return screenHeight - getHeight();
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

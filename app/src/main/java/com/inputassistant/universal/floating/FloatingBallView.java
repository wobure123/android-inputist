package com.inputassistant.universal.floating;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.inputassistant.universal.MainActivity;
import com.inputassistant.universal.R;
import com.inputassistant.universal.utils.InputMethodHelper;

/**
 * 悬浮球视图类
 * 处理悬浮球的显示、动画和交互
 */
public class FloatingBallView extends FrameLayout {
    private static final String TAG = "FloatingBallView";
    
    private ImageView ivFloatingBall;
    private TextView tvFloatingHint;
    
    private FloatingBallManager manager;
    private boolean isDragging = false;
    private float lastX, lastY;
    private float downX, downY;
    private long touchStartTime;
    private long lastClickTime = 0; // 防止重复点击
    
    // 点击和拖拽的判断阈值
    private static final float CLICK_THRESHOLD = 10f;
    private static final long CLICK_TIME_THRESHOLD = 200;
    private static final long CLICK_DEBOUNCE_TIME = 500; // 防抖时间

    public FloatingBallView(Context context) {
        super(context);
        init();
    }

    public FloatingBallView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 加载布局
        View.inflate(getContext(), R.layout.floating_ball, this);
        
        ivFloatingBall = findViewById(R.id.iv_floating_ball);
        tvFloatingHint = findViewById(R.id.tv_floating_hint);
        
        // 设置点击监听
        setOnClickListener(this::onBallClick);
        
        // 初始化为可见
        setVisibility(View.VISIBLE);
        setAlpha(0.8f);
        
        // 初始化状态显示
        updateStatusDisplay();
    }
    
    /**
     * 设置管理器引用
     */
    public void setManager(FloatingBallManager manager) {
        this.manager = manager;
    }
    
    /**
     * 处理悬浮球点击事件
     */
    private void onBallClick(View v) {
        long currentTime = System.currentTimeMillis();
        
        // 防重复点击
        if (currentTime - lastClickTime < CLICK_DEBOUNCE_TIME) {
            Log.d(TAG, "Click ignored due to debounce (too soon after last click)");
            return;
        }
        lastClickTime = currentTime;
        
        Log.d(TAG, "FloatingBall clicked");
        
        // 添加点击动画效果
        animateClick();
        
        // 直接调用管理器的点击事件，让服务统一处理
        if (manager != null) {
            manager.onFloatingBallClicked();
        } else {
            Log.e(TAG, "Manager is null, cannot handle click");
        }
    }
    
    /**
     * 显示提示消息
     */
    private void showToast(String message) {
        try {
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.w(TAG, "Failed to show toast: " + message, e);
        }
    }
    
    /**
     * 打开主应用
     */
    private void openMainActivity() {
        try {
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            getContext().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 点击动画效果
     */
    private void animateClick() {
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1.0f, 1.2f, 1.0f);
        scaleAnimator.setDuration(200);
        scaleAnimator.setInterpolator(new DecelerateInterpolator());
        scaleAnimator.addUpdateListener(animation -> {
            float scale = (Float) animation.getAnimatedValue();
            setScaleX(scale);
            setScaleY(scale);
        });
        scaleAnimator.start();
    }
    
    /**
     * 显示动画
     */
    public void showWithAnimation() {
        Log.d(TAG, "showWithAnimation() called");
        
        setVisibility(View.VISIBLE);
        setAlpha(0f);
        setScaleX(0.5f);
        setScaleY(0.5f);
        
        Log.d(TAG, "Starting show animation - visibility set to VISIBLE");
        
        animate()
            .alpha(0.8f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(300)
            .setInterpolator(new DecelerateInterpolator())
            .withStartAction(() -> Log.d(TAG, "Show animation started"))
            .withEndAction(() -> Log.d(TAG, "Show animation completed"))
            .start();
    }
    
    /**
     * 隐藏动画
     */
    public void hideWithAnimation(Runnable onComplete) {
        animate()
            .alpha(0f)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .setDuration(200)
            .setInterpolator(new DecelerateInterpolator())
            .withEndAction(() -> {
                setVisibility(View.GONE);
                if (onComplete != null) {
                    onComplete.run();
                }
            })
            .start();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = false;
                touchStartTime = System.currentTimeMillis();
                downX = lastX = event.getRawX();
                downY = lastY = event.getRawY();
                
                // 取消可能正在进行的动画
                clearAnimation();
                setAlpha(1.0f);
                break;
                
            case MotionEvent.ACTION_MOVE:
                float currentX = event.getRawX();
                float currentY = event.getRawY();
                
                float deltaX = currentX - downX;
                float deltaY = currentY - downY;
                
                // 判断是否开始拖拽
                if (!isDragging && (Math.abs(deltaX) > CLICK_THRESHOLD || Math.abs(deltaY) > CLICK_THRESHOLD)) {
                    isDragging = true;
                }
                
                if (isDragging && manager != null) {
                    // 更新悬浮球位置
                    manager.updatePosition(currentX, currentY);
                }
                
                lastX = currentX;
                lastY = currentY;
                break;
                
            case MotionEvent.ACTION_UP:
                long touchDuration = System.currentTimeMillis() - touchStartTime;
                float totalDistance = (float) Math.sqrt(
                    Math.pow(event.getRawX() - downX, 2) + Math.pow(event.getRawY() - downY, 2)
                );
                
                // 恢复透明度
                setAlpha(0.8f);
                
                if (isDragging) {
                    // 拖拽结束，执行磁性吸附
                    if (manager != null) {
                        manager.performMagneticSnap();
                    }
                } else if (touchDuration < CLICK_TIME_THRESHOLD && totalDistance < CLICK_THRESHOLD) {
                    // 判定为点击
                    performClick();
                }
                
                isDragging = false;
                break;
        }
        
        return true;
    }
    
    /**
     * 更新状态显示
     */
    public void updateStatus(boolean isInputActive) {
        updateStatusDisplay();
    }
    
    @Override
    public boolean performClick() {
        super.performClick();
        
        // 通知管理器处理点击事件
        if (manager != null) {
            manager.onFloatingBallClicked();
        }
        
        return true;
    }
    
    /**
     * 更新悬浮球状态显示
     */
    private void updateStatusDisplay() {
        try {
            InputMethodHelper.InputMethodStatus status = 
                InputMethodHelper.checkInputMethodStatus(getContext());
            
            switch (status) {
                case NOT_ENABLED:
                    // 输入法未启用 - 红色提示
                    tvFloatingHint.setVisibility(View.VISIBLE);
                    tvFloatingHint.setText("未启用");
                    tvFloatingHint.setTextColor(getContext().getColor(android.R.color.holo_red_light));
                    ivFloatingBall.setImageTintList(
                        getContext().getColorStateList(android.R.color.holo_red_light));
                    break;
                    
                case ENABLED_NOT_CURRENT:
                    // 已启用但非当前 - 橙色提示
                    tvFloatingHint.setVisibility(View.VISIBLE);
                    tvFloatingHint.setText("点击切换");
                    tvFloatingHint.setTextColor(getContext().getColor(android.R.color.holo_orange_light));
                    ivFloatingBall.setImageTintList(
                        getContext().getColorStateList(android.R.color.holo_orange_light));
                    break;
                    
                case ENABLED_AND_CURRENT:
                    // 已启用且为当前 - 绿色提示
                    tvFloatingHint.setVisibility(View.VISIBLE);
                    tvFloatingHint.setText("已激活");
                    tvFloatingHint.setTextColor(getContext().getColor(android.R.color.holo_green_light));
                    ivFloatingBall.setImageTintList(null); // 使用原色
                    break;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to update status display", e);
            // 默认状态
            tvFloatingHint.setVisibility(View.GONE);
            ivFloatingBall.setImageTintList(getContext().getColorStateList(android.R.color.white));
        }
    }
}

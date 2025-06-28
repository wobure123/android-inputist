package com.inputassistant.universal.floating;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.inputassistant.universal.R;
import com.inputassistant.universal.repository.SettingsRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * 悬浮球服务
 * 提供快速输入法切换功能
 */
public class FloatingBallService extends Service {
    private static final String TAG = "FloatingBallService";
    
    private WindowManager windowManager;
    private View floatingView;
    private ImageView floatingBall;
    private WindowManager.LayoutParams params;
    private SettingsRepository settingsRepository;
    private InputMethodManager inputMethodManager;
    private InputMethodHelper inputMethodHelper;
    
    // 悬浮球状态
    private boolean isDragging = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 检查悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        
        try {
            settingsRepository = new SettingsRepository(this);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            stopSelf();
            return;
        }
        
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodHelper = new InputMethodHelper(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        createFloatingBall();
    }
    
    private void createFloatingBall() {
        // 创建悬浮球视图
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.layout_floating_ball, null);
        floatingBall = floatingView.findViewById(R.id.floating_ball);
        
        // 按照参考项目风格设置悬浮球样式
        setupFloatingBallStyle();
        
        // 设置窗口参数
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = settingsRepository.getFloatingBallPositionX();
        params.y = settingsRepository.getFloatingBallPositionY();
        
        // 设置触摸监听
        setupTouchListener();
        
        // 添加到窗口管理器
        windowManager.addView(floatingView, params);
        
        // 设置统一的悬浮球样式（资源优化版本）
        setupSimpleFloatingBallStyle();
    }
    
    /**
     * 设置悬浮球样式 - 资源优化版本（统一样式）
     */
    private void setupSimpleFloatingBallStyle() {
        // 使用统一的简化键盘图标，减少资源消耗
        floatingBall.setImageResource(R.drawable.ic_floating_ball_simple);
        
        // 使用半透明的蓝色作为默认颜色
        int color = getResources().getColor(R.color.floating_ball_blue);
        floatingBall.setColorFilter(color);
        
        // 固定透明度为60%，避免频繁更新
        floatingBall.setAlpha(0.6f);
    }
    
    private void setupTouchListener() {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private long downTime;
            private float lastX, lastY;
            private float initialX, initialY;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downTime = System.currentTimeMillis();
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        initialX = params.x;
                        initialY = params.y;
                        isDragging = false;
                        // 添加触觉反馈
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - lastX;
                        float deltaY = event.getRawY() - lastY;
                        
                        // 拖拽判断条件
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true;
                            params.x += (int)deltaX;
                            params.y += (int)deltaY;
                            windowManager.updateViewLayout(floatingView, params);
                            lastX = event.getRawX();
                            lastY = event.getRawY();
                        }
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        long upTime = System.currentTimeMillis();
                        if (!isDragging && (upTime - downTime) < 500) {
                            // 短点击 - 切换输入法
                            animateClick();
                            switchInputMethod();
                        } else if (isDragging) {
                            // 拖拽结束 - 确保在屏幕范围内
                            ensureWithinScreen();
                            savePosition();
                        }
                        return true;
                }
                return false;
            }
        });
    }
    
    /**
     * 点击动画效果 - 参考项目简洁风格
     */
    private void animateClick() {
        if (floatingBall != null) {
            floatingBall.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(100)
                .withEndAction(() -> {
                    floatingBall.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(150)
                        .start();
                })
                .start();
        }
    }
    
    /**
     * 切换输入法 - 使用Activity方式解决系统限制
     */
    private void switchInputMethod() {
        try {
            // 获取当前输入法状态
            String currentIME = inputMethodHelper.getCurrentInputMethodId();
            String ourPackage = getPackageName();
            
            if (currentIME != null && currentIME.contains(ourPackage)) {
                // 当前是我们的输入法
                settingsRepository.savePreviousInputMethod(currentIME);
                showToast("💡 选择其他输入法");
            } else {
                // 当前不是我们的输入法
                if (currentIME != null && !currentIME.isEmpty()) {
                    settingsRepository.savePreviousInputMethod(currentIME);
                }
                showToast("💡 选择Inputist输入法");
            }
            
            // 根据Android版本选择不同的调用方式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android M+ 使用透明Activity方式（关键修复）
                Intent intent = new Intent(this, KeyboardManagerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                               Intent.FLAG_ACTIVITY_NO_ANIMATION |
                               Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.putExtra(KeyboardManagerActivity.DELAY_SHOW_KEY, 50L); // 进一步减少延迟
                startActivity(intent);
            } else {
                // Android M以下直接调用
                inputMethodHelper.showInputMethodPicker();
            }
            
            // 简化版本：移除状态更新，节省资源
            // 用户通过点击悬浮球直接获得输入法选择器，无需状态指示
            
        } catch (Exception e) {
            e.printStackTrace();
            // 兜底方案
            showToast("正在打开输入法选择器...");
            inputMethodHelper.showInputMethodPicker();
        }
    }
    
    /**
     * 显示提示消息
     */
    private void showToast(String message) {
        try {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 简化版本的输入法状态检查（可选功能）
     * 如果需要在 Toast 中显示不同提示，可以调用此方法
     */
    private boolean isOurInputMethodActive() {
        try {
            String currentIME = inputMethodHelper.getCurrentInputMethodId();
            String ourPackage = getPackageName();
            return currentIME != null && currentIME.contains(ourPackage);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 确保悬浮球在屏幕范围内，允许自由移动
     */
    private void ensureWithinScreen() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int ballWidth = floatingView.getWidth();
        int ballHeight = floatingView.getHeight();
        
        // 确保悬浮球不超出屏幕边界，但允许在任意位置停留
        params.x = Math.max(0, Math.min(params.x, screenWidth - ballWidth));
        params.y = Math.max(0, Math.min(params.y, screenHeight - ballHeight));
        
        windowManager.updateViewLayout(floatingView, params);
    }
    
    /**
     * 保存悬浮球位置
     */
    private void savePosition() {
        try {
            settingsRepository.saveFloatingBallPosition(params.x, params.y);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // 服务被杀死后会自动重启
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
    }
}

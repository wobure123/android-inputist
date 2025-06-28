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
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        createFloatingBall();
    }
    
    private void createFloatingBall() {
        // 创建悬浮球视图
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.layout_floating_ball, null);
        floatingBall = floatingView.findViewById(R.id.floating_ball);
        
        // 设置窗口参数
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = settingsRepository.getFloatingBallPositionX();
        params.y = settingsRepository.getFloatingBallPositionY();
        
        // 设置触摸监听
        setupTouchListener();
        
        // 添加到窗口管理器
        windowManager.addView(floatingView, params);
        
        // 更新悬浮球图标状态
        updateFloatingBallIcon();
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
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - lastX;
                        float deltaY = event.getRawY() - lastY;
                        
                        // 更宽松的拖拽判断条件
                        if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
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
                            switchInputMethod();
                        } else if (isDragging) {
                            // 拖拽结束 - 简单贴边
                            snapToEdge();
                            savePosition();
                        }
                        return true;
                }
                return false;
            }
        });
    }
    
    /**
     * 切换输入法 - 简化可靠的方案
     */
    private void switchInputMethod() {
        try {
            String currentIME = getCurrentInputMethodId();
            String ourPackage = getPackageName();
            
            if (currentIME != null && currentIME.contains(ourPackage)) {
                // 当前是我们的输入法，记住这个状态并提示选择其他输入法
                settingsRepository.savePreviousInputMethod(currentIME);
                showToast("💡 选择其他输入法可快速切换回来");
            } else {
                // 当前不是我们的输入法，记住它并提示选择Inputist
                if (currentIME != null && !currentIME.isEmpty()) {
                    settingsRepository.savePreviousInputMethod(currentIME);
                }
                showToast("💡 选择Inputist输入法开始使用AI功能");
            }
            
            // 直接显示输入法选择器（最可靠的方式）
            showInputMethodPicker();
            
            // 延迟更新状态，给用户时间选择
            if (floatingBall != null) {
                floatingBall.postDelayed(() -> updateFloatingBallIcon(), 1000);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showToast("打开输入法选择器");
            showInputMethodPicker();
        }
    }
    
    /**
     * 获取当前输入法ID
     */
    private String getCurrentInputMethodId() {
        try {
            return Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD
            );
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * 显示输入法选择器 - 简化可靠的方案
     */
    private void showInputMethodPicker() {
        try {
            if (inputMethodManager != null) {
                inputMethodManager.showInputMethodPicker();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
     * 更新悬浮球图标状态 - 简化逻辑
     */
    private void updateFloatingBallIcon() {
        try {
            String currentIME = getCurrentInputMethodId();
            String ourPackage = getPackageName();
            
            if (currentIME != null && currentIME.contains(ourPackage)) {
                // 当前是Inputist输入法
                floatingBall.setImageResource(R.drawable.ic_floating_ball_active);
                floatingBall.setAlpha(1.0f);
            } else {
                // 当前不是Inputist输入法
                floatingBall.setImageResource(R.drawable.ic_floating_ball_inactive);
                floatingBall.setAlpha(0.8f);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 默认状态
            floatingBall.setImageResource(R.drawable.ic_floating_ball_inactive);
            floatingBall.setAlpha(0.8f);
        }
    }
    
    /**
     * 简单贴边功能 - 不隐藏，保持完全可见
     */
    private void snapToEdge() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int ballWidth = floatingView.getWidth();
        
        // 简单贴边到最近的边缘，但保持完全可见
        if (params.x < screenWidth / 2) {
            params.x = 0;  // 贴左边
        } else {
            params.x = screenWidth - ballWidth;  // 贴右边
        }
        
        // 确保垂直位置在屏幕范围内
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int ballHeight = floatingView.getHeight();
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

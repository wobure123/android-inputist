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
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    
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
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isDragging = false;
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;
                        
                        // 判断是否为拖拽操作
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true;
                            params.x = (int) (initialX + deltaX);
                            params.y = (int) (initialY + deltaY);
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            // 点击事件 - 切换输入法
                            switchInputMethod();
                        } else {
                            // 拖拽结束 - 自动贴边
                            autoSnapToEdge();
                        }
                        return true;
                }
                return false;
            }
        });
    }
    
    /**
     * 切换输入法
     */
    private void switchInputMethod() {
        try {
            String currentIME = Settings.Secure.getString(
                    getContentResolver(), 
                    Settings.Secure.DEFAULT_INPUT_METHOD
            );
            
            String targetPackage = getPackageName();
            String inputistIME = targetPackage + "/.ime.TranslateInputMethodService";
            
            if (currentIME != null && currentIME.contains(targetPackage)) {
                // 当前是Inputist，切换到上一个输入法
                String previousIME = settingsRepository.getPreviousInputMethod();
                if (!previousIME.isEmpty() && !previousIME.equals(inputistIME)) {
                    switchToInputMethod(previousIME);
                } else {
                    // 如果没有保存的上一个输入法，显示输入法选择器
                    showInputMethodPicker();
                }
            } else {
                // 当前不是Inputist，保存当前输入法并切换到Inputist
                if (currentIME != null && !currentIME.equals(inputistIME)) {
                    settingsRepository.savePreviousInputMethod(currentIME);
                }
                switchToInputMethod(inputistIME);
            }
            
            // 更新悬浮球图标状态
            updateFloatingBallIcon();
            
        } catch (Exception e) {
            e.printStackTrace();
            // 如果自动切换失败，显示输入法选择器
            showInputMethodPicker();
        }
    }
    
    /**
     * 切换到指定输入法
     */
    private void switchToInputMethod(String inputMethodId) {
        try {
            Settings.Secure.putString(
                    getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD,
                    inputMethodId
            );
        } catch (Exception e) {
            e.printStackTrace();
            // 如果没有权限，显示输入法选择器
            showInputMethodPicker();
        }
    }
    
    /**
     * 显示输入法选择器
     */
    private void showInputMethodPicker() {
        if (inputMethodManager != null) {
            inputMethodManager.showInputMethodPicker();
        }
    }
    
    /**
     * 更新悬浮球图标状态
     */
    private void updateFloatingBallIcon() {
        try {
            String currentIME = Settings.Secure.getString(
                    getContentResolver(), 
                    Settings.Secure.DEFAULT_INPUT_METHOD
            );
            
            if (currentIME != null && currentIME.contains(getPackageName())) {
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
     * 自动贴边功能
     */
    private void autoSnapToEdge() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int ballWidth = floatingView.getWidth();
        
        // 判断悬浮球应该贴向左边还是右边
        if (params.x < screenWidth / 2) {
            // 贴向左边
            params.x = 0;
        } else {
            // 贴向右边
            params.x = screenWidth - ballWidth;
        }
        
        // 确保不会超出屏幕边界
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int ballHeight = floatingView.getHeight();
        
        if (params.y < 0) {
            params.y = 0;
        } else if (params.y > screenHeight - ballHeight) {
            params.y = screenHeight - ballHeight;
        }
        
        windowManager.updateViewLayout(floatingView, params);
        
        // 保存悬浮球位置
        settingsRepository.saveFloatingBallPosition(params.x, params.y);
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

package com.inputassistant.universal.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.inputassistant.universal.floating.FloatingBallService;
import com.inputassistant.universal.repository.SettingsRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * 全局输入检测服务 - 简化版
 * 监听软键盘弹出/隐藏状态来控制悬浮球显示
 */
public class GlobalInputDetectionService extends AccessibilityService {
    private static final String TAG = "GlobalInputDetectionService";
    
    private SettingsRepository settingsRepository;
    private FloatingBallService floatingBallService;
    private boolean isFloatingBallServiceBound = false;
    private boolean isFloatingBallEnabled = false;
    
    // 软键盘状态检测
    private int screenHeight = 0;
    private boolean keyboardVisible = false;
    private String currentInputMethod = "";
    
    // 服务连接
    private ServiceConnection floatingBallConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloatingBallService.FloatingBallBinder binder = (FloatingBallService.FloatingBallBinder) service;
            floatingBallService = binder.getService();
            isFloatingBallServiceBound = true;
            Log.d(TAG, "FloatingBallService connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            floatingBallService = null;
            isFloatingBallServiceBound = false;
            Log.d(TAG, "FloatingBallService disconnected");
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "GlobalInputDetectionService connected");
        
        // 初始化屏幕高度
        initScreenHeight();
        
        // 初始化设置仓库
        try {
            settingsRepository = new SettingsRepository(this);
            isFloatingBallEnabled = settingsRepository.isFloatingBallEnabled();
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to initialize settings repository", e);
            return;
        }
        
        // 配置辅助功能服务 - 只监听窗口变化事件
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.packageNames = null;
        info.notificationTimeout = 100;
        setServiceInfo(info);
        
        // 绑定悬浮球服务
        if (isFloatingBallEnabled) {
            bindFloatingBallService();
        }
    }
    
    /**
     * 初始化屏幕高度
     */
    private void initScreenHeight() {
        try {
            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            screenHeight = metrics.heightPixels;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize screen height", e);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 检查悬浮球是否启用
        try {
            isFloatingBallEnabled = settingsRepository != null && settingsRepository.isFloatingBallEnabled();
        } catch (Exception e) {
            return;
        }
        
        if (!isFloatingBallEnabled || !isFloatingBallServiceBound) {
            if (isFloatingBallEnabled && !isFloatingBallServiceBound) {
                bindFloatingBallService();
            }
            return;
        }
        
        // 只处理窗口变化事件来检测软键盘状态
        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            postDelayed(() -> checkKeyboardState(), 200);
        }
    }
    
    /**
     * 检查软键盘状态
     */
    private void checkKeyboardState() {
        if (screenHeight <= 0) return;
        
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) return;
            
            Rect windowBounds = new Rect();
            rootNode.getBoundsInScreen(windowBounds);
            rootNode.recycle();
            
            // 计算是否显示软键盘（25%阈值）
            int visibleHeight = windowBounds.height();
            boolean currentKeyboardVisible = (screenHeight - visibleHeight) > (screenHeight * 0.25);
            String currentIME = getCurrentInputMethod();
            
            // 检查状态变化
            if (currentKeyboardVisible != keyboardVisible || !currentIME.equals(currentInputMethod)) {
                keyboardVisible = currentKeyboardVisible;
                currentInputMethod = currentIME;
                
                if (keyboardVisible && !isInputistIME(currentInputMethod)) {
                    // 非Inputist输入法弹出，显示悬浮球
                    showFloatingBall();
                } else {
                    // 输入法隐藏或切换到Inputist，隐藏悬浮球
                    hideFloatingBall();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking keyboard state", e);
        }
    }
    
    /**
     * 获取当前输入法
     */
    private String getCurrentInputMethod() {
        try {
            String ime = android.provider.Settings.Secure.getString(
                getContentResolver(),
                android.provider.Settings.Secure.DEFAULT_INPUT_METHOD
            );
            return ime != null ? ime : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 检查是否是Inputist输入法
     */
    private boolean isInputistIME(String ime) {
        return ime != null && ime.contains("com.inputassistant.universal");
    }
    
    /**
     * 显示悬浮球
     */
    private void showFloatingBall() {
        if (floatingBallService != null) {
            floatingBallService.showFloatingBall();
        }
    }
    
    /**
     * 隐藏悬浮球
     */
    private void hideFloatingBall() {
        if (floatingBallService != null) {
            floatingBallService.hideFloatingBall();
        }
    }
    
    /**
     * 绑定悬浮球服务
     */
    private void bindFloatingBallService() {
        if (!isFloatingBallServiceBound) {
            Intent intent = new Intent(this, FloatingBallService.class);
            startService(intent);
            bindService(intent, floatingBallConnection, Context.BIND_AUTO_CREATE);
        }
    }
    
    /**
     * 解绑悬浮球服务
     */
    private void unbindFloatingBallService() {
        if (isFloatingBallServiceBound) {
            unbindService(floatingBallConnection);
            isFloatingBallServiceBound = false;
        }
    }
    
    /**
     * 延迟执行任务
     */
    private void postDelayed(Runnable runnable, long delayMillis) {
        android.os.Handler handler = new android.os.Handler(getMainLooper());
        handler.postDelayed(runnable, delayMillis);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initScreenHeight();
    }

    @Override
    public void onInterrupt() {
        // 服务中断时不需要特殊处理
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindFloatingBallService();
    }
}

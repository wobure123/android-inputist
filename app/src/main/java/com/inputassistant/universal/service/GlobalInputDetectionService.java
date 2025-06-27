package com.inputassistant.universal.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.inputassistant.universal.floating.FloatingBallService;
import com.inputassistant.universal.repository.SettingsRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * 全局输入检测服务
 * 使用辅助功能监听全局输入框焦点变化
 * 在任何输入框激活时显示悬浮球
 */
public class GlobalInputDetectionService extends AccessibilityService {
    private static final String TAG = "GlobalInputDetectionService";
    
    private SettingsRepository settingsRepository;
    private FloatingBallService floatingBallService;
    private boolean isFloatingBallServiceBound = false;
    private boolean isFloatingBallEnabled = false;
    
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
        
        // 初始化设置仓库
        try {
            settingsRepository = new SettingsRepository(this);
            isFloatingBallEnabled = settingsRepository.isFloatingBallEnabled();
            Log.d(TAG, "Floating ball enabled: " + isFloatingBallEnabled);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to initialize settings repository", e);
            return;
        }
        
        // 配置辅助功能服务
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED | 
                         AccessibilityEvent.TYPE_VIEW_CLICKED |
                         AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);
        
        // 如果悬浮球已启用，则绑定悬浮球服务
        if (isFloatingBallEnabled) {
            Log.d(TAG, "Floating ball is enabled, binding service");
            bindFloatingBallService();
        } else {
            Log.d(TAG, "Floating ball is disabled, not binding service");
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 实时检查悬浮球是否启用
        try {
            isFloatingBallEnabled = settingsRepository != null && settingsRepository.isFloatingBallEnabled();
        } catch (Exception e) {
            Log.e(TAG, "Failed to check floating ball enabled status", e);
            return;
        }
        
        if (!isFloatingBallEnabled) {
            Log.d(TAG, "Floating ball is disabled, ignoring event");
            return;
        }
        
        if (!isFloatingBallServiceBound) {
            Log.d(TAG, "FloatingBallService not bound, trying to bind...");
            bindFloatingBallService();
            return;
        }
        
        int eventType = event.getEventType();
        Log.d(TAG, "Accessibility event: " + eventType + ", package: " + event.getPackageName());
        
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                handleViewFocused(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                // 窗口变化时检查是否有输入框
                checkForInputFields();
                break;
        }
    }
    
    /**
     * 处理视图获得焦点事件
     */
    private void handleViewFocused(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source == null) return;
        
        try {
            // 检查是否是可编辑的输入框
            if (isEditableField(source)) {
                Log.d(TAG, "Input field focused, showing floating ball");
                showFloatingBall();
            } else {
                Log.d(TAG, "Non-input field focused, hiding floating ball");
                hideFloatingBall();
            }
        } finally {
            source.recycle();
        }
    }
    
    /**
     * 检查当前窗口是否有输入框
     */
    private void checkForInputFields() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        
        try {
            boolean hasInputField = findEditableField(rootNode);
            if (hasInputField) {
                // 延迟一点显示，确保输入法已经启动
                postDelayed(() -> {
                    if (isCurrentlyInInputField()) {
                        showFloatingBall();
                    }
                }, 300);
            } else {
                hideFloatingBall();
            }
        } finally {
            rootNode.recycle();
        }
    }
    
    /**
     * 递归查找可编辑字段
     */
    private boolean findEditableField(AccessibilityNodeInfo node) {
        if (node == null) return false;
        
        if (isEditableField(node)) {
            return true;
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                try {
                    if (findEditableField(child)) {
                        return true;
                    }
                } finally {
                    child.recycle();
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否是可编辑的输入框
     */
    private boolean isEditableField(AccessibilityNodeInfo node) {
        if (node == null) return false;
        
        return node.isEditable() || 
               node.isFocusable() && 
               (node.getClassName() != null && 
                (node.getClassName().toString().contains("EditText") ||
                 node.getClassName().toString().contains("TextInputEditText")));
    }
    
    /**
     * 检查当前是否在输入框中
     */
    private boolean isCurrentlyInInputField() {
        AccessibilityNodeInfo focused = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused != null) {
            try {
                return isEditableField(focused);
            } finally {
                focused.recycle();
            }
        }
        return false;
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
            // 先启动服务，再绑定
            startService(intent);
            boolean bound = bindService(intent, floatingBallConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Attempting to bind FloatingBallService, result: " + bound);
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
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindFloatingBallService();
        Log.d(TAG, "GlobalInputDetectionService destroyed");
    }
}

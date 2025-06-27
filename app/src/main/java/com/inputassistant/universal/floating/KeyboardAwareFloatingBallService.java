package com.inputassistant.universal.floating;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inputassistant.universal.repository.SettingsRepository;
import com.inputassistant.universal.utils.PermissionHelper;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * 键盘感知悬浮球服务
 * 使用现代WindowInsets API (API 30+) 和传统ViewTreeObserver方法的混合策略
 * 精准检测软键盘状态，实现悬浮球的智能显示/隐藏
 */
public class KeyboardAwareFloatingBallService extends Service {
    private static final String TAG = "KeyboardAwareFloatingBallService";
    
    private SettingsRepository settingsRepository;
    private FloatingBallManager floatingBallManager;
    private WindowManager windowManager;
    private View anchorView; // 用于检测键盘状态的锚点视图
    private WindowManager.LayoutParams anchorParams;
    
    private boolean isKeyboardVisible = false;
    private boolean isFloatingBallEnabled = false;
    private String currentInputMethod = "";
    private String previousInputMethod = "";
    
    // 传统方法相关
    private int screenHeight = 0;
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener;
    
    // 防抖动机制
    private android.os.Handler stateHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pendingStateChange = null;
    private static final long STATE_CHANGE_DELAY = 300; // 300ms 延迟避免快速切换
    
    // 定时检查相关
    private android.os.Handler periodicHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable periodicCheckRunnable;
    private boolean isPeriodicCheckRunning = false;
    
    // 输入活动检测
    private long lastInputActivityTime = 0;
    private static final long INPUT_ACTIVITY_TIMEOUT = 10000; // 10秒
    
    // Binder类，用于与其他组件通信
    public class KeyboardAwareBinder extends Binder {
        public KeyboardAwareFloatingBallService getService() {
            return KeyboardAwareFloatingBallService.this;
        }
    }
    
    private final IBinder binder = new KeyboardAwareBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "KeyboardAwareFloatingBallService created");
        Log.i(TAG, "Running on Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        Log.i(TAG, "Device: " + Build.MANUFACTURER + " " + Build.MODEL);
        
        // Android 15 特殊适配检查
        if (Build.VERSION.SDK_INT >= 35) {  // Android 15 is API 35
            Log.w(TAG, "Running on Android 15+, checking special compatibility requirements");
            
            // 检查是否在小米设备上需要特殊处理
            if (Build.MANUFACTURER.toLowerCase().contains("xiaomi") || 
                Build.MANUFACTURER.toLowerCase().contains("redmi")) {
                Log.w(TAG, "Detected Xiaomi/Redmi device on Android 15, may need special handling");
            }
        }
        
        // 检查权限
        if (!PermissionHelper.hasOverlayPermission(this)) {
            Log.w(TAG, "No overlay permission, stopping service");
            stopSelf();
            return;
        }
        
        // 初始化设置仓库
        try {
            settingsRepository = new SettingsRepository(this);
            isFloatingBallEnabled = settingsRepository.isFloatingBallEnabled();
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to initialize settings repository", e);
            stopSelf();
            return;
        }
        
        if (!isFloatingBallEnabled) {
            Log.d(TAG, "Floating ball disabled, stopping service");
            stopSelf();
            return;
        }
        
        // 初始化组件
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Log.d(TAG, "WindowManager initialized");
        
        try {
            floatingBallManager = new FloatingBallManager(this);
            Log.i(TAG, "FloatingBallManager created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create FloatingBallManager", e);
            stopSelf();
            return;
        }
        
        // 设置悬浮球点击监听器
        floatingBallManager.setOnFloatingBallClickListener(this::onFloatingBallClicked);
        Log.d(TAG, "Click listener set");
        
        // 初始化键盘检测
        initKeyboardDetection();
        
        // 获取当前输入法
        updateCurrentInputMethod();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "KeyboardAwareFloatingBallService started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "KeyboardAwareFloatingBallService destroyed");
        
        // 停止定时检查
        stopPeriodicKeyboardCheck();
        
        // 清理资源
        cleanupKeyboardDetection();
        
        if (floatingBallManager != null) {
            floatingBallManager.destroy();
            floatingBallManager = null;
        }
        
        super.onDestroy();
    }
    
    /**
     * 初始化键盘检测
     */
    private void initKeyboardDetection() {
        createAnchorView();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ 使用 WindowInsets
            initModernKeyboardDetection();
        } else {
            // API 24-29 使用传统方法
            initLegacyKeyboardDetection();
        }
    }
    
    /**
     * 创建锚点视图
     */
    private void createAnchorView() {
        Log.d(TAG, "Creating anchor view for keyboard detection");
        
        anchorView = new View(this);
        anchorView.setFocusable(false);
        anchorView.setFocusableInTouchMode(false);
        
        // 设置窗口参数
        anchorParams = new WindowManager.LayoutParams();
        
        // Android 版本适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            anchorParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            Log.d(TAG, "Using TYPE_APPLICATION_OVERLAY for Android 8+");
        } else {
            anchorParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            Log.d(TAG, "Using TYPE_PHONE for Android 7");
        }
        
        anchorParams.format = PixelFormat.TRANSLUCENT;
        anchorParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                           WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                           WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                           WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        anchorParams.width = 1;
        anchorParams.height = 1;
        anchorParams.gravity = Gravity.TOP | Gravity.START;
        anchorParams.x = 0;
        anchorParams.y = 0;
        
        try {
            windowManager.addView(anchorView, anchorParams);
            Log.i(TAG, "Anchor view created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add anchor view", e);
            Log.e(TAG, "Android version: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
            Log.e(TAG, "Device: " + Build.MANUFACTURER + " " + Build.MODEL);
            
            // 锚点视图创建失败，但不阻止服务运行，稍后重试
            anchorView = null;
            
            // 延迟重试创建锚点视图
            android.os.Handler handler = new android.os.Handler(getMainLooper());
            handler.postDelayed(() -> {
                Log.d(TAG, "Retrying to create anchor view...");
                createAnchorView();
            }, 2000);
        }
    }
    
    /**
     * 初始化现代键盘检测 (API 30+)
     */
    private void initModernKeyboardDetection() {
        if (anchorView == null) return;
        
        Log.d(TAG, "Setting up WindowInsets listener...");
        
        ViewCompat.setOnApplyWindowInsetsListener(anchorView, (v, insets) -> {
            boolean isVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            
            Log.d(TAG, "WindowInsets: IME visible=" + isVisible + ", height=" + imeHeight);
            
            handleKeyboardStateChange(isVisible);
            return insets;
        });
        
        // 在小米/红米设备上添加额外的监听机制
        if (Build.MANUFACTURER.toLowerCase().contains("xiaomi") || 
            Build.MANUFACTURER.toLowerCase().contains("redmi")) {
            Log.w(TAG, "Xiaomi device detected, enabling legacy keyboard detection as backup");
            initLegacyKeyboardDetection();
            
            // 添加定时检查机制作为最后的备用方案
            startPeriodicKeyboardCheck();
        }
    }
    
    /**
     * 初始化传统键盘检测 (API 24-29)
     */
    private void initLegacyKeyboardDetection() {
        if (anchorView == null) return;
        
        // 获取屏幕高度
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        
        layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (anchorView == null) return;
                
                android.graphics.Rect rect = new android.graphics.Rect();
                anchorView.getWindowVisibleDisplayFrame(rect);
                
                int visibleHeight = rect.height();
                int heightDiff = screenHeight - visibleHeight;
                
                // 动态阈值：考虑状态栏和导航栏
                int threshold = screenHeight / 4; // 25% 的屏幕高度作为阈值
                boolean isVisible = heightDiff > threshold;
                
                Log.d(TAG, "Legacy detection: screenHeight=" + screenHeight + 
                          ", visibleHeight=" + visibleHeight + 
                          ", heightDiff=" + heightDiff + 
                          ", threshold=" + threshold + 
                          ", isVisible=" + isVisible);
                
                handleKeyboardStateChange(isVisible);
            }
        };
        
        anchorView.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    }
    
    /**
     * 处理键盘状态变化
     */
    private void handleKeyboardStateChange(boolean isVisible) {
        if (isKeyboardVisible == isVisible) {
            Log.d(TAG, "Keyboard state unchanged: " + isVisible);
            return; // 状态未变化
        }
        
        // 记录输入活动
        if (isVisible) {
            recordInputActivity();
        }
        
        // 防抖动处理
        if (pendingStateChange != null) {
            stateHandler.removeCallbacks(pendingStateChange);
        }
        
        pendingStateChange = () -> {
            isKeyboardVisible = isVisible;
            updateCurrentInputMethod();
            
            boolean isInputist = isInputistIME(currentInputMethod);
            
            Log.i(TAG, "Keyboard state changed: visible=" + isVisible + 
                      ", currentIME=" + currentInputMethod + 
                      ", isInputist=" + isInputist);
            
            if (isVisible && !isInputist) {
                // 键盘弹出且不是Inputist输入法，显示悬浮球
                Log.i(TAG, "Should show floating ball: keyboard visible and not Inputist IME");
                showFloatingBall();
            } else {
                // 键盘隐藏或是Inputist输入法，隐藏悬浮球
                Log.i(TAG, "Should hide floating ball: keyboard hidden=" + !isVisible + " or isInputist=" + isInputist);
                hideFloatingBall();
            }
        };
        
        // 延迟执行防抖动任务
        stateHandler.postDelayed(pendingStateChange, STATE_CHANGE_DELAY);
    }
    
    /**
     * 更新当前输入法信息
     */
    private void updateCurrentInputMethod() {
        try {
            String ime = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD
            );
            
            if (ime != null && !ime.equals(currentInputMethod)) {
                previousInputMethod = currentInputMethod;
                currentInputMethod = ime;
                Log.d(TAG, "Input method changed: " + currentInputMethod);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get current input method", e);
        }
    }
    
    /**
     * 检查是否是Inputist输入法
     */
    private boolean isInputistIME(String ime) {
        return ime != null && ime.contains("com.inputassistant.universal");
    }
    
    /**
     * 悬浮球点击事件处理
     */
    private void onFloatingBallClicked() {
        Log.d(TAG, "Floating ball clicked");
        
        updateCurrentInputMethod();
        
        if (isInputistIME(currentInputMethod)) {
            // 当前是Inputist，切换到其他输入法
            switchToPreviousInputMethod();
        } else {
            // 当前不是Inputist，切换到Inputist
            switchToInputistInputMethod();
        }
    }
    
    /**
     * 切换到Inputist输入法
     */
    private void switchToInputistInputMethod() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                Log.d(TAG, "Switching to Inputist IME");
                
                // 保存当前输入法作为previous
                if (!isInputistIME(currentInputMethod)) {
                    previousInputMethod = currentInputMethod;
                    Log.d(TAG, "Saved previous IME: " + previousInputMethod);
                }
                
                // 直接显示输入法选择器，用户可以快速选择Inputist
                imm.showInputMethodPicker();
                
                // 可选：显示提示
                showToast("请选择 \"通用输入改写助手\"");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to switch to Inputist IME", e);
        }
    }
    
    /**
     * 切换到上一个输入法
     */
    private void switchToPreviousInputMethod() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                Log.d(TAG, "Switching to previous IME: " + previousInputMethod);
                
                // 方法1：使用系统提供的切换到上一个输入法
                boolean success = imm.switchToLastInputMethod(null);
                
                if (success) {
                    Log.d(TAG, "Successfully switched to last IME");
                    showToast("已切换回上一个输入法");
                } else {
                    Log.d(TAG, "switchToLastInputMethod failed, showing picker");
                    // 如果失败，显示输入法选择器
                    imm.showInputMethodPicker();
                    showToast("请选择要使用的输入法");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to switch to previous IME", e);
        }
    }
    
    /**
     * 显示提示消息
     */
    private void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 显示悬浮球
     */
    private void showFloatingBall() {
        Log.d(TAG, "showFloatingBall() called - Manager: " + (floatingBallManager != null) + 
                  ", Showing: " + (floatingBallManager != null && floatingBallManager.isShowing()));
        
        if (floatingBallManager != null && !floatingBallManager.isShowing()) {
            floatingBallManager.show();
            Log.i(TAG, "Floating ball show command sent");
        } else if (floatingBallManager == null) {
            Log.e(TAG, "FloatingBallManager is null, cannot show floating ball");
        } else {
            Log.d(TAG, "Floating ball already showing, skipping");
        }
    }
    
    /**
     * 隐藏悬浮球
     */
    private void hideFloatingBall() {
        Log.d(TAG, "hideFloatingBall() called - Manager: " + (floatingBallManager != null) + 
                  ", Showing: " + (floatingBallManager != null && floatingBallManager.isShowing()));
        
        if (floatingBallManager != null && floatingBallManager.isShowing()) {
            floatingBallManager.hide();
            Log.i(TAG, "Floating ball hide command sent");
        } else if (floatingBallManager == null) {
            Log.e(TAG, "FloatingBallManager is null, cannot hide floating ball");
        } else {
            Log.d(TAG, "Floating ball not showing, skipping hide");
        }
    }
    
    /**
     * 强制显示悬浮球（用于测试）
     */
    public void forceShowFloatingBall() {
        Log.d(TAG, "Force showing floating ball for testing");
        if (floatingBallManager != null) {
            // 先获取调试信息
            String debugInfo = floatingBallManager.getDebugInfo();
            Log.d(TAG, "FloatingBallManager debug info:\n" + debugInfo);
            
            // 如果状态异常，先重置
            if (floatingBallManager.isShowing()) {
                Log.w(TAG, "FloatingBall showing state is true but not visible, resetting...");
                floatingBallManager.forceResetState();
            }
            
            // 尝试显示
            floatingBallManager.show();
        }
    }
    
    /**
     * 获取服务状态信息（用于调试）
     */
    public String getServiceStatus() {
        StringBuilder status = new StringBuilder();
        status.append("KeyboardAwareFloatingBallService Status:\n");
        status.append("- Service running: true\n");
        status.append("- Floating ball enabled: ").append(isFloatingBallEnabled).append("\n");
        status.append("- Keyboard visible: ").append(isKeyboardVisible).append("\n");
        status.append("- Current IME: ").append(currentInputMethod).append("\n");
        status.append("- Anchor view: ").append(anchorView != null ? "Created" : "Failed").append("\n");
        status.append("- Floating ball manager: ").append(floatingBallManager != null ? "Initialized" : "Failed").append("\n");
        status.append("- Android version: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        status.append("- Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n\n");
        
        // 添加 FloatingBallManager 的详细调试信息
        if (floatingBallManager != null) {
            status.append(floatingBallManager.getDebugInfo());
        } else {
            status.append("FloatingBallManager is null!\n");
        }
        
        return status.toString();
    }
    
    /**
     * 清理键盘检测资源
     */
    private void cleanupKeyboardDetection() {
        if (anchorView != null) {
            try {
                // 移除监听器
                if (layoutListener != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    anchorView.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
                }
                
                // 移除视图
                windowManager.removeView(anchorView);
            } catch (Exception e) {
                Log.e(TAG, "Failed to cleanup anchor view", e);
            }
            anchorView = null;
        }
        
        // 停止定时检查
        stopPeriodicKeyboardCheck();
    }
    
    /**
     * 启动定时键盘检查（备用方案）
     */
    private void startPeriodicKeyboardCheck() {
        if (isPeriodicCheckRunning) return;
        
        Log.d(TAG, "Starting periodic keyboard check as backup for Xiaomi devices");
        isPeriodicCheckRunning = true;
        
        periodicCheckRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    checkKeyboardStateByInputMethodManager();
                    
                    // 每2秒检查一次
                    if (isPeriodicCheckRunning) {
                        periodicHandler.postDelayed(this, 2000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in periodic keyboard check", e);
                }
            }
        };
        
        periodicHandler.postDelayed(periodicCheckRunnable, 2000);
    }
    
    /**
     * 停止定时检查
     */
    private void stopPeriodicKeyboardCheck() {
        if (periodicCheckRunnable != null) {
            periodicHandler.removeCallbacks(periodicCheckRunnable);
            isPeriodicCheckRunning = false;
            Log.d(TAG, "Stopped periodic keyboard check");
        }
    }
    
    /**
     * 通过 InputMethodManager 检查键盘状态
     */
    private void checkKeyboardStateByInputMethodManager() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                // 检查当前输入法是否处于激活状态
                boolean isActive = imm.isActive();
                
                // 更新当前输入法信息
                String previousIME = currentInputMethod;
                updateCurrentInputMethod();
                
                // 如果输入法发生变化，记录日志
                if (!currentInputMethod.equals(previousIME)) {
                    Log.d(TAG, "Periodic check - IME changed from " + previousIME + " to " + currentInputMethod);
                }
                
                // 修正的检测逻辑：只在软键盘真正弹出时显示悬浮球
                boolean shouldShow = false;
                
                // 如果不是Inputist输入法
                if (!isInputistIME(currentInputMethod)) {
                    // 对于小米设备，使用更严格的检测：
                    // 1. 必须是第三方输入法
                    // 2. 必须有实际的输入活动（通过isActive检测）
                    if (Build.MANUFACTURER.toLowerCase().contains("xiaomi") || 
                        Build.MANUFACTURER.toLowerCase().contains("redmi")) {
                        
                        // 小米设备：第三方输入法 + 有输入活动
                        shouldShow = isCommonThirdPartyIME(currentInputMethod) && 
                                   (isActive || hasRecentInputActivity());
                        
                        Log.v(TAG, "Xiaomi device - IME: " + currentInputMethod + 
                                  ", isCommonThirdParty: " + isCommonThirdPartyIME(currentInputMethod) +
                                  ", isActive: " + isActive +
                                  ", shouldShow: " + shouldShow);
                    } else {
                        // 其他设备使用标准检测
                        shouldShow = isActive;
                        Log.v(TAG, "Standard device - IME active: " + isActive);
                    }
                }
                
                // 只有在状态真正改变时才处理
                if (shouldShow != isKeyboardVisible) {
                    Log.d(TAG, "Periodic check detected keyboard state change: " + shouldShow);
                    handleKeyboardStateChange(shouldShow);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in InputMethodManager keyboard check", e);
        }
    }
    
    /**
     * 检查是否是常见的第三方输入法
     */
    private boolean isCommonThirdPartyIME(String ime) {
        if (ime == null) return false;
        
        String imeLower = ime.toLowerCase();
        
        // 常见第三方输入法列表
        return imeLower.contains("sogou") ||      // 搜狗输入法
               imeLower.contains("baidu") ||      // 百度输入法
               imeLower.contains("iflytek") ||    // 讯飞输入法
               imeLower.contains("qq") ||         // QQ输入法
               imeLower.contains("gboard") ||     // Google输入法
               imeLower.contains("swiftkey") ||   // SwiftKey
               imeLower.contains("samsung") ||    // 三星输入法
               imeLower.contains("huawei") ||     // 华为输入法
               imeLower.contains("xiaomi") ||     // 小米输入法
               imeLower.contains("oppo") ||       // OPPO输入法
               imeLower.contains("vivo");         // VIVO输入法
    }

    /**
     * 获取当前键盘状态
     */
    public boolean isKeyboardVisible() {
        return isKeyboardVisible;
    }
    
    /**
     * 获取当前输入法
     */
    public String getCurrentInputMethod() {
        return currentInputMethod;
    }
    
    /**
     * 记录输入活动
     */
    private void recordInputActivity() {
        lastInputActivityTime = System.currentTimeMillis();
    }
    
    /**
     * 检查是否有最近的输入活动
     */
    private boolean hasRecentInputActivity() {
        long currentTime = System.currentTimeMillis();
        boolean hasRecent = (currentTime - lastInputActivityTime) < INPUT_ACTIVITY_TIMEOUT;
        
        // 如果WindowInsets检测到键盘变化，记录为输入活动
        if (isKeyboardVisible) {
            recordInputActivity();
            return true;
        }
        
        return hasRecent;
    }
}

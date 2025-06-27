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
        
        // 测试：立即尝试显示悬浮球（用于调试）
        if (floatingBallManager != null) {
            Log.d(TAG, "Testing: Attempting to show floating ball immediately for debugging");
            android.os.Handler testHandler = new android.os.Handler(getMainLooper());
            testHandler.postDelayed(() -> {
                Log.d(TAG, "Test: Forcing floating ball display");
                showFloatingBall();
            }, 1000); // 延迟1秒显示
        }
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
        
        ViewCompat.setOnApplyWindowInsetsListener(anchorView, (v, insets) -> {
            boolean isVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            
            Log.d(TAG, "WindowInsets: IME visible=" + isVisible + ", height=" + imeHeight);
            
            handleKeyboardStateChange(isVisible);
            return insets;
        });
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
                // 这里需要根据您的输入法Service名称来调整
                String inputistIme = "com.inputassistant.universal/.ime.TranslateInputMethodService";
                
                // 使用反射或Intent来切换输入法
                Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                // 或者尝试直接设置（需要权限）
                // Settings.Secure.putString(getContentResolver(), 
                //     Settings.Secure.DEFAULT_INPUT_METHOD, inputistIme);
                
                startActivity(intent);
                Log.d(TAG, "Switching to Inputist IME");
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
                // 尝试切换到上一个输入法
                imm.switchToLastInputMethod(null);
                Log.d(TAG, "Switching to previous IME: " + previousInputMethod);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to switch to previous IME", e);
        }
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
        status.append("- Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL);
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
}

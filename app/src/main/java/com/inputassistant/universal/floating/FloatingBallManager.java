package com.inputassistant.universal.floating;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

import com.inputassistant.universal.utils.PermissionHelper;

/**
 * 悬浮球管理器
 * 负责悬浮球的显示、隐藏、位置管理等核心功能
 */
public class FloatingBallManager {
    private static final String TAG = "FloatingBallManager";
    
    /**
     * 悬浮球点击监听器接口
     */
    public interface OnFloatingBallClickListener {
        void onFloatingBallClick();
    }
    
    private Context context;
    private WindowManager windowManager;
    private FloatingBallView floatingBallView;
    private FloatingMenuView floatingMenuView;
    
    private WindowManager.LayoutParams ballParams;
    private WindowManager.LayoutParams menuParams;
    
    private boolean isShowing = false;
    private boolean isMenuShowing = false;
    
    // 点击监听器
    private OnFloatingBallClickListener clickListener;
    
    // 位置记忆
    private SharedPreferences positionPrefs;
    private static final String PREFS_NAME = "floating_ball_position";
    private static final String KEY_X = "position_x";
    private static final String KEY_Y = "position_y";
    private static final String KEY_SIDE = "side"; // left or right
    
    // 屏幕尺寸
    private int screenWidth;
    private int screenHeight;
    
    public FloatingBallManager(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.positionPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        Log.d(TAG, "FloatingBallManager initializing...");
        
        try {
            initScreenSize();
            Log.d(TAG, "Screen size initialized: " + screenWidth + "x" + screenHeight);
            
            initFloatingBall();
            Log.d(TAG, "Floating ball initialized");
            
            initFloatingMenu();
            Log.d(TAG, "Floating menu initialized");
            
            Log.i(TAG, "FloatingBallManager initialization completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize FloatingBallManager", e);
        }
    }
    
    /**
     * 初始化屏幕尺寸
     */
    private void initScreenSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
    }
    
    /**
     * 初始化悬浮球
     */
    private void initFloatingBall() {
        floatingBallView = new FloatingBallView(context);
        floatingBallView.setManager(this);
        
        // 设置悬浮球布局参数
        ballParams = new WindowManager.LayoutParams();
        
        // 设置窗口类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ballParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            ballParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        ballParams.format = PixelFormat.TRANSLUCENT;
        ballParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                          WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        
        ballParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        ballParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        ballParams.gravity = Gravity.TOP | Gravity.START;
        
        // 设置初始位置
        setInitialPosition();
    }
    
    /**
     * 初始化悬浮菜单
     */
    private void initFloatingMenu() {
        floatingMenuView = new FloatingMenuView(context);
        floatingMenuView.setManager(this);
        
        // 设置菜单布局参数
        menuParams = new WindowManager.LayoutParams();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            menuParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            menuParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        menuParams.format = PixelFormat.TRANSLUCENT;
        menuParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        
        menuParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        menuParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        menuParams.gravity = Gravity.TOP | Gravity.START;
    }
    
    /**
     * 设置初始位置
     */
    private void setInitialPosition() {
        int defaultX = screenWidth - 100; // 默认在右侧
        int defaultY = screenHeight / 2;  // 默认在中间
        
        ballParams.x = positionPrefs.getInt(KEY_X, defaultX);
        ballParams.y = positionPrefs.getInt(KEY_Y, defaultY);
    }
    
    /**
     * 显示悬浮球
     */
    public void show() {
        Log.d(TAG, "show() called - Current state: isShowing=" + isShowing);
        
        // 检查权限
        boolean hasPermission = PermissionHelper.hasOverlayPermission(context);
        Log.d(TAG, "Overlay permission check: " + hasPermission);
        
        if (isShowing) {
            Log.w(TAG, "Floating ball already showing, skipping");
            return;
        }
        
        if (!hasPermission) {
            Log.e(TAG, "No overlay permission, cannot show floating ball");
            return;
        }
        
        // 检查组件状态
        Log.d(TAG, "Component check - windowManager: " + (windowManager != null) + 
                  ", floatingBallView: " + (floatingBallView != null) + 
                  ", ballParams: " + (ballParams != null));
        
        if (windowManager == null || floatingBallView == null || ballParams == null) {
            Log.e(TAG, "Required components are null, cannot show floating ball");
            return;
        }
        
        try {
            Log.d(TAG, "Adding floating ball view to window manager...");
            windowManager.addView(floatingBallView, ballParams);
            isShowing = true;
            
            Log.d(TAG, "Floating ball view added, starting animation...");
            floatingBallView.showWithAnimation();
            
            // 立即更新状态显示
            floatingBallView.updateStatus(true);
            
            Log.i(TAG, "Floating ball shown successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to show floating ball", e);
            Log.e(TAG, "Device info: " + Build.MANUFACTURER + " " + Build.MODEL + 
                      " (Android " + Build.VERSION.RELEASE + ", API " + Build.VERSION.SDK_INT + ")");
            
            // 重置状态
            isShowing = false;
            
            // 尝试重新创建视图
            try {
                Log.d(TAG, "Attempting to recreate floating ball view...");
                initFloatingBall();
                
                // 再次尝试显示
                windowManager.addView(floatingBallView, ballParams);
                isShowing = true;
                floatingBallView.showWithAnimation();
                floatingBallView.updateStatus(true);
                
                Log.i(TAG, "Floating ball shown successfully after recreation");
            } catch (Exception retryException) {
                Log.e(TAG, "Failed to show floating ball even after recreation", retryException);
            }
        }
    }
    
    /**
     * 隐藏悬浮球
     */
    public void hide() {
        if (!isShowing) {
            Log.v(TAG, "Floating ball is already hidden");
            return;
        }
        
        try {
            floatingBallView.hideWithAnimation(() -> {
                try {
                    windowManager.removeView(floatingBallView);
                    isShowing = false;
                    Log.d(TAG, "Floating ball hidden successfully");
                } catch (Exception e) {
                    Log.w(TAG, "Failed to remove floating ball view", e);
                }
            });
            
            // 同时隐藏菜单
            hideQuickMenu();
        } catch (Exception e) {
            Log.e(TAG, "Failed to hide floating ball", e);
        }
    }
    
    /**
     * 显示快捷菜单
     */
    public void showQuickMenu() {
        if (isMenuShowing || !isShowing) {
            return;
        }
        
        try {
            // 计算菜单位置（在悬浮球旁边）
            calculateMenuPosition();
            
            windowManager.addView(floatingMenuView, menuParams);
            isMenuShowing = true;
            floatingMenuView.showWithAnimation();
            Log.d(TAG, "Quick menu shown");
        } catch (Exception e) {
            Log.e(TAG, "Failed to show quick menu", e);
        }
    }
    
    /**
     * 隐藏快捷菜单
     */
    public void hideQuickMenu() {
        if (!isMenuShowing) {
            return;
        }
        
        try {
            floatingMenuView.hideWithAnimation(() -> {
                try {
                    windowManager.removeView(floatingMenuView);
                    isMenuShowing = false;
                    Log.d(TAG, "Quick menu hidden");
                } catch (Exception e) {
                    Log.w(TAG, "Failed to remove menu view", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to hide quick menu", e);
        }
    }
    
    /**
     * 计算菜单显示位置
     */
    private void calculateMenuPosition() {
        int menuWidth = 120; // 估算菜单宽度
        int menuHeight = 140; // 估算菜单高度
        int ballSize = 56; // 悬浮球大小
        
        // 根据悬浮球位置决定菜单显示方向
        boolean showOnLeft = ballParams.x > screenWidth / 2;
        
        if (showOnLeft) {
            // 在悬浮球左侧显示
            menuParams.x = ballParams.x - menuWidth - 10;
        } else {
            // 在悬浮球右侧显示
            menuParams.x = ballParams.x + ballSize + 10;
        }
        
        // 垂直居中对齐悬浮球
        menuParams.y = ballParams.y - (menuHeight - ballSize) / 2;
        
        // 确保菜单不超出屏幕边界
        if (menuParams.x < 0) {
            menuParams.x = 10;
        } else if (menuParams.x + menuWidth > screenWidth) {
            menuParams.x = screenWidth - menuWidth - 10;
        }
        
        if (menuParams.y < 0) {
            menuParams.y = 10;
        } else if (menuParams.y + menuHeight > screenHeight) {
            menuParams.y = screenHeight - menuHeight - 10;
        }
    }
    
    /**
     * 更新悬浮球位置（拖拽时调用）
     */
    public void updatePosition(float x, float y) {
        if (!isShowing) {
            return;
        }
        
        ballParams.x = (int) (x - floatingBallView.getWidth() / 2);
        ballParams.y = (int) (y - floatingBallView.getHeight() / 2);
        
        try {
            windowManager.updateViewLayout(floatingBallView, ballParams);
        } catch (Exception e) {
            Log.w(TAG, "Failed to update position", e);
        }
    }
    
    /**
     * 磁性吸附到屏幕边缘
     */
    public void performMagneticSnap() {
        if (!isShowing) {
            return;
        }
        
        int targetX;
        String side;
        
        // 判断吸附到左边还是右边
        if (ballParams.x < screenWidth / 2) {
            targetX = 0;
            side = "left";
        } else {
            targetX = screenWidth - floatingBallView.getWidth();
            side = "right";
        }
        
        // 执行动画
        animateToPosition(targetX, ballParams.y);
        
        // 保存位置
        savePosition(targetX, ballParams.y, side);
    }
    
    /**
     * 动画移动到指定位置
     */
    private void animateToPosition(int targetX, int targetY) {
        int startX = ballParams.x;
        int startY = ballParams.y;
        
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float progress = (Float) animation.getAnimatedValue();
            
            ballParams.x = (int) (startX + (targetX - startX) * progress);
            ballParams.y = (int) (startY + (targetY - startY) * progress);
            
            try {
                windowManager.updateViewLayout(floatingBallView, ballParams);
            } catch (Exception e) {
                Log.w(TAG, "Failed to update position during animation", e);
            }
        });
        animator.start();
    }
    
    /**
     * 保存位置到SharedPreferences
     */
    private void savePosition(int x, int y, String side) {
        positionPrefs.edit()
                .putInt(KEY_X, x)
                .putInt(KEY_Y, y)
                .putString(KEY_SIDE, side)
                .apply();
    }
    
    /**
     * 检查是否正在显示
     */
    public boolean isShowing() {
        return isShowing;
    }
    
    /**
     * 检查菜单是否正在显示
     */
    public boolean isMenuShowing() {
        return isMenuShowing;
    }
    
    /**
     * 设置悬浮球点击监听器
     */
    public void setOnFloatingBallClickListener(OnFloatingBallClickListener listener) {
        this.clickListener = listener;
    }
    
    /**
     * 触发悬浮球点击事件
     */
    public void onFloatingBallClicked() {
        if (clickListener != null) {
            clickListener.onFloatingBallClick();
        }
    }
    
    /**
     * 清理资源
     */
    public void destroy() {
        hide();
        floatingBallView = null;
        floatingMenuView = null;
    }
}

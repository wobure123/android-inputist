package com.inputassistant.universal.floating;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.inputassistant.universal.utils.PermissionHelper;

/**
 * 悬浮球服务
 * 管理悬浮球的生命周期，提供与外部通信的接口
 */
public class FloatingBallService extends Service {
    private static final String TAG = "FloatingBallService";
    
    private FloatingBallManager floatingBallManager;
    private boolean isServiceRunning = false;
    
    // Binder类，用于与其他组件通信
    public class FloatingBallBinder extends Binder {
        public FloatingBallService getService() {
            return FloatingBallService.this;
        }
    }
    
    private final IBinder binder = new FloatingBallBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "FloatingBallService created");
        
        // 检查权限
        if (!PermissionHelper.hasOverlayPermission(this)) {
            Log.w(TAG, "No overlay permission, stopping service");
            stopSelf();
            return;
        }
        
        // 初始化悬浮球管理器
        floatingBallManager = new FloatingBallManager(this);
        isServiceRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "FloatingBallService started");
        
        if (!PermissionHelper.hasOverlayPermission(this)) {
            Log.w(TAG, "No overlay permission, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }
        
        // 服务启动时显示悬浮球
        showFloatingBall();
        
        // 返回START_STICKY以便服务被杀死后能重启
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "FloatingBallService bound");
        return binder;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "FloatingBallService destroyed");
        
        // 清理资源
        if (floatingBallManager != null) {
            floatingBallManager.destroy();
            floatingBallManager = null;
        }
        
        isServiceRunning = false;
        super.onDestroy();
    }
    
    /**
     * 显示悬浮球
     */
    public void showFloatingBall() {
        Log.d(TAG, "showFloatingBall called");
        if (floatingBallManager == null) {
            Log.e(TAG, "FloatingBallManager is null");
            return;
        }
        
        if (floatingBallManager.isShowing()) {
            Log.d(TAG, "Floating ball is already showing");
            return;
        }
        
        floatingBallManager.show();
        Log.d(TAG, "Floating ball show command executed");
    }
    
    /**
     * 隐藏悬浮球
     */
    public void hideFloatingBall() {
        Log.d(TAG, "hideFloatingBall called");
        if (floatingBallManager == null) {
            Log.e(TAG, "FloatingBallManager is null");
            return;
        }
        
        if (!floatingBallManager.isShowing()) {
            Log.d(TAG, "Floating ball is already hidden");
            return;
        }
        
        floatingBallManager.hide();
        Log.d(TAG, "Floating ball hide command executed");
    }
    
    /**
     * 检查悬浮球是否正在显示
     */
    public boolean isFloatingBallShowing() {
        return floatingBallManager != null && floatingBallManager.isShowing();
    }
    
    /**
     * 检查服务是否正在运行
     */
    public boolean isServiceRunning() {
        return isServiceRunning;
    }
    
    /**
     * 显示快捷菜单
     */
    public void showQuickMenu() {
        if (floatingBallManager != null) {
            floatingBallManager.showQuickMenu();
        }
    }
    
    /**
     * 隐藏快捷菜单
     */
    public void hideQuickMenu() {
        if (floatingBallManager != null) {
            floatingBallManager.hideQuickMenu();
        }
    }
    
    /**
     * 获取悬浮球管理器（用于调试和高级操作）
     */
    public FloatingBallManager getFloatingBallManager() {
        return floatingBallManager;
    }
}

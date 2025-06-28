package com.inputassistant.universal.floating;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import androidx.core.app.NotificationCompat;
import com.inputassistant.universal.BuildConfig;
import com.inputassistant.universal.MainActivity;
import com.inputassistant.universal.R;
import com.inputassistant.universal.repository.SettingsRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * 悬浮球前台服务
 * 提供快速输入法切换功能，通过前台服务确保稳定运行
 */
public class FloatingBallService extends Service {
    private static final String TAG = "FloatingBallService";
    
    // 前台服务通知相关
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "floating_ball_service";
    public static final String ACTION_CLOSE_FLOATING_BALL = "com.inputassistant.universal.CLOSE_FLOATING_BALL";
    
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
            android.widget.Toast.makeText(this, "缺少悬浮窗权限", android.widget.Toast.LENGTH_SHORT).show();
            stopSelf();
            return;
        }
        
        // 创建通知渠道（Android 8.0+）
        createNotificationChannel();
        
        // 立即启动前台服务，避免ANR
        try {
            Notification notification = createSimpleNotification();
            
            // Android 14+ (API 34+) 需要指定前台服务类型
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "启动前台服务失败: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }
        
        // 初始化其他组件
        try {
            initializeComponents();
            createFloatingBall();
            android.widget.Toast.makeText(this, "悬浮球启动成功", android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "初始化失败: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() throws GeneralSecurityException, IOException {
        settingsRepository = new SettingsRepository(this);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodHelper = new InputMethodHelper(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }
    
    /**
     * 创建简化的通知（用于快速启动前台服务）
     */
    private Notification createSimpleNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Inputist 悬浮球")
            .setContentText("正在启动悬浮球服务...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }
    
    private void createFloatingBall() {
        // 创建悬浮球视图
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.layout_floating_ball, null);
        floatingBall = floatingView.findViewById(R.id.floating_ball);
        
        // 设置简化的悬浮球样式（资源优化版本）
        setupSimpleFloatingBallStyle();
        
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
        try {
            windowManager.addView(floatingView, params);
            
            // 悬浮球创建成功后，更新为完整的通知
            updateNotification();
            
        } catch (Exception e) {
            if (BuildConfig.DEBUG_LOGGING) {
                e.printStackTrace();
            }
            android.widget.Toast.makeText(this, "添加悬浮窗失败: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            // 如果添加悬浮窗失败，停止服务
            stopSelf();
            return;
        }
    }
    
    /**
     * 更新通知为完整版本
     */
    private void updateNotification() {
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, createNotification());
            }
        } catch (Exception e) {
            // 更新通知失败不影响服务运行
            if (BuildConfig.DEBUG_LOGGING) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "悬浮球服务",
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("输入法悬浮球后台服务，确保功能稳定运行");
                channel.setShowBadge(false);
                channel.setSound(null, null); // 静音
                channel.enableVibration(false); // 关闭震动
                
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                if (BuildConfig.DEBUG_LOGGING) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * 创建前台服务通知（带快捷开关）
     */
    private Notification createNotification() {
        try {
            // 点击通知打开主界面
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent mainPendingIntent = PendingIntent.getActivity(
                this, 0, mainIntent, 
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
            );
            
            // 关闭悬浮球的快捷按钮
            Intent closeIntent = new Intent(this, FloatingBallService.class);
            closeIntent.setAction(ACTION_CLOSE_FLOATING_BALL);
            PendingIntent closePendingIntent = PendingIntent.getService(
                this, 0, closeIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
            );
            
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Inputist 悬浮球运行中")
                .setContentText("点击进入设置界面管理悬浮球")
                .setSmallIcon(R.drawable.ic_floating_ball_simple)
                .setContentIntent(mainPendingIntent)
                .setOngoing(true) // 常驻通知，用户无法滑动删除
                .setPriority(NotificationCompat.PRIORITY_LOW) // 低优先级，减少干扰
                .setShowWhen(false) // 不显示时间
                .addAction(
                    R.drawable.ic_delete, // 使用删除图标
                    "关闭悬浮球",
                    closePendingIntent
                )
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText("悬浮球正在运行，随时切换输入法。点击通知进入设置，或点击关闭按钮停止服务。"))
                .build();
        } catch (Exception e) {
            if (BuildConfig.DEBUG_LOGGING) {
                e.printStackTrace();
            }
            // 创建一个简化的通知作为后备
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Inputist 悬浮球")
                .setContentText("服务运行中")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        }
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
            // 获取当前输入法状态并保存
            String currentIME = inputMethodHelper.getCurrentInputMethodId();
            if (currentIME != null && !currentIME.isEmpty()) {
                settingsRepository.savePreviousInputMethod(currentIME);
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
            
        } catch (Exception e) {
            // 性能优化：Release 版本减少异常处理开销
            if (BuildConfig.DEBUG_LOGGING) {
                e.printStackTrace();
            }
            // 兜底方案：直接调用输入法选择器
            inputMethodHelper.showInputMethodPicker();
        }
    }
    
    /**
     * 显示提示消息（性能优化版本）
     */
    private void showToast(String message) {
        try {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Release 版本中，只在 DEBUG_LOGGING 启用时打印日志
            if (BuildConfig.DEBUG_LOGGING) {
                e.printStackTrace();
            }
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
        // 处理关闭悬浮球的操作
        if (intent != null && ACTION_CLOSE_FLOATING_BALL.equals(intent.getAction())) {
            // 保存设置状态
            try {
                settingsRepository.setFloatingBallEnabled(false);
            } catch (Exception e) {
                if (BuildConfig.DEBUG_LOGGING) {
                    e.printStackTrace();
                }
            }
            
            // 显示提示
            showToast("悬浮球已关闭");
            
            // 停止服务
            stopSelf();
            return START_NOT_STICKY;
        }
        
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
        
        // 停止前台服务并移除通知
        stopForeground(true);
        
        // 清理悬浮窗
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                if (BuildConfig.DEBUG_LOGGING) {
                    e.printStackTrace();
                }
            }
        }
    }
}

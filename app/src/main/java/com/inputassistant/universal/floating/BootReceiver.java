package com.inputassistant.universal.floating;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import com.inputassistant.universal.repository.SettingsRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * 开机启动接收器
 * 在系统启动后自动启动悬浮球服务（如果用户已启用）
 */
public class BootReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            
            // 检查是否启用了悬浮球功能
            try {
                SettingsRepository settingsRepository = new SettingsRepository(context);
                boolean isFloatingBallEnabled = settingsRepository.isFloatingBallEnabled();
                
                if (isFloatingBallEnabled && Settings.canDrawOverlays(context)) {
                    // 启动悬浮球前台服务
                    Intent serviceIntent = new Intent(context, FloatingBallService.class);
                    
                    // Android 8.0+ 需要使用 startForegroundService
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                }
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}

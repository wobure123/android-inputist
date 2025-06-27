package com.inputassistant.universal.utils;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * 辅助功能权限管理工具
 */
public class AccessibilityHelper {
    
    /**
     * 检查辅助功能服务是否已启用
     */
    public static boolean isAccessibilityServiceEnabled(Context context, String serviceName) {
        String settingValue = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        
        android.util.Log.d("AccessibilityHelper", "All enabled services: " + settingValue);
        android.util.Log.d("AccessibilityHelper", "Looking for service: " + serviceName);
        
        if (settingValue != null) {
            String[] enabledServices = settingValue.split(":");
            for (String enabledService : enabledServices) {
                android.util.Log.d("AccessibilityHelper", "Found service: " + enabledService);
                if (enabledService.equals(serviceName)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查我们的辅助功能服务是否已启用
     */
    public static boolean isOurAccessibilityServiceEnabled(Context context) {
        // 正确的服务名称格式：包名/服务类全名
        String serviceName = context.getPackageName() + 
                "/com.inputassistant.universal.service.GlobalInputDetectionService";
        
        // 添加调试日志
        android.util.Log.d("AccessibilityHelper", "Checking service: " + serviceName);
        
        boolean enabled = isAccessibilityServiceEnabled(context, serviceName);
        android.util.Log.d("AccessibilityHelper", "Service enabled: " + enabled);
        
        return enabled;
    }
    
    /**
     * 打开辅助功能设置页面
     */
    public static void openAccessibilitySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    
    /**
     * 检查辅助功能总开关是否启用
     */
    public static boolean isAccessibilityEnabled(Context context) {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            // 设置未找到，表示未启用
        }
        
        return accessibilityEnabled == 1;
    }
}

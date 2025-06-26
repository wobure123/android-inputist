package com.inputassistant.universal.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;

/**
 * 权限管理工具类
 * 专门处理悬浮窗权限的申请和检查
 */
public class PermissionHelper {
    
    /**
     * 检查是否有悬浮窗权限
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true; // Android 6.0以下版本默认有权限
    }
    
    /**
     * 申请悬浮窗权限
     */
    public static void requestOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                showPermissionDialog(activity);
            }
        }
    }
    
    /**
     * 显示权限申请对话框
     */
    private static void showPermissionDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("需要悬浮窗权限")
                .setMessage("为了在其他应用中显示输入法助手悬浮球，需要开启悬浮窗权限。\n\n" +
                           "请在接下来的设置页面中找到「" + getAppName(activity) + "」并开启权限。")
                .setPositiveButton("去设置", (dialog, which) -> {
                    openOverlaySettings(activity);
                })
                .setNegativeButton("取消", null)
                .setCancelable(false)
                .show();
    }
    
    /**
     * 打开悬浮窗权限设置页面
     */
    public static void openOverlaySettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                // 如果无法打开特定应用的设置页面，打开总的悬浮窗设置
                Intent fallbackIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                fallbackIntent.setData(Uri.parse("package:" + context.getPackageName()));
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallbackIntent);
            }
        }
    }
    
    /**
     * 获取应用名称
     */
    private static String getAppName(Context context) {
        try {
            return context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
        } catch (Exception e) {
            return "输入法助手";
        }
    }
    
    /**
     * 检查并请求必要权限
     * @param activity 当前Activity
     * @param callback 权限检查结果回调
     */
    public static void checkAndRequestPermissions(Activity activity, PermissionCallback callback) {
        if (hasOverlayPermission(activity)) {
            callback.onPermissionGranted();
        } else {
            callback.onPermissionDenied();
            requestOverlayPermission(activity);
        }
    }
    
    /**
     * 权限检查回调接口
     */
    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }
}

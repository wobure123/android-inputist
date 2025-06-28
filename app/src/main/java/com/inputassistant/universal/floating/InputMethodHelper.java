package com.inputassistant.universal.floating;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

/**
 * 输入法切换帮助类
 * 解决在不同输入法状态下的系统限制问题
 */
public class InputMethodHelper {
    private static final String TAG = "InputMethodHelper";
    
    private final Context context;
    private final InputMethodManager inputMethodManager;
    private final Handler mainHandler;
    
    public InputMethodHelper(Context context) {
        this.context = context;
        this.inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 显示输入法选择器的增强方法
     * 尝试多种方式确保在所有情况下都能工作
     */
    public void showInputMethodPicker() {
        // 方法1：直接调用InputMethodManager
        if (tryShowPicker()) {
            return;
        }
        
        // 方法2：延迟重试
        mainHandler.postDelayed(() -> {
            if (tryShowPicker()) {
                return;
            }
            
            // 方法3：使用Intent方式
            tryShowPickerWithIntent();
        }, 200);
    }
    
    /**
     * 尝试显示输入法选择器
     */
    private boolean tryShowPicker() {
        try {
            if (inputMethodManager != null) {
                inputMethodManager.showInputMethodPicker();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * 使用Intent方式显示输入法选择器
     */
    private void tryShowPickerWithIntent() {
        try {
            // 尝试直接Intent
            Intent pickerIntent = new Intent("android.settings.SHOW_INPUT_METHOD_PICKER");
            pickerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(pickerIntent);
            
            Toast.makeText(context, "正在打开输入法选择器...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            // 最后的兜底方案：打开输入法设置页面
            showInputMethodSettings();
        }
    }
    
    /**
     * 打开输入法设置页面
     */
    private void showInputMethodSettings() {
        try {
            Intent settingsIntent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settingsIntent);
            
            Toast.makeText(context, "已打开输入法设置页面", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "无法打开输入法设置", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 获取当前输入法ID
     */
    public String getCurrentInputMethodId() {
        try {
            return Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD
            );
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * 检查当前是否为指定包名的输入法
     */
    public boolean isCurrentInputMethod(String packageName) {
        String currentIME = getCurrentInputMethodId();
        return currentIME != null && currentIME.contains(packageName);
    }
}

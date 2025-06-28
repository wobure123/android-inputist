package com.inputassistant.universal.floating;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.os.Handler;
import android.os.Looper;

/**
 * 输入法切换工具类
 * 提供多种输入法切换方式的回退机制
 */
public class InputMethodSwitcher {
    private static final String TAG = "InputMethodSwitcher";
    
    private final Context context;
    private final InputMethodManager inputMethodManager;
    private final Handler mainHandler;
    
    public InputMethodSwitcher(Context context) {
        this.context = context;
        this.inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 尝试切换到指定输入法
     * @param targetInputMethodId 目标输入法ID
     * @param fallbackToSelector 是否在失败时回退到选择器
     * @return 是否成功启动切换过程
     */
    public boolean switchToInputMethod(String targetInputMethodId, boolean fallbackToSelector) {
        // 方法1: 尝试直接设置输入法（需要系统权限）
        if (tryDirectSwitch(targetInputMethodId)) {
            return true;
        }
        
        // 方法2: 尝试通过Intent切换（适用于部分ROM）
        if (tryIntentSwitch(targetInputMethodId)) {
            return true;
        }
        
        // 方法3: 如果允许，显示输入法选择器
        if (fallbackToSelector) {
            showInputMethodPicker();
            return true;
        }
        
        return false;
    }
    
    /**
     * 直接设置输入法（需要WRITE_SECURE_SETTINGS权限）
     */
    private boolean tryDirectSwitch(String inputMethodId) {
        try {
            Settings.Secure.putString(
                    context.getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD,
                    inputMethodId
            );
            return true;
        } catch (SecurityException e) {
            // 没有权限
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 通过Intent尝试切换输入法（部分ROM支持）
     */
    private boolean tryIntentSwitch(String inputMethodId) {
        try {
            Intent intent = new Intent("android.settings.INPUT_METHOD_SETTINGS");
            intent.putExtra("input_method_id", inputMethodId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            // Intent不支持或失败
            return false;
        }
    }
    
    /**
     * 显示输入法选择器
     */
    public void showInputMethodPicker() {
        try {
            if (inputMethodManager != null) {
                inputMethodManager.showInputMethodPicker();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
     * 检查指定输入法是否为当前输入法
     */
    public boolean isCurrentInputMethod(String inputMethodId) {
        String current = getCurrentInputMethodId();
        return current != null && current.equals(inputMethodId);
    }
    
    /**
     * 检查输入法是否包含指定包名
     */
    public boolean isCurrentInputMethodContains(String packageName) {
        String current = getCurrentInputMethodId();
        return current != null && current.contains(packageName);
    }
}

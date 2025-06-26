package com.inputassistant.universal.utils;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.List;

/**
 * 输入法管理工具类
 * 处理输入法切换相关功能
 */
public class InputMethodHelper {
    private static final String TAG = "InputMethodHelper";
    
    /**
     * 检查我们的输入法是否已启用
     */
    public static boolean isOurInputMethodEnabled(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        String packageName = context.getPackageName();
        
        List<InputMethodInfo> enabledMethods = imm.getEnabledInputMethodList();
        for (InputMethodInfo info : enabledMethods) {
            if (info.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查我们的输入法是否为当前输入法
     */
    public static boolean isOurInputMethodCurrent(Context context) {
        try {
            String currentIme = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD
            );
            return currentIme != null && currentIme.contains(context.getPackageName());
        } catch (Exception e) {
            Log.w(TAG, "Failed to check current input method", e);
            return false;
        }
    }
    
    /**
     * 显示输入法选择器
     */
    public static void showInputMethodPicker(Context context) {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
                Log.d(TAG, "Showing input method picker");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show input method picker", e);
        }
    }
    
    /**
     * 切换到我们的输入法
     * 注意：这个方法需要系统权限，在普通应用中只能引导用户手动切换
     */
    public static void switchToOurInputMethod(Context context) {
        try {
            // 显示输入法选择器让用户手动选择
            showInputMethodPicker(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to switch input method", e);
        }
    }
    
    /**
     * 获取我们的输入法Service的完整名称
     */
    public static String getOurInputMethodId(Context context) {
        return context.getPackageName() + "/.ime.TranslateInputMethodService";
    }
    
    /**
     * 检查输入法设置状态
     */
    public static InputMethodStatus checkInputMethodStatus(Context context) {
        boolean enabled = isOurInputMethodEnabled(context);
        boolean current = isOurInputMethodCurrent(context);
        
        if (!enabled) {
            return InputMethodStatus.NOT_ENABLED;
        } else if (!current) {
            return InputMethodStatus.ENABLED_NOT_CURRENT;
        } else {
            return InputMethodStatus.ENABLED_AND_CURRENT;
        }
    }
    
    /**
     * 输入法状态枚举
     */
    public enum InputMethodStatus {
        NOT_ENABLED,           // 未启用
        ENABLED_NOT_CURRENT,   // 已启用但非当前
        ENABLED_AND_CURRENT    // 已启用且为当前
    }
}

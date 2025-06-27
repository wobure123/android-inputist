package com.inputassistant.universal.ime;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.inputmethodservice.InputMethodService;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.inputassistant.universal.R;
import com.inputassistant.universal.api.GenericLLMApiClient;
import com.inputassistant.universal.floating.FloatingBallService;
import com.inputassistant.universal.model.Action;
import com.inputassistant.universal.repository.SettingsRepository;
import com.inputassistant.universal.utils.PermissionHelper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * 输入法服务 - 核心实现
 * 提供动态Action按钮界面，执行LLM处理
 */
public class TranslateInputMethodService extends InputMethodService {
    private static final String TAG = "TranslateInputMethodService";
    
    private SettingsRepository settingsRepository;
    private GenericLLMApiClient apiClient;
    private LinearLayout keyboardView;
    private TextView tvStatus;
    private String currentInputText = "";
    private String previousInputMethod = null; // 记录上一个输入法
    
    // 悬浮球服务相关
    private FloatingBallService floatingBallService;
    private boolean isFloatingBallServiceBound = false;
    
    // 服务连接
    private ServiceConnection floatingBallConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloatingBallService.FloatingBallBinder binder = 
                (FloatingBallService.FloatingBallBinder) service;
            floatingBallService = binder.getService();
            isFloatingBallServiceBound = true;
            Log.d(TAG, "FloatingBallService connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            floatingBallService = null;
            isFloatingBallServiceBound = false;
            Log.d(TAG, "FloatingBallService disconnected");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "InputMethod Service Created");
        
        try {
            settingsRepository = new SettingsRepository(this);
            apiClient = new GenericLLMApiClient();
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to initialize SettingsRepository", e);
        }
        
        // 初始化悬浮球服务
        initFloatingBallService();
    }

    @Override
    public View onCreateInputView() {
        Log.d(TAG, "Creating input view");
        
        // 加载输入法界面布局
        keyboardView = (LinearLayout) getLayoutInflater().inflate(R.layout.layout_translate_ime, null);
        tvStatus = keyboardView.findViewById(R.id.tv_ime_status);
        
        // 检查配置状态
        if (settingsRepository == null || !settingsRepository.isConfigured()) {
            showConfigurationError();
            return keyboardView;
        }
        
        // 动态创建Action按钮
        createActionButtons();
        
        // 设置快捷操作按钮
        setupQuickActionButtons();
        
        return keyboardView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        Log.d(TAG, "Starting input, restarting=" + restarting);
        
        // 🎯 显示悬浮球
        showFloatingBall();
        
        // 获取当前输入框的文本
        captureCurrentText();
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        Log.d(TAG, "Starting input view");
        
        // 记录当前的默认输入法（在切换到我们的输入法之前）
        recordPreviousInputMethod();
        
        // 每次显示时刷新文本
        captureCurrentText();
        updateStatusDisplay();
    }
    
    @Override
    public void onFinishInput() {
        super.onFinishInput();
        Log.d(TAG, "Finishing input");
        
        // 🎯 隐藏悬浮球
        hideFloatingBall();
    }

    /**
     * 记录上一个输入法
     */
    private void recordPreviousInputMethod() {
        try {
            String defaultIme = Settings.Secure.getString(
                getContentResolver(), 
                Settings.Secure.DEFAULT_INPUT_METHOD
            );
            
            // 如果当前默认输入法不是我们的，则记录它
            if (defaultIme != null && !defaultIme.contains(getPackageName())) {
                previousInputMethod = defaultIme;
                settingsRepository.savePreviousInputMethod(defaultIme);
                Log.d(TAG, "Recorded previous IME: " + defaultIme);
            } else {
                // 尝试从持久化存储中获取
                previousInputMethod = settingsRepository.getPreviousInputMethod();
                Log.d(TAG, "Loaded previous IME from storage: " + previousInputMethod);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to record previous input method", e);
        }
    }

    /**
     * 切换回上一个输入法
     */
    private void switchBackToPreviousInputMethod() {
        if (previousInputMethod != null) {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    // 延迟切换，确保文本更新完成
                    new android.os.Handler().postDelayed(() -> {
                        try {
                            // 先隐藏当前输入法
                            requestHideSelf(0);
                            
                            // 延迟一点再切换，给系统时间处理
                            new android.os.Handler().postDelayed(() -> {
                                try {
                                    // 通知用户如何切换回原输入法
                                    showToast("文本已更新，长按输入框可切换回原输入法");
                                } catch (Exception e) {
                                    Log.w(TAG, "Failed to show switch hint", e);
                                }
                            }, 500);
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to hide input method", e);
                        }
                    }, 1000); // 1秒后执行切换
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to switch back to previous input method", e);
            }
        } else {
            // 如果没有记录到上一个输入法，直接隐藏
            requestHideSelf(0);
            showToast("文本已更新，请手动切换输入法");
        }
    }

    /**
     * 捕获当前输入框的文本
     */
    private void captureCurrentText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            try {
                // 获取光标前的文本（最多1000字符）
                CharSequence textBefore = ic.getTextBeforeCursor(1000, 0);
                // 获取光标后的文本（最多1000字符）
                CharSequence textAfter = ic.getTextAfterCursor(1000, 0);
                
                StringBuilder fullText = new StringBuilder();
                if (textBefore != null) fullText.append(textBefore);
                if (textAfter != null) fullText.append(textAfter);
                
                currentInputText = fullText.toString();
                Log.d(TAG, "Captured text: " + currentInputText);
            } catch (Exception e) {
                Log.e(TAG, "Error capturing text", e);
                currentInputText = "";
            }
        }
    }

    /**
     * 设置快捷操作按钮
     */
    private void setupQuickActionButtons() {
        // 删除按钮
        Button btnDelete = keyboardView.findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                // 删除光标前的一个字符
                CharSequence selectedText = ic.getSelectedText(0);
                if (selectedText != null && selectedText.length() > 0) {
                    // 如果有选中文本，删除选中的文本
                    ic.commitText("", 1);
                } else {
                    // 删除光标前的一个字符
                    ic.deleteSurroundingText(1, 0);
                }
                // 重新捕获文本
                captureCurrentText();
                updateStatusDisplay();
            }
        });

        // 空格按钮
        Button btnSpace = keyboardView.findViewById(R.id.btn_space);
        btnSpace.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(" ", 1);
                captureCurrentText();
                updateStatusDisplay();
            }
        });

        // 换行按钮
        Button btnEnter = keyboardView.findViewById(R.id.btn_enter);
        btnEnter.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText("\n", 1);
                captureCurrentText();
                updateStatusDisplay();
            }
        });

        // 输入法切换按钮
        Button btnSwitchIme = keyboardView.findViewById(R.id.btn_switch_ime);
        btnSwitchIme.setOnClickListener(v -> showInputMethodPicker());
    }

    /**
     * 显示处理完成对话框
     */
    private void showCompletionDialog() {
        // 延迟显示，确保文本更新完成
        new android.os.Handler().postDelayed(() -> {
            try {
                showToast("✅ 处理完成！点击'切换'按钮可快速切换输入法");
                
                // 移除自动隐藏功能，让用户自主选择何时切换
                // 用户可以：
                // 1. 点击"切换"按钮快速切换输入法
                // 2. 继续使用基础编辑功能（删除、空格、换行）
                // 3. 执行其他动作
                
            } catch (Exception e) {
                Log.w(TAG, "Failed to show completion dialog", e);
            }
        }, 500);
    }

    /**
     * 显示输入法选择器
     */
    private void showInputMethodPicker() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
                Log.d(TAG, "Showing input method picker");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show input method picker", e);
            showToast("无法显示输入法选择器");
        }
    }

    /**
     * 动态创建Action按钮
     */
    private void createActionButtons() {
        LinearLayout buttonsContainer = keyboardView.findViewById(R.id.ll_action_buttons);
        buttonsContainer.removeAllViews(); // 清除现有按钮
        
        List<Action> actions = settingsRepository.getActions();
        
        if (actions.isEmpty()) {
            // 没有可用的Action
            TextView emptyView = new TextView(this);
            emptyView.setText("暂无可用动作\n请在主应用中添加动作");
            emptyView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyView.setPadding(16, 16, 16, 16);
            buttonsContainer.addView(emptyView);
            return;
        }
        
        // 为每个Action创建按钮
        for (Action action : actions) {
            Button actionButton = createActionButton(action);
            buttonsContainer.addView(actionButton);
        }
    }

    /**
     * 创建单个Action按钮
     */
    private Button createActionButton(Action action) {
        Button button = new Button(this);
        button.setText(action.getName());
        button.setOnClickListener(v -> executeAction(action));
        
        // 设置按钮样式
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 4, 8, 4);
        button.setLayoutParams(params);
        
        return button;
    }

    /**
     * 执行Action
     */
    private void executeAction(Action action) {
        Log.d(TAG, "Executing action: " + action.getName());
        
        // 重新捕获当前文本
        captureCurrentText();
        
        if (TextUtils.isEmpty(currentInputText)) {
            showToast("输入框为空，无法处理");
            return;
        }
        
        // 更新状态显示
        tvStatus.setText("正在处理: " + action.getName() + "...");
        
        // 调用API
        String baseUrl = settingsRepository.getApiBaseUrl();
        String apiKey = settingsRepository.getApiKey();
        String modelName = settingsRepository.getModelName();
        
        apiClient.executeRequest(
                baseUrl,
                apiKey,
                modelName,
                action.getSystemPrompt(),
                currentInputText,
                new GenericLLMApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "API call successful: " + result);
                        updateInputText(result);
                        tvStatus.setText("处理完成");
                        
                        // 显示完成提示，并提供快捷切换选项
                        showCompletionDialog();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "API call failed: " + error);
                        showToast("处理失败: " + error);
                        tvStatus.setText("处理失败");
                    }
                }
        );
    }

    /**
     * 更新输入框文本
     */
    private void updateInputText(String processedText) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            try {
                // 根据设置决定文本处理模式
                boolean isReplaceMode = settingsRepository.isReplaceMode();
                String finalText;
                
                if (isReplaceMode) {
                    // 替换模式：仅保留AI回答
                    finalText = processedText;
                } else {
                    // 拼接模式：原文 + 分隔符 + AI回答
                    finalText = currentInputText + "\n======\n" + processedText;
                }
                
                // 开始批量编辑以提高性能
                ic.beginBatchEdit();
                
                // 方法1：尝试删除所有文本并重新插入
                try {
                    // 获取光标前后的文本长度
                    CharSequence textBefore = ic.getTextBeforeCursor(10000, 0);
                    CharSequence textAfter = ic.getTextAfterCursor(10000, 0);
                    
                    int beforeLength = textBefore != null ? textBefore.length() : 0;
                    int afterLength = textAfter != null ? textAfter.length() : 0;
                    
                    // 删除光标前的文本
                    if (beforeLength > 0) {
                        ic.deleteSurroundingText(beforeLength, 0);
                    }
                    
                    // 删除光标后的文本
                    if (afterLength > 0) {
                        ic.deleteSurroundingText(0, afterLength);
                    }
                    
                    // 插入新文本
                    ic.commitText(finalText, 1);
                    
                } catch (Exception e) {
                    Log.w(TAG, "Method 1 failed, trying method 2", e);
                    
                    // 方法2：尝试使用setComposingText
                    try {
                        ic.setComposingText(finalText, 1);
                        ic.finishComposingText();
                    } catch (Exception e2) {
                        Log.w(TAG, "Method 2 failed, trying method 3", e2);
                        
                        // 方法3：直接commitText，让系统处理
                        ic.commitText(finalText, 1);
                    }
                }
                
                // 结束批量编辑
                ic.endBatchEdit();
                
                Log.d(TAG, "Text updated successfully");
                
                // 根据模式显示不同的提示信息
                String mode = settingsRepository.isReplaceMode() ? "替换" : "拼接";
                showToast("文本已更新（" + mode + "模式）");
            } catch (Exception e) {
                Log.e(TAG, "Error updating text", e);
                showToast("更新文本失败");
            }
        }
    }

    /**
     * 显示配置错误
     */
    private void showConfigurationError() {
        LinearLayout buttonsContainer = keyboardView.findViewById(R.id.ll_action_buttons);
        buttonsContainer.removeAllViews();
        
        TextView errorView = new TextView(this);
        errorView.setText("❌ 请先在主应用中配置API设置");
        errorView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        errorView.setPadding(16, 16, 16, 16);
        buttonsContainer.addView(errorView);
        
        tvStatus.setText("未配置");
    }

    /**
     * 更新状态显示
     */
    private void updateStatusDisplay() {
        if (tvStatus != null) {
            int textLength = currentInputText.length();
            tvStatus.setText(String.format("已捕获 %d 字符", textLength));
        }
    }

    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 初始化悬浮球服务
     */
    private void initFloatingBallService() {
        // 检查权限
        if (!PermissionHelper.hasOverlayPermission(this)) {
            Log.w(TAG, "No overlay permission, skipping floating ball service");
            return;
        }
        
        try {
            // 启动并绑定悬浮球服务
            Intent intent = new Intent(this, FloatingBallService.class);
            startService(intent);
            bindService(intent, floatingBallConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "FloatingBallService started and bound");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start FloatingBallService", e);
        }
    }
    
    /**
     * 显示悬浮球
     */
    private void showFloatingBall() {
        if (isFloatingBallServiceBound && floatingBallService != null) {
            floatingBallService.showFloatingBall();
            Log.d(TAG, "Floating ball shown");
        }
    }
    
    /**
     * 隐藏悬浮球
     */
    private void hideFloatingBall() {
        if (isFloatingBallServiceBound && floatingBallService != null) {
            floatingBallService.hideFloatingBall();
            Log.d(TAG, "Floating ball hidden");
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "InputMethod Service destroyed");
        
        // 清理悬浮球服务
        if (isFloatingBallServiceBound) {
            try {
                unbindService(floatingBallConnection);
                isFloatingBallServiceBound = false;
            } catch (Exception e) {
                Log.w(TAG, "Failed to unbind FloatingBallService", e);
            }
        }
        
        // 停止悬浮球服务
        try {
            Intent intent = new Intent(this, FloatingBallService.class);
            stopService(intent);
        } catch (Exception e) {
            Log.w(TAG, "Failed to stop FloatingBallService", e);
        }
    }
}

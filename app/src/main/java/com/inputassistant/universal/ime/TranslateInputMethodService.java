package com.inputassistant.universal.ime;

import android.inputmethodservice.InputMethodService;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.inputassistant.universal.R;
import com.inputassistant.universal.api.GenericLLMApiClient;
import com.inputassistant.universal.model.Action;
import com.inputassistant.universal.repository.SettingsRepository;

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
        
        return keyboardView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        Log.d(TAG, "Starting input, restarting=" + restarting);
        
        // 获取当前输入框的文本
        captureCurrentText();
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        Log.d(TAG, "Starting input view");
        
        // 每次显示时刷新文本
        captureCurrentText();
        updateStatusDisplay();
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
                // 构建最终文本：原文 + 分隔符 + 处理后文本
                String finalText = currentInputText + "\n---\n" + processedText;
                
                // 选择全部文本并替换
                ic.selectAll();
                ic.commitText(finalText, 1);
                
                Log.d(TAG, "Text updated successfully");
                showToast("文本已更新");
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
}

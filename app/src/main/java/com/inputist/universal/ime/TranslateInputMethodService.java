package com.inputist.universal.ime;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.inputist.universal.R;
import com.inputist.universal.api.GenericLLMApiClient;
import com.inputist.universal.model.Action;
import com.inputist.universal.model.ApiConfig;
import com.inputist.universal.repository.SettingsRepository;

import java.util.List;

/**
 * 通用输入改写助手 - 输入法服务
 */
public class TranslateInputMethodService extends InputMethodService {
    private static final String TAG = "TranslateIME";
    
    private View inputView;
    private LinearLayout actionButtonContainer;
    private TextView statusText;
    private ProgressBar progressBar;
    
    private SettingsRepository settingsRepository;
    private GenericLLMApiClient apiClient;
    private List<Action> actions;
    private String currentInputText;
    
    private boolean isProcessing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TranslateInputMethodService onCreate");
        
        settingsRepository = SettingsRepository.getInstance(this);
        apiClient = new GenericLLMApiClient();
    }

    @Override
    public View onCreateInputView() {
        Log.d(TAG, "onCreateInputView");
        
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inputView = inflater.inflate(R.layout.layout_translate_ime, null);
        
        initializeViews();
        loadActionsAndCreateButtons();
        
        return inputView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        Log.d(TAG, "onStartInput, restarting: " + restarting);
        
        // 每次开始输入时重新读取当前文本
        readCurrentInputText();
        updateUI();
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        Log.d(TAG, "onStartInputView, restarting: " + restarting);
        
        // 重新加载Actions（可能用户在设置中有更改）
        loadActionsAndCreateButtons();
        readCurrentInputText();
        updateUI();
    }

    /**
     * 初始化视图组件
     */
    private void initializeViews() {
        actionButtonContainer = inputView.findViewById(R.id.action_button_container);
        statusText = inputView.findViewById(R.id.status_text);
        progressBar = inputView.findViewById(R.id.progress_bar);
        
        // 设置关闭按钮
        Button closeButton = inputView.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(v -> requestHideSelf(0));
        
        // 设置刷新按钮
        Button refreshButton = inputView.findViewById(R.id.btn_refresh);
        refreshButton.setOnClickListener(v -> {
            readCurrentInputText();
            updateUI();
        });
    }

    /**
     * 加载Actions并创建对应的按钮
     */
    private void loadActionsAndCreateButtons() {
        actions = settingsRepository.getActions();
        actionButtonContainer.removeAllViews();
        
        if (actions.isEmpty()) {
            // 显示提示信息
            TextView emptyHint = new TextView(this);
            emptyHint.setText("暂无可用功能，请在设置中添加");
            emptyHint.setPadding(16, 16, 16, 16);
            actionButtonContainer.addView(emptyHint);
            return;
        }
        
        // 为每个Action创建按钮
        for (Action action : actions) {
            Button actionButton = createActionButton(action);
            actionButtonContainer.addView(actionButton);
        }
    }

    /**
     * 创建Action按钮
     */
    private Button createActionButton(Action action) {
        Button button = new Button(this);
        button.setText(action.getName());
        button.setOnClickListener(v -> executeAction(action));
        
        // 设置按钮样式
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 4, 8, 4);
        button.setLayoutParams(params);
        
        return button;
    }

    /**
     * 读取当前输入框的文本
     */
    private void readCurrentInputText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            currentInputText = "";
            return;
        }
        
        try {
            // 尝试获取输入框中的所有文本
            CharSequence beforeCursor = ic.getTextBeforeCursor(10000, 0);
            CharSequence afterCursor = ic.getTextAfterCursor(10000, 0);
            
            StringBuilder fullText = new StringBuilder();
            if (beforeCursor != null) {
                fullText.append(beforeCursor);
            }
            if (afterCursor != null) {
                fullText.append(afterCursor);
            }
            
            currentInputText = fullText.toString();
            Log.d(TAG, "Current input text: " + currentInputText);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to read input text", e);
            currentInputText = "";
        }
    }

    /**
     * 执行指定的Action
     */
    private void executeAction(Action action) {
        if (isProcessing) {
            showToast("正在处理中，请稍候...");
            return;
        }
        
        ApiConfig apiConfig = settingsRepository.getApiConfig();
        if (!apiConfig.isValid()) {
            showToast("请先在设置中配置API");
            return;
        }
        
        if (TextUtils.isEmpty(currentInputText)) {
            showToast("输入框为空，无法处理");
            return;
        }
        
        Log.d(TAG, "Executing action: " + action.getName());
        Log.d(TAG, "System prompt: " + action.getSystemPrompt());
        Log.d(TAG, "User text: " + currentInputText);
        
        setProcessing(true);
        
        apiClient.executeRequest(apiConfig, action.getSystemPrompt(), currentInputText, 
                new GenericLLMApiClient.ApiCallback() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "API success: " + result);
                handleApiSuccess(result);
                setProcessing(false);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "API error: " + error);
                showToast("处理失败: " + error);
                setProcessing(false);
            }
        });
    }

    /**
     * 处理API成功响应
     */
    private void handleApiSuccess(String result) {
        if (TextUtils.isEmpty(result)) {
            showToast("API返回空结果");
            return;
        }
        
        // 拼接原文和处理结果
        String combinedText = currentInputText + "\n---\n" + result;
        
        // 替换输入框内容
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            try {
                // 选择所有文本并替换
                ic.selectAll();
                ic.commitText(combinedText, 1);
                
                showToast("处理完成");
                Log.d(TAG, "Text replaced successfully");
                
                // 更新当前文本
                currentInputText = combinedText;
                updateUI();
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to commit text", e);
                showToast("文本替换失败");
            }
        } else {
            showToast("无法访问输入框");
        }
    }

    /**
     * 设置处理状态
     */
    private void setProcessing(boolean processing) {
        isProcessing = processing;
        updateUI();
    }

    /**
     * 更新UI状态
     */
    private void updateUI() {
        if (progressBar != null) {
            progressBar.setVisibility(isProcessing ? View.VISIBLE : View.GONE);
        }
        
        if (statusText != null) {
            if (isProcessing) {
                statusText.setText("正在处理...");
            } else if (TextUtils.isEmpty(currentInputText)) {
                statusText.setText("输入框为空");
            } else {
                String preview = currentInputText.length() > 50 ? 
                        currentInputText.substring(0, 50) + "..." : currentInputText;
                statusText.setText("当前文本: " + preview);
            }
        }
        
        // 更新按钮状态
        for (int i = 0; i < actionButtonContainer.getChildCount(); i++) {
            View child = actionButtonContainer.getChildAt(i);
            if (child instanceof Button) {
                child.setEnabled(!isProcessing && !TextUtils.isEmpty(currentInputText));
            }
        }
    }

    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "TranslateInputMethodService onDestroy");
        
        if (apiClient != null) {
            apiClient.cancelAllRequests();
        }
    }
}

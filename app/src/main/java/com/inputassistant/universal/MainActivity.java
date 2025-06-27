package com.inputassistant.universal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.inputassistant.universal.adapter.ActionAdapter;
import com.inputassistant.universal.model.Action;
import com.inputassistant.universal.repository.SettingsRepository;
import com.inputassistant.universal.utils.PermissionHelper;
import com.inputassistant.universal.utils.AccessibilityHelper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * 主活动 - 配置中心
 * 管理API设置和Action列表
 */
public class MainActivity extends AppCompatActivity implements ActionAdapter.OnActionClickListener {
    private static final int REQUEST_ACTION_EDIT = 1;
    
    private EditText etApiBaseUrl;
    private EditText etApiKey;
    private EditText etModelName;
    private Button btnSaveApiSettings;
    private Button btnSetupIME;
    private Button btnFloatingBallSettings;  // 悬浮球设置按钮
    private RecyclerView rvActions;
    private Button fabAddAction;  // 改为 Button 类型
    private TextView tvStatus;
    private TextView tvMainTitle;  // 主标题视图
    private Switch switchTextMode;  // 文本处理模式切换开关
    private TextView tvModeDescription;  // 模式描述文本
    
    private SettingsRepository settingsRepository;
    private ActionAdapter actionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initRepository();
        setupRecyclerView();
        loadSettings();
        setupClickListeners();
        updateStatus();
    }

    private void initViews() {
        etApiBaseUrl = findViewById(R.id.et_api_base_url);
        etApiKey = findViewById(R.id.et_api_key);
        etModelName = findViewById(R.id.et_model_name);
        btnSaveApiSettings = findViewById(R.id.btn_save_api_settings);
        btnSetupIME = findViewById(R.id.btn_setup_ime);
        btnFloatingBallSettings = findViewById(R.id.btn_floating_ball_settings);
        rvActions = findViewById(R.id.rv_actions);
        fabAddAction = findViewById(R.id.fab_add_action);
        tvStatus = findViewById(R.id.tv_status);
        tvMainTitle = findViewById(R.id.tv_main_title);
        switchTextMode = findViewById(R.id.switch_text_mode);
        tvModeDescription = findViewById(R.id.tv_mode_description);
        
        // 动态设置标题，包含版本号
        setMainTitleWithVersion();
    }

    private void initRepository() {
        try {
            settingsRepository = new SettingsRepository(this);
        } catch (GeneralSecurityException | IOException e) {
            showError("初始化安全存储失败: " + e.getMessage());
            finish();
        }
    }

    private void setupRecyclerView() {
        actionAdapter = new ActionAdapter(this);
        rvActions.setLayoutManager(new LinearLayoutManager(this));
        rvActions.setAdapter(actionAdapter);
        
        loadActions();
    }

    private void loadSettings() {
        etApiBaseUrl.setText(settingsRepository.getApiBaseUrl());
        etApiKey.setText(settingsRepository.getApiKey());
        etModelName.setText(settingsRepository.getModelName());
        
        // 初始化文本处理模式设置
        initTextModeSettings();
    }

    private void initTextModeSettings() {
        // 初始化开关状态
        boolean isReplaceMode = settingsRepository.isReplaceMode();
        switchTextMode.setChecked(isReplaceMode);
        updateModeDescription();
    }

    private void updateModeDescription() {
        boolean isReplaceMode = switchTextMode.isChecked();
        if (isReplaceMode) {
            tvModeDescription.setText("替换模式：仅保留AI回答，替换原文");
        } else {
            tvModeDescription.setText("拼接模式：原文 + 分割线 + AI回答");
        }
    }

    private void loadActions() {
        List<Action> actions = settingsRepository.getActions();
        actionAdapter.updateActions(actions);
    }

    private void setupClickListeners() {
        btnSaveApiSettings.setOnClickListener(v -> saveApiSettings());
        btnSetupIME.setOnClickListener(v -> openIMESettings());
        btnFloatingBallSettings.setOnClickListener(v -> openFloatingBallSettings());
        fabAddAction.setOnClickListener(v -> openActionEditor(null));
        
        // 文本处理模式切换监听
        switchTextMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsRepository.setTextProcessingMode(isChecked);
            updateModeDescription();
            showToast(isChecked ? "已切换到替换模式" : "已切换到拼接模式");
        });
    }

    private void saveApiSettings() {
        String baseUrl = etApiBaseUrl.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();
        String modelName = etModelName.getText().toString().trim();

        if (baseUrl.isEmpty() || apiKey.isEmpty()) {
            showError("请填写API基础URL和API密钥");
            return;
        }

        if (modelName.isEmpty()) {
            modelName = "gpt-3.5-turbo";
        }

        settingsRepository.saveApiBaseUrl(baseUrl);
        settingsRepository.saveApiKey(apiKey);
        settingsRepository.saveModelName(modelName);

        Toast.makeText(this, "API设置已保存", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void openIMESettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            startActivity(intent);
            
            new AlertDialog.Builder(this)
                    .setTitle("输入法设置指南")
                    .setMessage("请在输入法设置中：\n\n1. 启用「通用输入改写助手」\n2. 设置为默认输入法（可选）\n3. 授予必要权限")
                    .setPositiveButton("知道了", null)
                    .show();
        } catch (Exception e) {
            showError("无法打开输入法设置");
        }
    }

    private void openActionEditor(Action action) {
        Intent intent = new Intent(this, ActionEditorActivity.class);
        if (action != null) {
            intent.putExtra("action_id", action.getId());
            intent.putExtra("action_name", action.getName());
            intent.putExtra("action_system_prompt", action.getSystemPrompt());
        }
        startActivityForResult(intent, REQUEST_ACTION_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ACTION_EDIT && resultCode == RESULT_OK) {
            loadActions(); // 刷新Action列表
        }
    }

    @Override
    public void onActionEdit(Action action) {
        openActionEditor(action);
    }

    @Override
    public void onActionDelete(Action action) {
        new AlertDialog.Builder(this)
                .setTitle("删除动作")
                .setMessage("确定要删除动作「" + action.getName() + "」吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    settingsRepository.deleteAction(action.getId());
                    loadActions();
                    Toast.makeText(this, "动作已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateStatus() {
        boolean isConfigured = settingsRepository.isConfigured();
        boolean isIMEEnabled = isIMEEnabled();
        
        StringBuilder status = new StringBuilder();
        
        if (isConfigured) {
            status.append("✅ API已配置");
        } else {
            status.append("❌ API未配置");
        }
        
        status.append("\n");
        
        if (isIMEEnabled) {
            status.append("✅ 输入法已启用");
        } else {
            status.append("❌ 输入法未启用");
        }
        
        tvStatus.setText(status.toString());
    }

    private boolean isIMEEnabled() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        String packageName = getPackageName();
        return imm.getEnabledInputMethodList().stream()
                .anyMatch(info -> info.getPackageName().equals(packageName));
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("错误")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 打开悬浮球设置
     */
    private void openFloatingBallSettings() {
        // 首先检查悬浮窗权限
        if (!PermissionHelper.hasOverlayPermission(this)) {
            showFloatingBallPermissionDialog();
            return;
        }
        
        // 检查辅助功能权限
        if (!AccessibilityHelper.isOurAccessibilityServiceEnabled(this)) {
            showAccessibilityPermissionDialog();
            return;
        }
        
        // 权限都已获得，显示功能设置对话框
        showFloatingBallSettingsDialog();
    }
    
    /**
     * 显示悬浮球设置对话框
     */
    private void showFloatingBallSettingsDialog() {
        boolean isEnabled = settingsRepository.isFloatingBallEnabled();
        
        new AlertDialog.Builder(this)
                .setTitle("🎈 悬浮球功能设置")
                .setMessage("当前状态：" + (isEnabled ? "✅ 已启用" : "❌ 已禁用") + "\n\n" +
                           "🎯 功能说明：\n" +
                           "• 在任何应用的输入框激活时自动显示悬浮球\n" +
                           "• 点击悬浮球快速切换到输入法助手\n" +
                           "• 输入完成后悬浮球自动隐藏\n" +
                           "• 支持拖拽和磁性吸附\n\n" +
                           "✨ 使用提示：\n" +
                           "1. 在任意应用中点击输入框\n" +
                           "2. 悬浮球会自动出现\n" +
                           "3. 点击悬浮球即可快速切换输入法")
                .setPositiveButton(isEnabled ? "禁用悬浮球" : "启用悬浮球", (dialog, which) -> {
                    toggleFloatingBall(!isEnabled);
                })
                .setNeutralButton("权限设置", (dialog, which) -> {
                    showPermissionManagementDialog();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 切换悬浮球启用状态
     */
    private void toggleFloatingBall(boolean enable) {
        settingsRepository.setFloatingBallEnabled(enable);
        showToast(enable ? "悬浮球已启用" : "悬浮球已禁用");
    }
    
    /**
     * 显示权限管理对话框
     */
    private void showPermissionManagementDialog() {
        boolean hasOverlay = PermissionHelper.hasOverlayPermission(this);
        boolean hasAccessibility = AccessibilityHelper.isOurAccessibilityServiceEnabled(this);
        
        String message = "权限状态检查：\n\n" +
                        "🔑 悬浮窗权限：" + (hasOverlay ? "✅ 已授予" : "❌ 未授予") + "\n" +
                        "🔑 辅助功能权限：" + (hasAccessibility ? "✅ 已启用" : "❌ 未启用") + "\n\n" +
                        "悬浮球功能需要两个权限都启用才能正常工作。";
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("权限管理")
                .setMessage(message);
                
        if (!hasOverlay) {
            builder.setPositiveButton("设置悬浮窗权限", (dialog, which) -> {
                PermissionHelper.openOverlaySettings(this);
            });
        }
        
        if (!hasAccessibility) {
            builder.setNeutralButton("设置辅助功能", (dialog, which) -> {
                AccessibilityHelper.openAccessibilitySettings(this);
            });
        }
        
        builder.setNegativeButton("取消", null).show();
    }
    
    /**
     * 显示辅助功能权限说明对话框
     */
    private void showAccessibilityPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("需要辅助功能权限")
                .setMessage("悬浮球功能需要辅助功能权限来检测全局输入框状态。\n\n" +
                           "✨ 开启后，您就可以：\n" +
                           "• 在任何应用中自动检测输入框\n" +
                           "• 享受智能悬浮球便捷体验\n" +
                           "• 无需手动切换输入法\n\n" +
                           "🔒 隐私说明：\n" +
                           "我们只检测输入框状态，不收集任何输入内容。")
                .setPositiveButton("去设置", (dialog, which) -> {
                    AccessibilityHelper.openAccessibilitySettings(this);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示悬浮球权限说明对话框
     */
    private void showFloatingBallPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("需要悬浮窗权限")
                .setMessage("悬浮球功能需要悬浮窗权限才能正常工作。\n\n" +
                           "开启权限后，您就可以：\n" +
                           "• 在任意应用中快速调用输入法助手\n" +
                           "• 享受更便捷的文本处理体验\n\n" +
                           "请点击\"去设置\"开启权限。")
                .setPositiveButton("去设置", (dialog, which) -> {
                    PermissionHelper.openOverlaySettings(this);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void setMainTitleWithVersion() {
        try {
            // 获取版本名称
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;
            
            // 设置带版本号的标题
            String titleWithVersion = getString(R.string.main_title) + " v" + versionName;
            tvMainTitle.setText(titleWithVersion);
            
        } catch (Exception e) {
            // 如果获取版本号失败，使用默认标题
            tvMainTitle.setText(R.string.main_title);
        }
    }
}

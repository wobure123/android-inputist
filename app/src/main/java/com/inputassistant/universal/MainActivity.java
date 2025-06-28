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
import com.inputassistant.universal.floating.FloatingBallService;
import com.inputassistant.universal.model.Action;
import com.inputassistant.universal.repository.SettingsRepository;

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
    private RecyclerView rvActions;
    private Button fabAddAction;  // 改为 Button 类型
    private TextView tvStatus;
    private Switch switchTextMode;  // 文本处理模式切换开关
    private TextView tvModeDescription;  // 模式描述文本
    private Switch switchFloatingBall;  // 悬浮球开关
    private Button btnFloatingBallPermission;  // 悬浮球权限按钮
    
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
        rvActions = findViewById(R.id.rv_actions);
        fabAddAction = findViewById(R.id.fab_add_action);
        tvStatus = findViewById(R.id.tv_status);
        switchTextMode = findViewById(R.id.switch_text_mode);
        tvModeDescription = findViewById(R.id.tv_mode_description);
        switchFloatingBall = findViewById(R.id.switch_floating_ball);
        btnFloatingBallPermission = findViewById(R.id.btn_floating_ball_permission);
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
        
        // 初始化悬浮球设置
        initFloatingBallSettings();
    }

    private void initTextModeSettings() {
        // 初始化开关状态
        boolean isReplaceMode = settingsRepository.isReplaceMode();
        switchTextMode.setChecked(isReplaceMode);
        updateModeDescription();
    }

    private void initFloatingBallSettings() {
        // 初始化悬浮球开关状态
        boolean isFloatingBallEnabled = settingsRepository.isFloatingBallEnabled();
        switchFloatingBall.setChecked(isFloatingBallEnabled);
        
        // 更新权限按钮状态
        updateFloatingBallPermissionButton();
    }

    private void updateFloatingBallPermissionButton() {
        if (Settings.canDrawOverlays(this)) {
            btnFloatingBallPermission.setText("✓ 悬浮权限已授予");
            btnFloatingBallPermission.setEnabled(false);
        } else {
            btnFloatingBallPermission.setText("授予悬浮权限");
            btnFloatingBallPermission.setEnabled(true);
        }
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
        fabAddAction.setOnClickListener(v -> openActionEditor(null));
        
        // 文本处理模式切换监听
        switchTextMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsRepository.setTextProcessingMode(isChecked);
            updateModeDescription();
            showToast(isChecked ? "已切换到替换模式" : "已切换到拼接模式");
        });
        
        // 悬浮球开关监听
        switchFloatingBall.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 启用悬浮球
                if (Settings.canDrawOverlays(this)) {
                    enableFloatingBall();
                } else {
                    // 没有权限，取消勾选并提示用户授权
                    switchFloatingBall.setChecked(false);
                    showToast("请先授予悬浮权限");
                }
            } else {
                // 禁用悬浮球
                disableFloatingBall();
            }
        });
        
        // 悬浮球权限按钮监听
        btnFloatingBallPermission.setOnClickListener(v -> requestFloatingBallPermission());
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
        } else if (requestCode == REQUEST_FLOATING_BALL_PERMISSION) {
            // 悬浮球权限请求结果
            if (Settings.canDrawOverlays(this)) {
                updateFloatingBallPermissionButton();
                showToast("悬浮权限已授予，现在可以启用悬浮球功能");
            } else {
                showToast("未授予悬浮权限，无法使用悬浮球功能");
            }
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
     * 启用悬浮球（前台服务版本）
     */
    private void enableFloatingBall() {
        try {
            settingsRepository.setFloatingBallEnabled(true);
            Intent serviceIntent = new Intent(this, FloatingBallService.class);
            
            // Android 8.0+ 需要使用 startForegroundService
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
                showToast("悬浮球已启用（前台服务模式）");
            } else {
                startService(serviceIntent);
                showToast("悬浮球已启用");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showToast("启动悬浮球失败: " + e.getMessage());
        }
    }

    /**
     * 禁用悬浮球
     */
    private void disableFloatingBall() {
        settingsRepository.setFloatingBallEnabled(false);
        Intent serviceIntent = new Intent(this, FloatingBallService.class);
        stopService(serviceIntent);
        showToast("悬浮球已禁用");
    }

    /**
     * 请求悬浮球权限
     */
    private void requestFloatingBallPermission() {
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("需要悬浮权限")
                    .setMessage("悬浮球功能需要在其他应用上层显示的权限，请在接下来的设置页面中允许此权限。")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_FLOATING_BALL_PERMISSION);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    private static final int REQUEST_FLOATING_BALL_PERMISSION = 2;

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        // 更新悬浮球权限按钮状态
        updateFloatingBallPermissionButton();
    }
}

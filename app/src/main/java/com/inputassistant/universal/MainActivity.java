package com.inputassistant.universal;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.inputassistant.universal.adapter.ActionAdapter;
import com.inputassistant.universal.floating.FloatingBallService;
import com.inputassistant.universal.floating.KeyboardAwareFloatingBallService;
import com.inputassistant.universal.model.Action;
import com.inputassistant.universal.repository.SettingsRepository;
import com.inputassistant.universal.utils.PermissionHelper;
import com.inputassistant.universal.utils.InputMethodHelper;

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
    
    // 权限状态跟踪，避免重复提示
    private boolean lastOverlayPermissionState = false;

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
        
        // 初始化权限状态
        initPermissionStates();
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
        
        // 权限已获得，显示功能设置对话框
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
                .setNeutralButton("交互测试", (dialog, which) -> {
                    testFloatingBallInteraction();
                })
                .setNegativeButton("服务检查", (dialog, which) -> {
                    checkFloatingBallServices();
                })
                .show();
    }
    
    /**
     * 切换悬浮球启用状态
     */
    private void toggleFloatingBall(boolean enable) {
        settingsRepository.setFloatingBallEnabled(enable);
        
        if (enable) {
            // 启用悬浮球时，启动新的键盘感知服务
            Intent serviceIntent = new Intent(this, KeyboardAwareFloatingBallService.class);
            startService(serviceIntent);
            showToast("✅ 悬浮球已启用，请到其他应用测试输入框");
        } else {
            // 禁用悬浮球时，停止所有相关服务
            Intent oldServiceIntent = new Intent(this, FloatingBallService.class);
            stopService(oldServiceIntent);
            
            Intent newServiceIntent = new Intent(this, KeyboardAwareFloatingBallService.class);
            stopService(newServiceIntent);
            
            showToast("❌ 悬浮球已禁用");
        }
    }
    
    /**
     * 显示权限管理对话框
     */
    private void showPermissionManagementDialog() {
        boolean hasOverlay = PermissionHelper.hasOverlayPermission(this);
        
        String message = "权限状态检查：\n\n" +
                        "🔑 悬浮窗权限：" + (hasOverlay ? "✅ 已授予" : "❌ 未授予") + "\n\n" +
                        "悬浮球功能只需要悬浮窗权限即可正常工作。\n\n" +
                        "💡 提示：开启权限后返回应用会自动检测并提示成功。";
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("权限管理")
                .setMessage(message);
                
        if (!hasOverlay) {
            builder.setPositiveButton("设置悬浮窗权限", (dialog, which) -> {
                PermissionHelper.openOverlaySettings(this);
            });
        } else {
            builder.setPositiveButton("确定", null);
        }
        
        builder.setNegativeButton("取消", null).show();
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
                           "💡 操作提示：开启后返回应用会自动检测并提示成功。")
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
        
        // 检查权限状态变化，给出成功提示
        checkPermissionStatusAndNotify();
    }
    
    /**
     * 检查权限状态并给出相应提示
     */
    private void checkPermissionStatusAndNotify() {
        boolean hasOverlay = PermissionHelper.hasOverlayPermission(this);
        
        // 检查悬浮窗权限是否刚刚获得
        if (hasOverlay && !lastOverlayPermissionState) {
            showToast("✅ 悬浮窗权限已授予");
            
            boolean isFloatingBallEnabled = settingsRepository.isFloatingBallEnabled();
            if (!isFloatingBallEnabled) {
                // 权限有了，但功能未启用，延迟显示对话框避免与toast冲突
                postDelayed(() -> showPermissionSuccessDialog(), 1000);
            } else {
                // 一切就绪
                postDelayed(() -> showToast("🎈 悬浮球功能已完全启用！"), 500);
            }
        }
        
        // 更新权限状态  
        lastOverlayPermissionState = hasOverlay;
    }
    
    /**
     * 延迟执行任务
     */
    private void postDelayed(Runnable runnable, long delayMillis) {
        new android.os.Handler(getMainLooper()).postDelayed(runnable, delayMillis);
    }
    
    /**
     * 显示权限配置成功对话框
     */
    private void showPermissionSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🎉 权限配置完成")
                .setMessage("恭喜！悬浮窗权限已配置完成：\n\n" +
                           "✅ 悬浮窗权限：已授予\n\n" +
                           "现在可以启用悬浮球功能了！")
                .setPositiveButton("启用悬浮球", (dialog, which) -> {
                    settingsRepository.setFloatingBallEnabled(true);
                    showToast("🎈 悬浮球功能已启用！");
                })
                .setNegativeButton("稍后启用", null)
                .show();
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
    
    /**
     * 初始化权限状态
     */
    private void initPermissionStates() {
        lastOverlayPermissionState = PermissionHelper.hasOverlayPermission(this);
    }
    
    /**
     * 测试悬浮球功能
     */
    private void testFloatingBall() {
        boolean hasOverlay = PermissionHelper.hasOverlayPermission(this);
        boolean isEnabled = settingsRepository.isFloatingBallEnabled();
        
        StringBuilder result = new StringBuilder();
        result.append("🔍 悬浮球功能测试结果：\n\n");
        result.append("🔑 悬浮窗权限：").append(hasOverlay ? "✅ 已授予" : "❌ 未授予").append("\n");
        result.append("🎈 悬浮球功能：").append(isEnabled ? "✅ 已启用" : "❌ 已禁用").append("\n\n");
        
        if (hasOverlay && isEnabled) {
            result.append("🎉 所有条件都满足！\n");
            result.append("请到其他应用中点击输入框测试。");
            
            // 启动新的键盘感知悬浮球服务进行测试
            try {
                Intent serviceIntent = new Intent(this, KeyboardAwareFloatingBallService.class);
                startService(serviceIntent);
                result.append("\n\n✅ 键盘感知悬浮球服务已启动");
                result.append("\n💡 现在可以到其他应用测试输入框检测");
                result.append("\n🔍 查看日志：adb logcat -s KeyboardAwareFloatingBallService");
            } catch (Exception e) {
                result.append("\n\n⚠️ 启动服务失败：").append(e.getMessage());
            }
        } else {
            result.append("❌ 还有条件未满足，请按照提示完成配置。");
        }
        
        new AlertDialog.Builder(this)
                .setTitle("🧪 测试结果")
                .setMessage(result.toString())
                .setPositiveButton("我知道了", null)
                .show();
    }
    
    /**
     * 强制显示悬浮球（用于测试）
     */
    private void forceShowFloatingBall() {
        boolean hasOverlay = PermissionHelper.hasOverlayPermission(this);
        
        if (!hasOverlay) {
            showToast("❌ 需要悬浮窗权限");
            return;
        }
        
        try {
            // 启动键盘感知悬浮球服务
            Intent serviceIntent = new Intent(this, KeyboardAwareFloatingBallService.class);
            startService(serviceIntent);
            
            // 延迟后绑定服务并获取状态
            postDelayed(() -> {
                bindKeyboardAwareServiceForTest();
            }, 1000);
            
            showToast("🎈 正在启动键盘感知悬浮球服务...");
            
        } catch (Exception e) {
            showToast("❌ 启动服务失败：" + e.getMessage());
            Log.e("MainActivity", "Force show floating ball failed", e);
        }
    }
    
    /**
     * 绑定键盘感知服务进行测试和诊断
     */
    private void bindKeyboardAwareServiceForTest() {
        Intent serviceIntent = new Intent(this, KeyboardAwareFloatingBallService.class);
        bindService(serviceIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    KeyboardAwareFloatingBallService.KeyboardAwareBinder binder = 
                        (KeyboardAwareFloatingBallService.KeyboardAwareBinder) service;
                    KeyboardAwareFloatingBallService keyboardService = binder.getService();
                    
                    // 获取服务状态并显示
                    String status = keyboardService.getServiceStatus();
                    Log.d("MainActivity", "Service status:\n" + status);
                    
                    // 强制显示悬浮球进行测试
                    keyboardService.forceShowFloatingBall();
                    
                    // 显示诊断信息
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("🔍 服务诊断信息")
                            .setMessage(status + "\n\n💡 如果悬浮球仍未出现，请查看 Logcat 日志获取详细错误信息。")
                            .setPositiveButton("确定", null)
                            .show();
                    
                    // 延迟后解绑服务
                    postDelayed(() -> {
                        try {
                            unbindService(this);
                        } catch (Exception e) {
                            Log.w("MainActivity", "Unbind service failed", e);
                        }
                    }, 2000);
                    
                } catch (Exception e) {
                    showToast("❌ 服务连接失败：" + e.getMessage());
                    Log.e("MainActivity", "Service connection failed", e);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d("MainActivity", "KeyboardAwareFloatingBallService disconnected");
            }
        }, Context.BIND_AUTO_CREATE);
    }
    
    /**
     * 检查悬浮球相关服务状态
     */
    private void checkFloatingBallServices() {
        StringBuilder status = new StringBuilder();
        status.append("🔍 悬浮球服务状态检查：\n\n");
        
        // 检查权限
        boolean hasOverlay = PermissionHelper.hasOverlayPermission(this);
        boolean isEnabled = settingsRepository.isFloatingBallEnabled();
        
        status.append("🔑 悬浮窗权限：").append(hasOverlay ? "✅ 已授予" : "❌ 未授予").append("\n");
        status.append("🎈 悬浮球功能：").append(isEnabled ? "✅ 已启用" : "❌ 已禁用").append("\n\n");
        
        // 检查服务运行状态
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean keyboardAwareServiceRunning = false;
        
        for (android.app.ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
            if (service.service.getClassName().contains("KeyboardAwareFloatingBallService")) {
                keyboardAwareServiceRunning = true;
            }
        }
        
        status.append("🔧 服务运行状态：\n");
        status.append("  • KeyboardAwareFloatingBallService：").append(keyboardAwareServiceRunning ? "✅ 运行中" : "❌ 未运行").append("\n\n");
        
        // 建议修复步骤
        status.append("🛠️ 修复建议：\n");
        if (!hasOverlay) {
            status.append("1. 请授予悬浮窗权限\n");
        }
        if (!isEnabled) {
            status.append("2. 请启用悬浮球功能\n");
        }
        if (!keyboardAwareServiceRunning && hasOverlay && isEnabled) {
            status.append("3. 键盘感知服务未运行，请重启应用或重新启动服务\n");
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("服务状态检查")
                .setMessage(status.toString())
                .setPositiveButton("重新启动服务", (dialog, which) -> {
                    restartFloatingBallServices();
                })
                .setNeutralButton("测试悬浮球", (dialog, which) -> {
                    forceShowFloatingBall();
                })
                .setNegativeButton("关闭", null)
                .show();
    }
    
    /**
     * 重新启动悬浮球相关服务
     */
    private void restartFloatingBallServices() {
        try {
            // 停止旧的服务
            Intent oldServiceIntent = new Intent(this, com.inputassistant.universal.floating.FloatingBallService.class);
            stopService(oldServiceIntent);
            
            // 启动新的键盘感知服务
            Intent newServiceIntent = new Intent(this, KeyboardAwareFloatingBallService.class);
            
            // 延迟后重新启动
            postDelayed(() -> {
                if (settingsRepository.isFloatingBallEnabled()) {
                    startService(newServiceIntent);
                    showToast("✅ 键盘感知悬浮球服务已重启");
                }
            }, 1000);
            
        } catch (Exception e) {
            showToast("❌ 重启服务失败：" + e.getMessage());
            Log.e("MainActivity", "Restart services failed", e);
        }
    }
    
    /**
     * 测试悬浮球交互功能
     */
    private void testFloatingBallInteraction() {
        StringBuilder result = new StringBuilder();
        result.append("🎯 悬浮球交互测试：\n\n");
        
        // 检查输入法状态
        InputMethodHelper.InputMethodStatus status = 
            InputMethodHelper.checkInputMethodStatus(this);
        
        result.append("📱 当前输入法状态：");
        switch (status) {
            case NOT_ENABLED:
                result.append("❌ 输入法助手未启用\n");
                result.append("💡 点击悬浮球应该：跳转到设置页面\n");
                break;
            case ENABLED_NOT_CURRENT:
                result.append("🟡 输入法助手已启用但非当前\n");
                result.append("💡 点击悬浮球应该：显示输入法选择器\n");
                break;
            case ENABLED_AND_CURRENT:
                result.append("✅ 输入法助手已是当前输入法\n");
                result.append("💡 点击悬浮球应该：显示快捷菜单\n");
                break;
        }
        
        result.append("\n🔧 预期交互流程：\n");
        result.append("1. 点击悬浮球看到对应状态提示\n");
        result.append("2. 根据状态执行相应操作\n");
        result.append("3. 悬浮球颜色应该反映当前状态\n\n");
        
        result.append("🎨 悬浮球颜色说明：\n");
        result.append("🔴 红色 = 未启用输入法\n");
        result.append("🟠 橙色 = 可点击切换\n");
        result.append("🟢 绿色 = 已激活状态\n");
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("交互测试说明")
                .setMessage(result.toString())
                .setPositiveButton("强制显示悬浮球", (dialog, which) -> {
                    forceShowFloatingBall();
                })
                .setNegativeButton("关闭", null)
                .show();
    }
}

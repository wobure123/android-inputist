package com.inputist.universal.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.inputist.universal.R;
import com.inputist.universal.api.GenericLLMApiClient;
import com.inputist.universal.model.Action;
import com.inputist.universal.model.ApiConfig;
import com.inputist.universal.repository.SettingsRepository;
import com.inputist.universal.ui.adapter.ActionAdapter;

import java.util.List;

/**
 * 主界面 - 配置中心
 */
public class MainActivity extends AppCompatActivity implements ActionAdapter.OnActionClickListener {
    
    private TextInputEditText editBaseUrl;
    private TextInputEditText editApiKey;
    private TextInputEditText editModelName;
    private MaterialButton btnSaveConfig;
    private MaterialButton btnTestConfig;
    
    private RecyclerView recyclerActions;
    private ActionAdapter actionAdapter;
    private FloatingActionButton fabAddAction;
    
    private SettingsRepository settingsRepository;
    private GenericLLMApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        setSupportActionBar(findViewById(R.id.toolbar));
        
        settingsRepository = SettingsRepository.getInstance(this);
        apiClient = new GenericLLMApiClient();
        
        initializeViews();
        setupRecyclerView();
        loadData();
        
        // 首次运行检查
        if (settingsRepository.isFirstRun()) {
            showFirstRunGuide();
        }
    }

    private void initializeViews() {
        // API配置相关
        editBaseUrl = findViewById(R.id.edit_base_url);
        editApiKey = findViewById(R.id.edit_api_key);
        editModelName = findViewById(R.id.edit_model_name);
        btnSaveConfig = findViewById(R.id.btn_save_config);
        btnTestConfig = findViewById(R.id.btn_test_config);
        
        // Actions列表相关
        recyclerActions = findViewById(R.id.recycler_actions);
        fabAddAction = findViewById(R.id.fab_add_action);
        
        // 设置点击事件
        btnSaveConfig.setOnClickListener(v -> saveApiConfig());
        btnTestConfig.setOnClickListener(v -> testApiConfig());
        fabAddAction.setOnClickListener(v -> openActionEditor(null));
    }

    private void setupRecyclerView() {
        actionAdapter = new ActionAdapter(this);
        recyclerActions.setLayoutManager(new LinearLayoutManager(this));
        recyclerActions.setAdapter(actionAdapter);
    }

    private void loadData() {
        // 加载API配置
        ApiConfig apiConfig = settingsRepository.getApiConfig();
        if (apiConfig != null) {
            editBaseUrl.setText(apiConfig.getBaseUrl());
            editApiKey.setText(apiConfig.getApiKey());
            editModelName.setText(apiConfig.getModelName());
        }
        
        // 加载Actions
        loadActions();
    }

    private void loadActions() {
        List<Action> actions = settingsRepository.getActions();
        actionAdapter.setActions(actions);
    }

    private void saveApiConfig() {
        String baseUrl = editBaseUrl.getText().toString().trim();
        String apiKey = editApiKey.getText().toString().trim();
        String modelName = editModelName.getText().toString().trim();
        
        if (TextUtils.isEmpty(baseUrl)) {
            editBaseUrl.setError("请输入API地址");
            return;
        }
        
        if (TextUtils.isEmpty(apiKey)) {
            editApiKey.setError("请输入API密钥");
            return;
        }
        
        if (TextUtils.isEmpty(modelName)) {
            editModelName.setError("请输入模型名称");
            return;
        }
        
        // 验证URL格式
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            editBaseUrl.setError("URL格式不正确");
            return;
        }
        
        ApiConfig config = new ApiConfig(baseUrl, apiKey, modelName);
        settingsRepository.saveApiConfig(config);
        
        Toast.makeText(this, "API配置已保存", Toast.LENGTH_SHORT).show();
    }

    private void testApiConfig() {
        saveApiConfig(); // 先保存配置
        
        ApiConfig apiConfig = settingsRepository.getApiConfig();
        if (!apiConfig.isValid()) {
            Toast.makeText(this, "请先填写完整的API配置", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnTestConfig.setEnabled(false);
        btnTestConfig.setText("测试中...");
        
        apiClient.testApiConfig(apiConfig, new GenericLLMApiClient.ApiCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    btnTestConfig.setEnabled(true);
                    btnTestConfig.setText("测试配置");
                    Toast.makeText(MainActivity.this, "API配置测试成功", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnTestConfig.setEnabled(true);
                    btnTestConfig.setText("测试配置");
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("测试失败")
                            .setMessage("API配置测试失败：\n" + error)
                            .setPositiveButton("确定", null)
                            .show();
                });
            }
        });
    }

    private void openActionEditor(Action action) {
        Intent intent = new Intent(this, ActionEditorActivity.class);
        if (action != null) {
            intent.putExtra(ActionEditorActivity.EXTRA_ACTION_ID, action.getId());
        }
        startActivity(intent);
    }

    private void showFirstRunGuide() {
        new AlertDialog.Builder(this)
                .setTitle("欢迎使用通用输入改写助手")
                .setMessage("首次使用需要进行以下设置：\n" +
                        "1. 配置API信息\n" +
                        "2. 启用输入法服务\n" +
                        "3. 添加自定义功能\n\n" +
                        "是否查看详细使用指南？")
                .setPositiveButton("查看指南", (dialog, which) -> {
                    startActivity(new Intent(this, GuideActivity.class));
                    settingsRepository.setFirstRunCompleted();
                })
                .setNegativeButton("稍后再说", (dialog, which) -> {
                    settingsRepository.setFirstRunCompleted();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_guide) {
            startActivity(new Intent(this, GuideActivity.class));
            return true;
        } else if (id == R.id.action_ime_settings) {
            openIMESettings();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void openIMESettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开输入法设置", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActions(); // 从其他Activity返回时刷新列表
    }

    // ActionAdapter.OnActionClickListener 实现
    @Override
    public void onActionClick(Action action) {
        openActionEditor(action);
    }

    @Override
    public void onActionEdit(Action action) {
        openActionEditor(action);
    }

    @Override
    public void onActionDelete(Action action) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除功能 \"" + action.getName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    settingsRepository.deleteAction(action.getId());
                    loadActions();
                    Toast.makeText(this, "功能已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}

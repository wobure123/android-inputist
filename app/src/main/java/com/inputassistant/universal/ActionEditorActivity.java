package com.inputassistant.universal;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.inputassistant.universal.model.Action;
import com.inputassistant.universal.repository.SettingsRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * 动作编辑活动
 * 用于创建和编辑Action
 */
public class ActionEditorActivity extends AppCompatActivity {
    private EditText etActionName;
    private EditText etSystemPrompt;
    private Button btnSave;
    private Button btnCancel;
    
    private SettingsRepository settingsRepository;
    private String editingActionId = null; // 如果是编辑模式，存储Action ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_editor);
        
        initViews();
        initRepository();
        loadDataFromIntent();
        setupClickListeners();
    }

    private void initViews() {
        etActionName = findViewById(R.id.et_action_name);
        etSystemPrompt = findViewById(R.id.et_system_prompt);
        btnSave = findViewById(R.id.btn_save_action);
        btnCancel = findViewById(R.id.btn_cancel);
    }

    private void initRepository() {
        try {
            settingsRepository = new SettingsRepository(this);
        } catch (GeneralSecurityException | IOException e) {
            showError("初始化失败: " + e.getMessage());
            finish();
        }
    }

    private void loadDataFromIntent() {
        // 检查是否是编辑模式
        editingActionId = getIntent().getStringExtra("action_id");
        String actionName = getIntent().getStringExtra("action_name");
        String systemPrompt = getIntent().getStringExtra("action_system_prompt");

        if (editingActionId != null) {
            // 编辑模式
            setTitle("编辑动作");
            etActionName.setText(actionName);
            etSystemPrompt.setText(systemPrompt);
        } else {
            // 创建模式
            setTitle("创建动作");
            // 提供一些预设的系统指令示例
            etSystemPrompt.setHint("例如：你是一个专业的翻译专家，请将用户输入的文本翻译成英文...");
        }
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveAction());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveAction() {
        String name = etActionName.getText().toString().trim();
        String systemPrompt = etSystemPrompt.getText().toString().trim();

        // 验证输入
        if (name.isEmpty()) {
            etActionName.setError("请输入动作名称");
            etActionName.requestFocus();
            return;
        }

        if (systemPrompt.isEmpty()) {
            etSystemPrompt.setError("请输入系统指令");
            etSystemPrompt.requestFocus();
            return;
        }

        try {
            if (editingActionId != null) {
                // 编辑模式：更新现有Action
                Action updatedAction = new Action(editingActionId, name, systemPrompt);
                settingsRepository.updateAction(updatedAction);
                Toast.makeText(this, "动作已更新", Toast.LENGTH_SHORT).show();
            } else {
                // 创建模式：添加新Action
                Action newAction = new Action(name, systemPrompt);
                settingsRepository.addAction(newAction);
                Toast.makeText(this, "动作已创建", Toast.LENGTH_SHORT).show();
            }

            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            showError("保存失败: " + e.getMessage());
        }
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("错误")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        // 检查是否有未保存的更改
        if (hasUnsavedChanges()) {
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("有未保存的更改，确定要退出吗？")
                    .setPositiveButton("退出", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUnsavedChanges() {
        String currentName = etActionName.getText().toString().trim();
        String currentPrompt = etSystemPrompt.getText().toString().trim();
        
        if (editingActionId != null) {
            // 编辑模式：与原始数据比较
            String originalName = getIntent().getStringExtra("action_name");
            String originalPrompt = getIntent().getStringExtra("action_system_prompt");
            return !currentName.equals(originalName != null ? originalName : "") ||
                   !currentPrompt.equals(originalPrompt != null ? originalPrompt : "");
        } else {
            // 创建模式：检查是否有任何输入
            return !currentName.isEmpty() || !currentPrompt.isEmpty();
        }
    }
}

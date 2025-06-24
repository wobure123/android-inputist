package com.inputist.universal.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.inputist.universal.R;
import com.inputist.universal.model.Action;
import com.inputist.universal.repository.SettingsRepository;

/**
 * 动作编辑界面
 */
public class ActionEditorActivity extends AppCompatActivity {
    
    public static final String EXTRA_ACTION_ID = "action_id";
    
    private TextInputEditText editActionName;
    private TextInputEditText editSystemPrompt;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    
    private SettingsRepository settingsRepository;
    private Action currentAction;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_editor);
        
        settingsRepository = SettingsRepository.getInstance(this);
        
        // 检查是否是编辑模式
        String actionId = getIntent().getStringExtra(EXTRA_ACTION_ID);
        if (actionId != null) {
            currentAction = settingsRepository.findActionById(actionId);
            isEditMode = currentAction != null;
        }
        
        setupToolbar();
        initializeViews();
        loadData();
    }

    private void setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "编辑功能" : "添加功能");
        }
    }

    private void initializeViews() {
        editActionName = findViewById(R.id.edit_action_name);
        editSystemPrompt = findViewById(R.id.edit_system_prompt);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        
        btnSave.setOnClickListener(v -> saveAction());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadData() {
        if (isEditMode && currentAction != null) {
            editActionName.setText(currentAction.getName());
            editSystemPrompt.setText(currentAction.getSystemPrompt());
        } else {
            // 新建模式，可以设置一些默认值
            currentAction = new Action();
        }
    }

    private void saveAction() {
        String name = editActionName.getText().toString().trim();
        String systemPrompt = editSystemPrompt.getText().toString().trim();
        
        // 验证输入
        if (TextUtils.isEmpty(name)) {
            editActionName.setError("请输入功能名称");
            editActionName.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(systemPrompt)) {
            editSystemPrompt.setError("请输入系统指令");
            editSystemPrompt.requestFocus();
            return;
        }
        
        // 更新Action对象
        currentAction.setName(name);
        currentAction.setSystemPrompt(systemPrompt);
        
        // 保存到数据库
        if (isEditMode) {
            settingsRepository.updateAction(currentAction);
            Toast.makeText(this, "功能已更新", Toast.LENGTH_SHORT).show();
        } else {
            settingsRepository.addAction(currentAction);
            Toast.makeText(this, "功能已添加", Toast.LENGTH_SHORT).show();
        }
        
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isEditMode) {
            getMenuInflater().inflate(R.menu.action_editor_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        } else if (id == R.id.action_preview) {
            previewAction();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除此功能吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    settingsRepository.deleteAction(currentAction.getId());
                    Toast.makeText(this, "功能已删除", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void previewAction() {
        String name = editActionName.getText().toString().trim();
        String systemPrompt = editSystemPrompt.getText().toString().trim();
        
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(systemPrompt)) {
            Toast.makeText(this, "请先填写完整信息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String preview = "功能名称: " + name + "\n\n" +
                        "系统指令: " + systemPrompt + "\n\n" +
                        "使用说明: 在输入法界面点击此按钮，会将当前输入框的文本与此系统指令一起发送给AI处理。";
        
        new AlertDialog.Builder(this)
                .setTitle("功能预览")
                .setMessage(preview)
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        // 检查是否有未保存的更改
        String name = editActionName.getText().toString().trim();
        String systemPrompt = editSystemPrompt.getText().toString().trim();
        
        boolean hasChanges = false;
        if (isEditMode && currentAction != null) {
            hasChanges = !name.equals(currentAction.getName()) || 
                        !systemPrompt.equals(currentAction.getSystemPrompt());
        } else {
            hasChanges = !TextUtils.isEmpty(name) || !TextUtils.isEmpty(systemPrompt);
        }
        
        if (hasChanges) {
            new AlertDialog.Builder(this)
                    .setTitle("未保存的更改")
                    .setMessage("您有未保存的更改，确定要离开吗？")
                    .setPositiveButton("离开", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("继续编辑", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}

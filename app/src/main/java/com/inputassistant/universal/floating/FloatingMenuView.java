package com.inputassistant.universal.floating;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.inputassistant.universal.MainActivity;
import com.inputassistant.universal.R;
import com.inputassistant.universal.utils.InputMethodHelper;

/**
 * 悬浮菜单视图
 * 显示快速操作选项
 */
public class FloatingMenuView extends LinearLayout {
    private static final String TAG = "FloatingMenuView";
    
    private Button btnSwitchToAssistant;
    private Button btnQuickTranslate;
    private Button btnSettings;
    
    private FloatingBallManager manager;

    public FloatingMenuView(Context context) {
        super(context);
        init();
    }

    public FloatingMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 加载布局
        View.inflate(getContext(), R.layout.floating_menu, this);
        
        btnSwitchToAssistant = findViewById(R.id.btn_switch_to_assistant);
        btnQuickTranslate = findViewById(R.id.btn_quick_translate);
        btnSettings = findViewById(R.id.btn_settings);
        
        setupClickListeners();
        
        // 初始设置为不可见
        setVisibility(View.GONE);
        setAlpha(0f);
    }
    
    /**
     * 设置管理器引用
     */
    public void setManager(FloatingBallManager manager) {
        this.manager = manager;
    }
    
    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        // 切换到输入法助手
        btnSwitchToAssistant.setOnClickListener(v -> {
            Log.d(TAG, "Switch to assistant button clicked");
            
            InputMethodHelper.InputMethodStatus status = 
                InputMethodHelper.checkInputMethodStatus(getContext());
            
            switch (status) {
                case NOT_ENABLED:
                    android.widget.Toast.makeText(getContext(), 
                        "请先在设置中启用输入法助手", android.widget.Toast.LENGTH_LONG).show();
                    openMainActivity();
                    break;
                    
                case ENABLED_NOT_CURRENT:
                    android.widget.Toast.makeText(getContext(), 
                        "正在打开输入法选择器...", android.widget.Toast.LENGTH_SHORT).show();
                    InputMethodHelper.showInputMethodPicker(getContext());
                    break;
                    
                case ENABLED_AND_CURRENT:
                    android.widget.Toast.makeText(getContext(), 
                        "输入法助手已是当前输入法", android.widget.Toast.LENGTH_SHORT).show();
                    openMainActivity();
                    break;
            }
            hideMenu();
        });
        
        // 快速翻译功能
        btnQuickTranslate.setOnClickListener(v -> {
            Log.d(TAG, "Quick translate button clicked");
            
            // 检查是否可以执行翻译
            InputMethodHelper.InputMethodStatus status = 
                InputMethodHelper.checkInputMethodStatus(getContext());
                
            if (status == InputMethodHelper.InputMethodStatus.ENABLED_AND_CURRENT) {
                android.widget.Toast.makeText(getContext(), 
                    "请在输入法助手中使用翻译功能", android.widget.Toast.LENGTH_SHORT).show();
                openMainActivity();
            } else {
                android.widget.Toast.makeText(getContext(), 
                    "请先切换到输入法助手", android.widget.Toast.LENGTH_SHORT).show();
                InputMethodHelper.showInputMethodPicker(getContext());
            }
            hideMenu();
        });
        
        // 设置
        btnSettings.setOnClickListener(v -> {
            Log.d(TAG, "Settings button clicked");
            android.widget.Toast.makeText(getContext(), 
                "打开输入法助手设置", android.widget.Toast.LENGTH_SHORT).show();
            openMainActivity();
            hideMenu();
        });
        
        // 点击菜单外部区域隐藏菜单
        setOnClickListener(v -> hideMenu());
    }
    
    /**
     * 隐藏菜单
     */
    private void hideMenu() {
        if (manager != null) {
            manager.hideQuickMenu();
        }
    }
    
    /**
     * 打开主应用
     */
    private void openMainActivity() {
        try {
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            getContext().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 显示动画
     */
    public void showWithAnimation() {
        setVisibility(View.VISIBLE);
        setAlpha(0f);
        setScaleX(0.8f);
        setScaleY(0.8f);
        
        animate()
            .alpha(1.0f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(200)
            .start();
    }
    
    /**
     * 隐藏动画
     */
    public void hideWithAnimation(Runnable onComplete) {
        animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(150)
            .withEndAction(() -> {
                setVisibility(View.GONE);
                if (onComplete != null) {
                    onComplete.run();
                }
            })
            .start();
    }
    
    /**
     * 更新菜单选项状态
     */
    public void updateMenuState() {
        InputMethodHelper.InputMethodStatus status = 
            InputMethodHelper.checkInputMethodStatus(getContext());
            
        switch (status) {
            case NOT_ENABLED:
                btnSwitchToAssistant.setText("启用输入法");
                btnQuickTranslate.setEnabled(false);
                break;
                
            case ENABLED_NOT_CURRENT:
                btnSwitchToAssistant.setText("切换输入法");
                btnQuickTranslate.setEnabled(false);
                break;
                
            case ENABLED_AND_CURRENT:
                btnSwitchToAssistant.setText("输入法选择");
                btnQuickTranslate.setEnabled(true);
                break;
        }
    }
}

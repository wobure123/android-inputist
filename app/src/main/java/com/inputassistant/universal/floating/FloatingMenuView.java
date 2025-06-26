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
            InputMethodHelper.showInputMethodPicker(getContext());
            hideMenu();
        });
        
        // 快速翻译（待实现具体功能）
        btnQuickTranslate.setOnClickListener(v -> {
            // TODO: 实现快速翻译功能
            // 可以直接调用输入法服务的翻译动作
            hideMenu();
        });
        
        // 设置
        btnSettings.setOnClickListener(v -> {
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

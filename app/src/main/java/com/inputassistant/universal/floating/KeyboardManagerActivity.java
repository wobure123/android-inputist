package com.inputassistant.universal.floating;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.inputassistant.universal.R;

/**
 * 输入法管理Activity
 * 专门用于显示输入法选择器，解决悬浮球在非Inputist输入法状态下无法响应的问题
 */
public class KeyboardManagerActivity extends AppCompatActivity {
    
    public static final String DELAY_SHOW_KEY = "DELAY_SHOW_KEY";
    
    private long delay = 200L; // 延迟显示时间
    
    private InputMethodManager imeManager;
    private View rootView;
    
    /**
     * 对话框状态
     */
    enum DialogState {
        NONE, PICKING, CHOSEN
    }
    
    private DialogState mState;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mState = DialogState.NONE;
        super.onCreate(savedInstanceState);
        
        // 设置空布局
        setContentView(R.layout.activity_keyboard_manager);
        rootView = findViewById(R.id.root_view);
        
        imeManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        
        if (getIntent() != null) {
            delay = getIntent().getLongExtra(DELAY_SHOW_KEY, delay);
        }
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mState == DialogState.PICKING) {
            mState = DialogState.CHOSEN;
        } else if (mState == DialogState.CHOSEN) {
            finish();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        rootView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (imeManager != null) {
                    imeManager.showInputMethodPicker();
                }
                mState = DialogState.PICKING;
            }
        }, delay);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

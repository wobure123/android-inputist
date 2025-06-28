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
 * 关键特点：透明、不在最近任务中显示、自动关闭
 */
public class KeyboardManagerActivity extends AppCompatActivity {
    
    public static final String DELAY_SHOW_KEY = "DELAY_SHOW_KEY";
    
    private long delay = 50L; // 进一步缩短延迟时间
    
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
        
        // 设置透明布局
        setContentView(R.layout.activity_keyboard_manager);
        rootView = findViewById(R.id.root_view);
        
        imeManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        
        if (getIntent() != null) {
            delay = getIntent().getLongExtra(DELAY_SHOW_KEY, delay);
        }
        
        // 确保Activity不影响当前任务栈
        setTaskDescription(new android.app.ActivityManager.TaskDescription("", null, 0));
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // 当输入法选择器显示后，焦点会改变
        if (mState == DialogState.PICKING) {
            mState = DialogState.CHOSEN;
        } else if (mState == DialogState.CHOSEN) {
            // 输入法选择完成，立即关闭Activity
            finish();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // 延迟显示输入法选择器
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
    
    @Override
    protected void onPause() {
        super.onPause();
        // 当Activity失去焦点时，确保关闭
        if (mState == DialogState.CHOSEN) {
            finish();
        }
    }
}

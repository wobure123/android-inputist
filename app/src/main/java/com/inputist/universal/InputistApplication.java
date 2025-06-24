package com.inputist.universal;

import android.app.Application;
import android.util.Log;

/**
 * 应用程序主类
 */
public class InputistApplication extends Application {
    private static final String TAG = "InputistApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate");
        
        // 初始化全局配置
        initializeApplication();
    }

    private void initializeApplication() {
        // 设置全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.e(TAG, "Uncaught exception", ex);
                // 可以在这里添加崩溃日志上报
                System.exit(1);
            }
        });
    }
}

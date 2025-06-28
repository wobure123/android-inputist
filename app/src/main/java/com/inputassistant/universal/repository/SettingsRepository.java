package com.inputassistant.universal.repository;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.inputassistant.universal.model.Action;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * 设置数据仓库
 * 负责API配置和Action列表的加密存储
 */
public class SettingsRepository {
    private static final String PREFS_FILE_NAME = "secure_settings";
    private static final String KEY_API_BASE_URL = "api_base_url";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL_NAME = "model_name";
    private static final String KEY_ACTIONS_JSON = "actions_json";
    private static final String KEY_PREVIOUS_IME = "previous_ime"; // 保存上一个输入法
    private static final String KEY_TEXT_MODE = "text_processing_mode"; // 文本处理模式
    private static final String KEY_FLOATING_BALL_ENABLED = "floating_ball_enabled"; // 悬浮球开关
    private static final String KEY_FLOATING_BALL_POSITION_X = "floating_ball_x"; // 悬浮球X位置
    private static final String KEY_FLOATING_BALL_POSITION_Y = "floating_ball_y"; // 悬浮球Y位置

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public SettingsRepository(Context context) throws GeneralSecurityException, IOException {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        
        this.sharedPreferences = EncryptedSharedPreferences.create(
                PREFS_FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
        
        this.gson = new Gson();
    }

    // API 配置相关方法
    public void saveApiBaseUrl(String baseUrl) {
        sharedPreferences.edit().putString(KEY_API_BASE_URL, baseUrl).apply();
    }

    public String getApiBaseUrl() {
        return sharedPreferences.getString(KEY_API_BASE_URL, "");
    }

    public void saveApiKey(String apiKey) {
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public String getApiKey() {
        return sharedPreferences.getString(KEY_API_KEY, "");
    }

    public void saveModelName(String modelName) {
        sharedPreferences.edit().putString(KEY_MODEL_NAME, modelName).apply();
    }

    public String getModelName() {
        return sharedPreferences.getString(KEY_MODEL_NAME, "gpt-3.5-turbo");
    }

    // Action 管理相关方法
    public void saveActions(List<Action> actions) {
        String json = gson.toJson(actions);
        sharedPreferences.edit().putString(KEY_ACTIONS_JSON, json).apply();
    }

    public List<Action> getActions() {
        String json = sharedPreferences.getString(KEY_ACTIONS_JSON, "[]");
        Type listType = new TypeToken<List<Action>>(){}.getType();
        List<Action> actions = gson.fromJson(json, listType);
        return actions != null ? actions : new ArrayList<>();
    }

    public void addAction(Action action) {
        List<Action> actions = getActions();
        actions.add(action);
        saveActions(actions);
    }

    public void updateAction(Action updatedAction) {
        List<Action> actions = getActions();
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i).getId().equals(updatedAction.getId())) {
                actions.set(i, updatedAction);
                break;
            }
        }
        saveActions(actions);
    }

    public void deleteAction(String actionId) {
        List<Action> actions = getActions();
        actions.removeIf(action -> action.getId().equals(actionId));
        saveActions(actions);
    }

    public boolean isConfigured() {
        return !getApiBaseUrl().isEmpty() && !getApiKey().isEmpty();
    }

    // 输入法相关方法
    public void savePreviousInputMethod(String inputMethodId) {
        sharedPreferences.edit().putString(KEY_PREVIOUS_IME, inputMethodId).apply();
    }

    public String getPreviousInputMethod() {
        return sharedPreferences.getString(KEY_PREVIOUS_IME, "");
    }

    // 文本处理模式相关方法
    public void setTextProcessingMode(boolean isReplaceMode) {
        sharedPreferences.edit().putBoolean(KEY_TEXT_MODE, isReplaceMode).apply();
    }

    public boolean isReplaceMode() {
        return sharedPreferences.getBoolean(KEY_TEXT_MODE, false); // 默认为拼接模式(false)
    }

    public String getTextProcessingModeDescription() {
        return isReplaceMode() ? "替换模式" : "拼接模式";
    }

    // 悬浮球相关方法
    public void setFloatingBallEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_FLOATING_BALL_ENABLED, enabled).apply();
    }

    public boolean isFloatingBallEnabled() {
        return sharedPreferences.getBoolean(KEY_FLOATING_BALL_ENABLED, false);
    }

    public void saveFloatingBallPosition(int x, int y) {
        sharedPreferences.edit()
                .putInt(KEY_FLOATING_BALL_POSITION_X, x)
                .putInt(KEY_FLOATING_BALL_POSITION_Y, y)
                .apply();
    }

    public int getFloatingBallPositionX() {
        return sharedPreferences.getInt(KEY_FLOATING_BALL_POSITION_X, 0);
    }

    public int getFloatingBallPositionY() {
        return sharedPreferences.getInt(KEY_FLOATING_BALL_POSITION_Y, 100);
    }

    // 清除所有数据（用于重置或调试）
    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }
}

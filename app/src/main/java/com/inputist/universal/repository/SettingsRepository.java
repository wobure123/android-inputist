package com.inputist.universal.repository;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.inputist.universal.model.Action;
import com.inputist.universal.model.ApiConfig;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 设置和数据存储仓库
 * 负责API配置和Action列表的持久化存储
 */
public class SettingsRepository {
    private static final String PREFS_NAME = "inputist_settings";
    private static final String KEY_API_CONFIG = "api_config";
    private static final String KEY_ACTIONS = "actions";
    private static final String KEY_FIRST_RUN = "first_run";
    
    private static SettingsRepository instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    
    // 内存缓存
    private ApiConfig cachedApiConfig;
    private List<Action> cachedActions;

    private SettingsRepository(Context context) {
        this.gson = new Gson();
        try {
            // 创建主密钥
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // 创建加密的SharedPreferences
            this.prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encrypted storage", e);
        }
    }

    public static synchronized SettingsRepository getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsRepository(context.getApplicationContext());
        }
        return instance;
    }

    // ===== API配置相关 =====

    /**
     * 保存API配置
     */
    public void saveApiConfig(ApiConfig config) {
        if (config == null) return;
        
        String json = gson.toJson(config);
        prefs.edit().putString(KEY_API_CONFIG, json).apply();
        cachedApiConfig = config;
    }

    /**
     * 获取API配置
     */
    public ApiConfig getApiConfig() {
        if (cachedApiConfig != null) {
            return cachedApiConfig;
        }
        
        String json = prefs.getString(KEY_API_CONFIG, null);
        if (json != null) {
            try {
                cachedApiConfig = gson.fromJson(json, ApiConfig.class);
                return cachedApiConfig;
            } catch (Exception e) {
                // JSON解析失败，返回默认配置
                e.printStackTrace();
            }
        }
        
        // 返回默认配置
        cachedApiConfig = new ApiConfig();
        return cachedApiConfig;
    }

    /**
     * 检查API配置是否已设置
     */
    public boolean hasApiConfig() {
        ApiConfig config = getApiConfig();
        return config.isValid();
    }

    // ===== Action列表相关 =====

    /**
     * 保存Action列表
     */
    public void saveActions(List<Action> actions) {
        if (actions == null) {
            actions = new ArrayList<>();
        }
        
        String json = gson.toJson(actions);
        prefs.edit().putString(KEY_ACTIONS, json).apply();
        cachedActions = new ArrayList<>(actions);
    }

    /**
     * 获取Action列表
     */
    public List<Action> getActions() {
        if (cachedActions != null) {
            return new ArrayList<>(cachedActions);
        }
        
        String json = prefs.getString(KEY_ACTIONS, null);
        if (json != null) {
            try {
                Type listType = new TypeToken<List<Action>>(){}.getType();
                List<Action> actions = gson.fromJson(json, listType);
                cachedActions = actions != null ? actions : new ArrayList<>();
                return new ArrayList<>(cachedActions);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // 返回空列表并创建默认Action
        cachedActions = createDefaultActions();
        saveActions(cachedActions);
        return new ArrayList<>(cachedActions);
    }

    /**
     * 添加新Action
     */
    public void addAction(Action action) {
        if (action == null || !action.isValid()) return;
        
        List<Action> actions = getActions();
        actions.add(action);
        saveActions(actions);
    }

    /**
     * 更新Action
     */
    public void updateAction(Action action) {
        if (action == null || !action.isValid()) return;
        
        List<Action> actions = getActions();
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i).getId().equals(action.getId())) {
                actions.set(i, action);
                saveActions(actions);
                break;
            }
        }
    }

    /**
     * 删除Action
     */
    public void deleteAction(String actionId) {
        if (actionId == null) return;
        
        List<Action> actions = getActions();
        actions.removeIf(action -> actionId.equals(action.getId()));
        saveActions(actions);
    }

    /**
     * 根据ID查找Action
     */
    public Action findActionById(String actionId) {
        if (actionId == null) return null;
        
        List<Action> actions = getActions();
        for (Action action : actions) {
            if (actionId.equals(action.getId())) {
                return action;
            }
        }
        return null;
    }

    // ===== 应用设置相关 =====

    /**
     * 检查是否首次运行
     */
    public boolean isFirstRun() {
        return prefs.getBoolean(KEY_FIRST_RUN, true);
    }

    /**
     * 标记已完成首次运行
     */
    public void setFirstRunCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
    }

    // ===== 私有方法 =====

    /**
     * 创建默认的Action列表
     */
    private List<Action> createDefaultActions() {
        List<Action> defaultActions = new ArrayList<>();
        
        // 翻译成中文
        Action translateToChinese = new Action(
                "翻译成中文",
                "你是一个精通多语言的翻译家，请将用户提供的任何语言的文本准确、流畅地翻译成简体中文。保持原文的语气和风格。"
        );
        defaultActions.add(translateToChinese);
        
        // 翻译成英文
        Action translateToEnglish = new Action(
                "翻译成英文",
                "You are an expert translator. Please translate the user's text into fluent, natural English. Maintain the original tone and style."
        );
        defaultActions.add(translateToEnglish);
        
        // 润色文本
        Action polishText = new Action(
                "润色文本",
                "你是一个专业的文本编辑师。请对用户提供的文本进行润色，使其更加通顺、优雅、专业。保持原意不变，但提升表达质量。"
        );
        defaultActions.add(polishText);
        
        // 总结要点
        Action summarize = new Action(
                "总结要点",
                "请对用户提供的文本进行总结，提取关键要点。用简洁明了的语言概括主要内容，保持逻辑清晰。"
        );
        defaultActions.add(summarize);
        
        return defaultActions;
    }

    /**
     * 清除所有缓存
     */
    public void clearCache() {
        cachedApiConfig = null;
        cachedActions = null;
    }

    /**
     * 清除所有设置（慎用）
     */
    public void clearAllSettings() {
        prefs.edit().clear().apply();
        clearCache();
    }
}

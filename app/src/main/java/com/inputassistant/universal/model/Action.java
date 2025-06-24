package com.inputassistant.universal.model;

import java.util.UUID;

/**
 * 动作数据模型
 * 每个Action代表一个自定义的LLM处理功能
 */
public class Action {
    private String id;
    private String name;
    private String systemPrompt;

    // 默认构造函数（用于JSON反序列化）
    public Action() {
        this.id = UUID.randomUUID().toString();
    }

    // 带参数构造函数
    public Action(String name, String systemPrompt) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.systemPrompt = systemPrompt;
    }

    // 完整构造函数（用于编辑现有Action）
    public Action(String id, String name, String systemPrompt) {
        this.id = id;
        this.name = name;
        this.systemPrompt = systemPrompt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Action action = (Action) obj;
        return id != null && id.equals(action.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Action{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", systemPrompt='" + systemPrompt + '\'' +
                '}';
    }
}

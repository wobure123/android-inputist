package com.inputist.universal.model;

import java.util.UUID;

/**
 * Action数据模型
 * 表示一个自定义的输入处理动作
 */
public class Action {
    private String id;
    private String name;
    private String systemPrompt;
    private long createdTime;
    private long modifiedTime;

    // 构造函数
    public Action() {
        this.id = UUID.randomUUID().toString();
        this.createdTime = System.currentTimeMillis();
        this.modifiedTime = System.currentTimeMillis();
    }

    public Action(String name, String systemPrompt) {
        this();
        this.name = name;
        this.systemPrompt = systemPrompt;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
        updateModifiedTime();
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        updateModifiedTime();
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    // 辅助方法
    private void updateModifiedTime() {
        this.modifiedTime = System.currentTimeMillis();
    }

    /**
     * 验证Action是否有效
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() 
               && systemPrompt != null && !systemPrompt.trim().isEmpty();
    }

    /**
     * 创建Action的副本
     */
    public Action copy() {
        Action copy = new Action();
        copy.id = this.id;
        copy.name = this.name;
        copy.systemPrompt = this.systemPrompt;
        copy.createdTime = this.createdTime;
        copy.modifiedTime = this.modifiedTime;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Action action = (Action) obj;
        return id != null ? id.equals(action.id) : action.id == null;
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
                ", systemPrompt='" + (systemPrompt != null ? systemPrompt.substring(0, Math.min(50, systemPrompt.length())) + "..." : "null") + '\'' +
                ", createdTime=" + createdTime +
                ", modifiedTime=" + modifiedTime +
                '}';
    }
}

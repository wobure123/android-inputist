package com.inputist.universal.model;

/**
 * API配置数据模型
 */
public class ApiConfig {
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private int timeout;
    private boolean useProxy;
    private String proxyHost;
    private int proxyPort;

    // 默认构造函数
    public ApiConfig() {
        this.timeout = 30; // 默认30秒超时
        this.useProxy = false;
    }

    public ApiConfig(String baseUrl, String apiKey, String modelName) {
        this();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    // Getters
    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isUseProxy() {
        return useProxy;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    // Setters
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * 验证API配置是否有效
     */
    public boolean isValid() {
        return baseUrl != null && !baseUrl.trim().isEmpty()
               && apiKey != null && !apiKey.trim().isEmpty()
               && modelName != null && !modelName.trim().isEmpty();
    }

    /**
     * 获取格式化的BaseURL（确保以/结尾）
     */
    public String getFormattedBaseUrl() {
        if (baseUrl == null) return null;
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    /**
     * 隐藏API密钥用于显示（只显示前4位和后4位）
     */
    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    @Override
    public String toString() {
        return "ApiConfig{" +
                "baseUrl='" + baseUrl + '\'' +
                ", apiKey='" + getMaskedApiKey() + '\'' +
                ", modelName='" + modelName + '\'' +
                ", timeout=" + timeout +
                ", useProxy=" + useProxy +
                ", proxyHost='" + proxyHost + '\'' +
                ", proxyPort=" + proxyPort +
                '}';
    }
}

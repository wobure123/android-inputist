package com.inputist.universal.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.inputist.universal.api.model.OpenAIRequest;
import com.inputist.universal.api.model.OpenAIResponse;
import com.inputist.universal.model.ApiConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 通用LLM API客户端
 * 支持任何兼容OpenAI标准的API端点
 */
public class GenericLLMApiClient {
    private static final String TAG = "GenericLLMApiClient";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private OkHttpClient httpClient;
    private final Gson gson;
    private final Handler mainHandler;

    public GenericLLMApiClient() {
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initializeHttpClient(null);
    }

    /**
     * 初始化HTTP客户端
     */
    private void initializeHttpClient(ApiConfig config) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        // 如果配置了代理
        if (config != null && config.isUseProxy() && config.getProxyHost() != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, 
                    new InetSocketAddress(config.getProxyHost(), config.getProxyPort()));
            builder.proxy(proxy);
        }

        this.httpClient = builder.build();
    }

    /**
     * 执行API请求
     * @param apiConfig API配置
     * @param systemPrompt 系统指令
     * @param userPrompt 用户输入
     * @param callback 回调接口
     */
    public void executeRequest(ApiConfig apiConfig, String systemPrompt, String userPrompt, ApiCallback callback) {
        if (apiConfig == null || !apiConfig.isValid()) {
            callback.onError("API配置无效");
            return;
        }

        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            callback.onError("用户输入为空");
            return;
        }

        // 重新初始化客户端（考虑代理设置）
        initializeHttpClient(apiConfig);

        // 构建请求
        OpenAIRequest.Message systemMessage = OpenAIRequest.Message.system(systemPrompt);
        OpenAIRequest.Message userMessage = OpenAIRequest.Message.user(userPrompt);
        
        OpenAIRequest request = new OpenAIRequest(
                apiConfig.getModelName(),
                Arrays.asList(systemMessage, userMessage)
        );

        // 构建HTTP请求
        String jsonBody = gson.toJson(request);
        RequestBody body = RequestBody.create(jsonBody, JSON);
        
        String url = apiConfig.getFormattedBaseUrl() + "v1/chat/completions";
        
        Request httpRequest = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiConfig.getApiKey())
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        Log.d(TAG, "Sending request to: " + url);
        Log.d(TAG, "Request body: " + jsonBody);

        // 异步执行请求
        httpClient.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed", e);
                String errorMessage = "网络请求失败: " + e.getMessage();
                mainHandler.post(() -> callback.onError(errorMessage));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response code: " + response.code());
                    Log.d(TAG, "Response body: " + responseBody);

                    if (!response.isSuccessful()) {
                        String errorMessage = "HTTP " + response.code() + ": " + response.message();
                        
                        // 尝试解析错误信息
                        try {
                            OpenAIResponse errorResponse = gson.fromJson(responseBody, OpenAIResponse.class);
                            if (errorResponse.getError() != null) {
                                errorMessage = errorResponse.getError().getMessage();
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to parse error response", e);
                        }
                        
                        final String finalErrorMessage = errorMessage;
                        mainHandler.post(() -> callback.onError(finalErrorMessage));
                        return;
                    }

                    // 解析响应
                    OpenAIResponse apiResponse = gson.fromJson(responseBody, OpenAIResponse.class);
                    
                    if (!apiResponse.isSuccess()) {
                        String errorMessage = "API返回错误";
                        if (apiResponse.getError() != null) {
                            errorMessage = apiResponse.getError().getMessage();
                        }
                        final String finalErrorMessage = errorMessage;
                        mainHandler.post(() -> callback.onError(finalErrorMessage));
                        return;
                    }

                    String result = apiResponse.getFirstChoiceContent();
                    if (result == null || result.trim().isEmpty()) {
                        mainHandler.post(() -> callback.onError("API返回空结果"));
                        return;
                    }

                    // 成功回调
                    mainHandler.post(() -> callback.onSuccess(result.trim()));

                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse response", e);
                    mainHandler.post(() -> callback.onError("响应解析失败: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * 取消所有进行中的请求
     */
    public void cancelAllRequests() {
        if (httpClient != null) {
            httpClient.dispatcher().cancelAll();
        }
    }

    /**
     * API回调接口
     */
    public interface ApiCallback {
        /**
         * 请求成功
         * @param result 处理后的文本结果
         */
        void onSuccess(String result);

        /**
         * 请求失败
         * @param error 错误信息
         */
        void onError(String error);
    }

    /**
     * 测试API配置是否有效
     */
    public void testApiConfig(ApiConfig apiConfig, ApiCallback callback) {
        executeRequest(apiConfig, "You are a helpful assistant.", "Hello", new ApiCallback() {
            @Override
            public void onSuccess(String result) {
                callback.onSuccess("API配置测试成功");
            }

            @Override
            public void onError(String error) {
                callback.onError("API配置测试失败: " + error);
            }
        });
    }
}

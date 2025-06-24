package com.inputassistant.universal.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 通用LLM API客户端
 * 支持任何兼容OpenAI标准的API端点
 */
public class GenericLLMApiClient {
    private static final String TAG = "GenericLLMApiClient";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Handler mainHandler;

    public GenericLLMApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 执行LLM请求
     * @param baseUrl API基础URL
     * @param apiKey API密钥
     * @param modelName 模型名称
     * @param systemPrompt 系统指令
     * @param userPrompt 用户输入
     * @param callback 回调接口
     */
    public void executeRequest(String baseUrl, String apiKey, String modelName, 
                              String systemPrompt, String userPrompt, 
                              ApiCallback callback) {
        // 构建请求体
        JsonObject requestBody = buildRequestBody(modelName, systemPrompt, userPrompt);
        
        // 构建请求
        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        // 异步执行请求
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed", e);
                mainHandler.post(() -> callback.onError("网络请求失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        String errorMsg = responseBody != null ? responseBody.string() : "Unknown error";
                        Log.e(TAG, "API request unsuccessful: " + response.code() + " - " + errorMsg);
                        mainHandler.post(() -> callback.onError("API请求失败: HTTP " + response.code()));
                        return;
                    }

                    if (responseBody == null) {
                        mainHandler.post(() -> callback.onError("响应体为空"));
                        return;
                    }

                    String responseString = responseBody.string();
                    Log.d(TAG, "API response: " + responseString);
                    
                    // 解析响应
                    String result = parseResponse(responseString);
                    if (result != null) {
                        mainHandler.post(() -> callback.onSuccess(result));
                    } else {
                        mainHandler.post(() -> callback.onError("解析响应失败"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    mainHandler.post(() -> callback.onError("处理响应时出错: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * 构建OpenAI格式的请求体
     */
    private JsonObject buildRequestBody(String modelName, String systemPrompt, String userPrompt) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", modelName);
        requestBody.addProperty("max_tokens", 1000);
        requestBody.addProperty("temperature", 0.7);
        
        // 构建messages数组
        JsonArray messages = new JsonArray();
        
        // 系统消息
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);
        
        // 用户消息
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);
        messages.add(userMessage);
        
        requestBody.add("messages", messages);
        
        Log.d(TAG, "Request body: " + requestBody.toString());
        return requestBody;
    }

    /**
     * 解析OpenAI格式的响应
     */
    private String parseResponse(String responseString) {
        try {
            JsonObject responseJson = gson.fromJson(responseString, JsonObject.class);
            
            if (responseJson.has("choices") && responseJson.get("choices").isJsonArray()) {
                JsonArray choices = responseJson.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();
                    if (firstChoice.has("message")) {
                        JsonObject message = firstChoice.getAsJsonObject("message");
                        if (message.has("content")) {
                            return message.get("content").getAsString().trim();
                        }
                    }
                }
            }
            
            Log.e(TAG, "Invalid response format: " + responseString);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response", e);
            return null;
        }
    }

    /**
     * API回调接口
     */
    public interface ApiCallback {
        void onSuccess(String result);
        void onError(String error);
    }
}

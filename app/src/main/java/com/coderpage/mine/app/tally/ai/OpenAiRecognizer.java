package com.coderpage.mine.app.tally.ai;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OpenAI 兼容 API 识别器
 * 
 * @author Flandre Scarlet
 */
public class OpenAiRecognizer implements AiRecognizer {

    private static final String TAG = "OpenAiRecognizer";

    private final AiApiConfig config;
    private final OkHttpClient client;
    private final Gson gson;

    // 系统提示词
    private static final String SYSTEM_PROMPT = "你是一个账单识别助手。请从图片中识别账单信息。";

    // 用户提示词
    private static final String USER_PROMPT = "请识别图片中的账单信息，回答三个问题：\n" +
            "1. 是否涉及支付或收入？\n" +
            "2. 是支出还是收入？\n" +
            "3. 金额是多少？\n" +
            "严格按以下JSON格式输出，不要包含任何额外文字：\n" +
            "{\"bill\":true/false,\"expenseOrIncome\":\"expense/income/other\",\"money\":数值}\n" +
            "示例: {\"bill\":true,\"expenseOrIncome\":\"expense\",\"money\":23.5}";

    public OpenAiRecognizer(AiApiConfig config) {
        this.config = config;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    @Override
    public RecognitionResult recognize(String base64Image) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("max_tokens", 500);

            // 构建消息
            List<Map<String, Object>> messages = new ArrayList<>();

            // 用户消息（包含图片）
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");

            List<Map<String, Object>> content = new ArrayList<>();

            // 图片
            Map<String, Object> imageContent = new HashMap<>();
            Map<String, Object> imageUrl = new HashMap<>();
            imageUrl.put("url", "data:image/png;base64," + base64Image);
            imageContent.put("type", "image_url");
            imageContent.put("image_url", imageUrl);
            content.add(imageContent);

            // 文本
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", USER_PROMPT);
            content.add(textContent);

            userMessage.put("content", content);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            // 发送请求
            String jsonBody = gson.toJson(requestBody);
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), jsonBody);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(config.getApiUrl() + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json");

            // 根据提供商添加额外的 headers
            if (AiApiConfig.PROVIDER_ANTHROPIC.equals(config.getProvider())) {
                requestBuilder.addHeader("anthropic-version", "2023-06-01");
                requestBuilder.addHeader("x-api-key", config.getApiKey());
                // Anthropic 使用不同的端点
                requestBuilder.url(config.getApiUrl() + "/messages");
                requestBody.put("system", SYSTEM_PROMPT);
            }

            Request request = requestBuilder.post(body).build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    return RecognitionResult.error("请求失败: " + response.code() + " - " + responseBody);
                }

                return parseResponse(responseBody);
            }

        } catch (IOException e) {
            Log.e(TAG, "识别失败", e);
            return RecognitionResult.error("网络错误: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "识别异常", e);
            return RecognitionResult.error("识别异常: " + e.getMessage());
        }
    }

    @Override
    public RecognitionResult testConnection() {
        // 使用简单的测试提示
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("max_tokens", 10);

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "回复 OK");
            messages.add(userMessage);

            requestBody.put("messages", messages);

            String jsonBody = gson.toJson(requestBody);
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), jsonBody);

            Request request = new Request.Builder()
                    .url(config.getApiUrl() + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    return RecognitionResult.success(true, "test", 0);
                } else {
                    return RecognitionResult.error("连接失败: " + response.code() + " - " + responseBody);
                }
            }

        } catch (IOException e) {
            return RecognitionResult.error("连接失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isValid() {
        return config.isValid();
    }

    private RecognitionResult parseResponse(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            // 提取响应内容
            String content = "";
            if (json.has("choices")) {
                JsonArray choices = json.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    JsonObject message = choice.getAsJsonObject("message");
                    content = message.get("content").getAsString();
                }
            }

            // 解析 JSON 响应
            return parseBillResponse(content);

        } catch (Exception e) {
            Log.e(TAG, "解析响应失败", e);
            return RecognitionResult.error("解析响应失败: " + e.getMessage());
        }
    }

    private RecognitionResult parseBillResponse(String content) {
        try {
            // 提取 JSON
            int start = content.indexOf("{");
            int end = content.lastIndexOf("}");
            if (start != -1 && end != -1) {
                String jsonStr = content.substring(start, end + 1);
                JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

                boolean isBill = json.has("bill") && json.get("bill").getAsBoolean();
                String type = json.has("expenseOrIncome") ? 
                        json.get("expenseOrIncome").getAsString() : "other";
                double amount = json.has("money") ? 
                        json.get("money").getAsDouble() : 0;

                return RecognitionResult.success(isBill, type, amount);
            }
        } catch (Exception e) {
            Log.e(TAG, "解析账单JSON失败", e);
        }

        // 备用解析：正则匹配
        return parseWithRegex(content);
    }

    private RecognitionResult parseWithRegex(String content) {
        try {
            boolean isBill = false;
            String type = "other";
            double amount = 0;

            // 检查 bill
            if (content.contains("\"bill\":true") || content.contains("\"bill\": true")) {
                isBill = true;
            }

            // 检查类型
            if (content.contains("expense")) {
                type = "expense";
            } else if (content.contains("income")) {
                type = "income";
            }

            // 提取金额
            java.util.regex.Pattern pattern = 
                    java.util.regex.Pattern.compile("\\d+\\.?\\d*");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                amount = Double.parseDouble(matcher.group());
            }

            if (isBill && !type.equals("other")) {
                return RecognitionResult.success(isBill, type, amount);
            }

        } catch (Exception e) {
            Log.e(TAG, "正则解析失败", e);
        }

        return RecognitionResult.error("无法解析响应内容");
    }
}

package com.coderpage.mine.app.tally.sync;

import com.coderpage.mine.app.tally.config.NotionConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Notion API 客户端
 * 
 * @author Flandre Scarlet
 */
public class NotionApiClient {
    
    private static final String BASE_URL = "https://api.notion.com/v1/";
    private static final String NOTION_VERSION = "2022-06-28";
    
    private final OkHttpClient client;
    private final NotionConfig config;
    
    private static NotionApiClient instance;
    
    private NotionApiClient(NotionConfig config) {
        this.config = config;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public static synchronized NotionApiClient getInstance(NotionConfig config) {
        if (instance == null) {
            instance = new NotionApiClient(config);
        }
        return instance;
    }
    
    /**
     * 查询数据库中的所有记录
     */
    public String queryDatabase(String cursor) throws IOException {
        Map<String, Object> body = new HashMap<>();
        if (cursor != null && !cursor.isEmpty()) {
            body.put("start_cursor", cursor);
        }
        
        String jsonBody = new com.google.gson.Gson().toJson(body);
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), jsonBody);
        
        String url = BASE_URL + "databases/" + config.getDatabaseId() + "/query";
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + config.getIntegrationToken())
                .addHeader("Notion-Version", NOTION_VERSION)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }
    
    /**
     * 创建新记录
     */
    public String createPage(String pageData) throws IOException {
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), pageData);
        
        Request request = new Request.Builder()
                .url(BASE_URL + "pages")
                .addHeader("Authorization", "Bearer " + config.getIntegrationToken())
                .addHeader("Notion-Version", NOTION_VERSION)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }
    
    /**
     * 更新记录
     */
    public String updatePage(String pageId, String pageData) throws IOException {
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), pageData);
        
        Request request = new Request.Builder()
                .url(BASE_URL + "pages/" + pageId)
                .addHeader("Authorization", "Bearer " + config.getIntegrationToken())
                .addHeader("Notion-Version", NOTION_VERSION)
                .addHeader("Content-Type", "application/json")
                .patch(requestBody)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }
    
    /**
     * 验证配置是否有效
     */
    public boolean validateConfig() {
        try {
            // 尝试查询数据库
            queryDatabase(null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 验证数据库结构
     */
    public String getDatabaseSchema() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "databases/" + config.getDatabaseId())
                .addHeader("Authorization", "Bearer " + config.getIntegrationToken())
                .addHeader("Notion-Version", NOTION_VERSION)
                .get()
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }
}

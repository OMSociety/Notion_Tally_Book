package com.coderpage.mine.app.tally.ai;

import android.content.Context;
import android.content.SharedPreferences;

import com.coderpage.mine.persistence.database.MineDatabase;
import com.coderpage.mine.persistence.entity.KeyValue;

/**
 * AI API 配置管理
 * 
 * @author Flandre Scarlet
 */
public class AiApiConfig {
    
    private static final String KEY_AI_PROVIDER = "ai_provider";
    private static final String KEY_AI_API_URL = "ai_api_url";
    private static final String KEY_AI_API_KEY = "ai_api_key";
    private static final String KEY_AI_MODEL = "ai_model";
    
    // 提供商类型
    public static final String PROVIDER_SILICONFLOW = "siliconflow";
    public static final String PROVIDER_OPENAI = "openai";
    public static final String PROVIDER_ANTHROPIC = "anthropic";
    public static final String PROVIDER_CUSTOM = "custom";
    
    // 预设模板
    public static final ProviderTemplate[] PROVIDER_TEMPLATES = {
        new ProviderTemplate("SiliconFlow (推荐)", PROVIDER_SILICONFLOW, 
            "https://api.siliconflow.cn/v1", "Qwen/Qwen2.5-VL-72B-Instruct"),
        new ProviderTemplate("OpenAI", PROVIDER_OPENAI, 
            "https://api.openai.com/v1", "gpt-4o"),
        new ProviderTemplate("Claude (Anthropic)", PROVIDER_ANTHROPIC, 
            "https://api.anthropic.com/v1", "claude-sonnet-4-7-20250611"),
        new ProviderTemplate("自定义", PROVIDER_CUSTOM, "", "")
    };
    
    private String provider;
    private String apiUrl;
    private String apiKey;
    private String model;
    
    public static class ProviderTemplate {
        public String name;
        public String provider;
        public String defaultUrl;
        public String defaultModel;
        
        public ProviderTemplate(String name, String provider, String defaultUrl, String defaultModel) {
            this.name = name;
            this.provider = provider;
            this.defaultUrl = defaultUrl;
            this.defaultModel = defaultModel;
        }
    }
    
    public AiApiConfig() {
        this.provider = PROVIDER_SILICONFLOW;
        this.apiUrl = "";
        this.apiKey = "";
        this.model = "";
    }
    
    // Getters and Setters
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    // 验证配置是否完整
    public boolean isValid() {
        return apiUrl != null && !apiUrl.isEmpty() 
            && apiKey != null && !apiKey.isEmpty()
            && model != null && !model.isEmpty();
    }
    
    // 保存配置
    public void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ai_config", Context.MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_AI_PROVIDER, provider)
            .putString(KEY_AI_API_URL, apiUrl)
            .putString(KEY_AI_API_KEY, apiKey)
            .putString(KEY_AI_MODEL, model)
            .apply();
    }
    
    // 加载配置
    public static AiApiConfig load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ai_config", Context.MODE_PRIVATE);
        AiApiConfig config = new AiApiConfig();
        config.setProvider(prefs.getString(KEY_AI_PROVIDER, PROVIDER_SILICONFLOW));
        config.setApiUrl(prefs.getString(KEY_AI_API_URL, ""));
        config.setApiKey(prefs.getString(KEY_AI_API_KEY, ""));
        config.setModel(prefs.getString(KEY_AI_MODEL, ""));
        return config;
    }
    
    // 根据提供商获取模板
    public static ProviderTemplate getTemplate(String provider) {
        for (ProviderTemplate template : PROVIDER_TEMPLATES) {
            if (template.provider.equals(provider)) {
                return template;
            }
        }
        return PROVIDER_TEMPLATES[PROVIDER_TEMPLATES.length - 1]; // 返回自定义
    }
}

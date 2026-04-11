package com.coderpage.mine.app.tally.ai;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.databinding.ObservableField;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.coderpage.framework.BaseViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 设置 ViewModel
 * 
 * @author Flandre Scarlet
 */
public class AiSettingViewModel extends BaseViewModel {

    private static final String TAG = "AiSettingViewModel";

    public final ObservableField<String> apiUrl = new ObservableField<>();
    public final ObservableField<String> apiKey = new ObservableField<>();
    public final ObservableField<String> model = new ObservableField<>();
    public final ObservableField<String> selectedProvider = new ObservableField<>("siliconflow");
    public final ObservableField<Boolean> isLoading = new ObservableField<>(false);

    private final MutableLiveData<TestResult> testResult = new MutableLiveData<>();
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AiSettingViewModel(Application application) {
        super(application);
    }

    public LiveData<TestResult> getTestResult() {
        return testResult;
    }

    public int getProviderIndex() {
        String provider = selectedProvider.get();
        if (provider == null) provider = "siliconflow";
        
        for (int i = 0; i < AiApiConfig.PROVIDER_TEMPLATES.length; i++) {
            if (AiApiConfig.PROVIDER_TEMPLATES[i].provider.equals(provider)) {
                return i;
            }
        }
        return 0;
    }

    public AiApiConfig.ProviderTemplate[] getProviderTemplates() {
        return AiApiConfig.PROVIDER_TEMPLATES;
    }

    public void setApiUrl(String url) {
        apiUrl.set(url);
    }

    public void setApiKey(String key) {
        apiKey.set(key);
    }

    public void setModel(String modelName) {
        model.set(modelName);
    }

    public void setProviderIndex(int index) {
        if (index >= 0 && index < AiApiConfig.PROVIDER_TEMPLATES.length) {
            AiApiConfig.ProviderTemplate template = AiApiConfig.PROVIDER_TEMPLATES[index];
            selectedProvider.set(template.provider);
            if (apiUrl.get() == null || apiUrl.get().isEmpty()) {
                apiUrl.set(template.defaultUrl);
            }
            if (model.get() == null || model.get().isEmpty()) {
                model.set(template.defaultModel);
            }
        }
    }

    public void loadConfig() {
        AiApiConfig config = AiApiConfig.load(getApplication());
        selectedProvider.set(config.getProvider());
        apiUrl.set(config.getApiUrl());
        apiKey.set(config.getApiKey());
        model.set(config.getModel());
    }

    public void saveConfig() {
        AiApiConfig config = new AiApiConfig();
        config.setProvider(selectedProvider.get());
        config.setApiUrl(apiUrl.get());
        config.setApiKey(apiKey.get());
        config.setModel(model.get());
        config.save(getApplication());
    }

    public void testConnection() {
        isLoading.set(true);
        
        executor.execute(() -> {
            try {
                String url = apiUrl.get();
                String key = apiKey.get();
                String modelName = model.get();
                
                if (url == null || url.isEmpty() || key == null || key.isEmpty()) {
                    postResult(false, "请填写完整的 API 信息");
                    return;
                }

                // 简单的连接测试：发送一个简单的请求
                String testUrl = url;
                if (!testUrl.endsWith("/chat/completions")) {
                    testUrl = testUrl + "/chat/completions";
                }

                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/json"),
                    "{\"model\": \"" + modelName + "\", \"messages\": [{\"role\": \"user\", \"content\": \"hi\"}], \"max_tokens\": 5}"
                );
                
                okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(testUrl)
                    .addHeader("Authorization", "Bearer " + key)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        postResult(true, "连接成功！API 配置正确。");
                    } else {
                        postResult(false, "连接失败: " + response.code() + " " + response.message());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Test connection error", e);
                postResult(false, "连接失败: " + e.getMessage());
            }
        });
    }

    private void postResult(boolean success, String message) {
        mainHandler.post(() -> {
            isLoading.set(false);
            TestResult result = new TestResult();
            result.success = success;
            result.message = message;
            testResult.setValue(result);
        });
    }

    public static class TestResult {
        public boolean success;
        public String message;
    }
}

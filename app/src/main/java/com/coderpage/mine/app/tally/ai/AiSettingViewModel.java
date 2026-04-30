package com.coderpage.mine.app.tally.ai;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.databinding.ObservableField;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 设置 ViewModel
 * 
 * @author Flandre Scarlet
 */
public class AiSettingViewModel extends com.coderpage.framework.BaseViewModel {

    private static final String TAG = "AiSettingViewModel";

    // Observable Fields for Data Binding
    public final ObservableField<String> apiUrl = new ObservableField<>();
    public final ObservableField<String> apiKey = new ObservableField<>();
    public final ObservableField<String> model = new ObservableField<>();
    public final ObservableField<String> selectedProvider = new ObservableField<>("siliconflow");
    public final ObservableField<Boolean> isLoading = new ObservableField<>(false);

    // LiveData for test result
    private final MutableLiveData<TestResult> testResult = new MutableLiveData<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AiSettingViewModel(Application application) {
        super(application);
        loadConfig();
    }

    // Getters
    public LiveData<TestResult> getTestResult() {
        return testResult;
    }

    public AiApiConfig.ProviderTemplate[] getProviderTemplates() {
        return AiApiConfig.PROVIDER_TEMPLATES;
    }

    public int getProviderIndex() {
        String provider = selectedProvider.get();
        for (int i = 0; i < AiApiConfig.PROVIDER_TEMPLATES.length; i++) {
            if (AiApiConfig.PROVIDER_TEMPLATES[i].provider.equals(provider)) {
                return i;
            }
        }
        return 0;
    }

    public void setProviderIndex(int index) {
        if (index >= 0 && index < AiApiConfig.PROVIDER_TEMPLATES.length) {
            AiApiConfig.ProviderTemplate template = AiApiConfig.PROVIDER_TEMPLATES[index];
            selectedProvider.set(template.provider);
            // 如果是预设模板，自动填充地址和模型
            if (apiUrl.get() == null || apiUrl.get().isEmpty() || 
                !apiUrl.get().equals(template.defaultUrl)) {
                apiUrl.set(template.defaultUrl);
            }
            if (model.get() == null || model.get().isEmpty() ||
                !isPresetModel(model.get())) {
                model.set(template.defaultModel);
            }
        }
    }

    private boolean isPresetModel(String modelName) {
        for (AiApiConfig.ProviderTemplate template : AiApiConfig.PROVIDER_TEMPLATES) {
            if (template.defaultModel.equals(modelName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 加载配置
     */
    public void loadConfig() {
        AiApiConfig config = AiApiConfig.load(getApplication());
        selectedProvider.set(config.getProvider());
        apiUrl.set(config.getApiUrl());
        apiKey.set(config.getApiKey());
        model.set(config.getModel());
    }

    /**
     * 保存配置
     */
    public void saveConfig() {
        AiApiConfig config = new AiApiConfig();
        config.setProvider(selectedProvider.get());
        config.setApiUrl(apiUrl.get());
        config.setApiKey(apiKey.get());
        config.setModel(model.get());
        config.save(getApplication());
        Log.d(TAG, "配置已保存");
    }

    /**
     * 测试连接
     */
    public void testConnection() {
        isLoading.set(true);
        testResult.setValue(null);

        executor.execute(() -> {
            try {
                AiApiConfig config = new AiApiConfig();
                config.setProvider(selectedProvider.get());
                config.setApiUrl(apiUrl.get());
                config.setApiKey(apiKey.get());
                config.setModel(model.get());

                if (!config.isValid()) {
                    mainHandler.post(() -> {
                        isLoading.set(false);
                        testResult.setValue(new TestResult(false, "请填写完整的 API 配置"));
                    });
                    return;
                }

                AiRecognizer recognizer = AiRecognizerFactory.create(config);
                RecognitionResult result = recognizer.testConnection();

                mainHandler.post(() -> {
                    isLoading.set(false);
                    if (result.success) {
                        testResult.setValue(new TestResult(true, "连接成功！API 配置正确。"));
                    } else {
                        testResult.setValue(new TestResult(false, "连接失败: " + result.errorMessage));
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "测试连接异常", e);
                mainHandler.post(() -> {
                    isLoading.set(false);
                    testResult.setValue(new TestResult(false, "异常: " + e.getMessage()));
                });
            }
        });
    }

    /**
     * 测试结果
     */
    public static class TestResult {
        public boolean success;
        public String message;

        public TestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}

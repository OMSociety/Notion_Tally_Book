package com.coderpage.mine.app.tally.module.setting;

import android.app.Activity;
import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.content.Intent;
import android.databinding.ObservableField;
import androidx.annotation.NonNull;
import android.util.Log;

import com.coderpage.base.common.Callback;
import com.coderpage.base.common.IError;
import com.coderpage.base.utils.LogUtils;
import com.coderpage.framework.BaseViewModel;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.persistence.database.MineDatabase;
import com.coderpage.mine.persistence.entity.KeyValue;

import java.util.concurrent.ExecutorService;
import com.coderpage.mine.app.tally.config.NotionConfig;
import com.coderpage.mine.app.tally.sync.NotionSyncManager;
import java.util.concurrent.Executors;

/**
 * @author lc. 2019-04-27 09:59
 * @since 0.6.2
 */

public class SettingViewModel extends BaseViewModel {

    private static final String TAG = LogUtils.makeLogTag(SettingViewModel.class);


    // 创建一个单线程的执行器用于数据库操作
    private final ExecutorService mDatabaseExecutor = Executors.newSingleThreadExecutor();

    public SettingViewModel(Application application) {
        super(application);
        loadSettings();
    }


    // 使用 ObservableField 用于双向绑定
    public final ObservableField<String> apiKey = new ObservableField<>();
    public final ObservableField<String> aiModel = new ObservableField<>();
    public final ObservableField<Boolean> smsRecognitionEnabled = new ObservableField<>(false);
    public final ObservableField<String> detectionList = new ObservableField<>();
    // Notion 同步相关
    public final ObservableField<String> notionToken = new ObservableField<>();
    public final ObservableField<String> notionDatabaseId = new ObservableField<>();
    public final ObservableField<String> lastSyncTime = new ObservableField<>("从未同步");
    public final ObservableField<Boolean> isSyncing = new ObservableField<>(false);
    public final ObservableField<Boolean> autoSyncEnabled = new ObservableField<>(false);
    
    private NotionSyncManager syncManager;


    // 保留 MutableLiveData 用于内部状态管理（如果需要）
    private MutableLiveData<String> mApiKeyLiveData = new MutableLiveData<>();
    private MutableLiveData<String> mAiModelLiveData = new MutableLiveData<>();
    // 添加 ObservableField 用于短信识别开关状态
    private MutableLiveData<Boolean> mSmsRecognitionEnabledLiveData = new MutableLiveData<>();
    private MutableLiveData<String> mDetectionListLiveData = new MutableLiveData<>();

    public LiveData<String> getDetectionListLiveData() {
        return mDetectionListLiveData;
    }
    public LiveData<Boolean> getSmsRecognitionEnabledLiveData() {
        return mSmsRecognitionEnabledLiveData;
    }
    public LiveData<String> getApiKeyLiveData() {
        return mApiKeyLiveData;
    }

    public LiveData<String> getAiModelLiveData() {
        return mAiModelLiveData;
    }

    /**
     * 保存 API 密钥到数据库
     * @param apiKey API 密钥
     */
    public void saveApiKey(String apiKey) {
        this.apiKey.set(apiKey);
        mApiKeyLiveData.setValue(apiKey);
        saveSetting(SettingWorkerConstant.KEY_API_KEY, apiKey);
    }

    /**
     * 保存检测名单到数据库
     * @param detectionList 检测名单字符串
     */
    public void saveDetectionList(String detectionList) {
        this.detectionList.set(detectionList);
        mDetectionListLiveData.setValue(detectionList);
        saveSetting(SettingWorkerConstant.KEY_DETECTION_LIST, detectionList);
    }

    public void saveSmsRecognitionEnabled(boolean enabled) {
        this.smsRecognitionEnabled.set(enabled);
        mSmsRecognitionEnabledLiveData.setValue(enabled);
        saveSetting(SettingWorkerConstant.KEY_SMS_RECOGNITION_ENABLED, String.valueOf(enabled));
    }

    /**
     * 保存 AI 模型名称到数据库
     * @param aiModel AI 模型名称
     */
    public void saveAiModel(String aiModel) {
        this.aiModel.set(aiModel);
        mAiModelLiveData.setValue(aiModel);
        saveSetting(SettingWorkerConstant.KEY_AI_MODEL, aiModel);
    }

    /**
     * 从数据库加载设置
     */
    
    /**
     * 保存 Notion Token
     */
    public void saveNotionToken(String token) {
        this.notionToken.set(token);
        NotionConfig.getInstance(getApplication()).setIntegrationToken(token);
    }
    
    /**
     * 保存 Notion Database ID
     */
    public void saveNotionDatabaseId(String databaseId) {
        this.notionDatabaseId.set(databaseId);
        NotionConfig.getInstance(getApplication()).setDatabaseId(databaseId);
    }
    
    /**
     * 保存启动时自动同步设置
     */
    public void saveAutoSyncEnabled(boolean enabled) {
        this.autoSyncEnabled.set(enabled);
        NotionConfig.getInstance(getApplication()).setAutoSync(enabled);
    }
    
    /**
     * 开始同步
     */
    public synchronized void startSync(Activity activity) {
        if (isSyncing.get() != null && isSyncing.get()) {
            return;
        }

        NotionConfig config = NotionConfig.getInstance(getApplication());
        if (!config.isConfigured()) {
            return;
        }

        if (syncManager == null) {
            syncManager = new NotionSyncManager(getApplication());
        }

        isSyncing.set(true);
        
        syncManager.setSyncListener(new NotionSyncManager.SyncListener() {
            @Override
            public void onSyncStart() {
            }
            
            @Override
            public void onSyncProgress(int current, int total, String message) {
            }
            
            @Override
            public void onSyncComplete(NotionSyncManager.SyncResult result) {
                isSyncing.set(false);
                updateLastSyncTime();
            }
            
            @Override
            public void onSyncError(String error) {
                isSyncing.set(false);
            }
        });
        
        syncManager.startSync();
    }
    
    private void updateLastSyncTime() {
        NotionConfig config = NotionConfig.getInstance(getApplication());
        long lastSync = config.getLastSyncTime();
        if (lastSync > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            lastSyncTime.set(sdf.format(new java.util.Date(lastSync)));
        } else {
            lastSyncTime.set("从未同步");
        }
    }

    private void loadSettings() {
        mDatabaseExecutor.execute(() -> {
            try {
                MineDatabase db = MineDatabase.getInstance();

                KeyValue apiKeySetting = db.keyValueDao().query(SettingWorkerConstant.KEY_API_KEY);
                KeyValue aiModelSetting = db.keyValueDao().query(SettingWorkerConstant.KEY_AI_MODEL);
                KeyValue smsRecognitionEnabledSetting = db.keyValueDao().query(SettingWorkerConstant.KEY_SMS_RECOGNITION_ENABLED); // 新增
                KeyValue detectionListSetting = db.keyValueDao().query(SettingWorkerConstant.KEY_DETECTION_LIST);
                
                // 加载自动同步设置
                NotionConfig notionConfig = NotionConfig.getInstance(getApplication());
                boolean autoSyncEnabledValue = notionConfig.isAutoSyncEnabled();

                runOnUiThread(() -> {
                    String apiKeyValue = apiKeySetting != null ? apiKeySetting.getValue() : "";
                    String aiModelValue = aiModelSetting != null ? aiModelSetting.getValue() : "";
                    String detectionListValue = detectionListSetting != null ? detectionListSetting.getValue() : "";
                    boolean smsRecognitionEnabledValue = smsRecognitionEnabledSetting != null ?
                            Boolean.parseBoolean(smsRecognitionEnabledSetting.getValue()) : false; // 新增

                    // 同时设置 ObservableField 和 LiveData
                    apiKey.set(apiKeyValue);
                    aiModel.set(aiModelValue);
                    detectionList.set(detectionListValue);
                    smsRecognitionEnabled.set(smsRecognitionEnabledValue); // 新增
                    autoSyncEnabled.set(autoSyncEnabledValue); // 新增
                    mApiKeyLiveData.setValue(apiKeyValue);
                    mAiModelLiveData.setValue(aiModelValue);
                    mDetectionListLiveData.setValue(detectionListValue);
                    mSmsRecognitionEnabledLiveData.setValue(smsRecognitionEnabledValue); // 新增
                });
            } catch (Exception e) {
                Log.e(TAG, "加载设置失败", e);
            }
        });
    }

    /**
     * 保存设置到数据库
     * @param key 设置键
     * @param value 设置值
     */
    private void saveSetting(String key, String value) {
        mDatabaseExecutor.execute(() -> {
            try {
                MineDatabase db = MineDatabase.getInstance();
                KeyValue setting = new KeyValue(key, "");
                setting.setKey(key);
                setting.setValue(value != null ? value : "");
                db.keyValueDao().delete(key);
                db.keyValueDao().insert(setting);
            } catch (Exception e) {
                Log.e(TAG, "保存设置失败: " + key, e);
            }
        });
    }

    public void clearAllRecords(Callback<Boolean, IError> callback) {
        mDatabaseExecutor.execute(() -> {
            try {
                MineDatabase db = MineDatabase.getInstance();
                // 调用 RecordDao 的 deleteAll 方法删除所有记录
                TallyDatabase.getInstance().recordDao().deleteAll();
                // 成功回调
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.success(true));
                }

            } catch (Exception e) {
                Log.e(TAG, "清除所有账单记录失败", e);
                // 失败回调
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.failure(new IError() {
                        @Override
                        public String msg() {
                            return "清除所有账单记录失败: " + e.getMessage();
                        }

                        @Override
                        public int code() {
                            return -1;
                        }
                    }));
                }
            }
        });
    }




    ///////////////////////////////////////////////////////////////////////////
    // 生命周期
    ///////////////////////////////////////////////////////////////////////////

    protected void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

    }
    public void onRequestPermissionsResult(Activity activity,
                                           int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        // 关闭线程池
        if (mDatabaseExecutor != null && !mDatabaseExecutor.isShutdown()) {
            mDatabaseExecutor.shutdown();
        }
    }

}

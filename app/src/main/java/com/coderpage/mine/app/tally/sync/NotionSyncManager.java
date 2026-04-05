package com.coderpage.mine.app.tally.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.coderpage.mine.app.tally.config.NotionConfig;
import com.coderpage.mine.persistence.entity.TallyRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Notion 同步管理器
 *
 * 职责：
 * <ul>
 *   <li>管理同步方向（本地→Notion / Notion→本地 / 双向）</li>
 *   <li>通过 NotionApiClient 与 Notion API 通信</li>
 *   <li>通过 SyncRepository 操作本地数据库（遵循分层架构）</li>
 *   <li>通过 NotionDatabaseValidator 校验数据库模板</li>
 * </ul>
 *
 * <b>同步流程：</b>
 * 1. 检查配置完整性 → 2. 校验数据库字段模板 → 3. 执行同步 → 4. 回调结果
 *
 * 使用方式：
 * <pre>
 *   NotionSyncManager manager = new NotionSyncManager(context);
 *   manager.setSyncListener(new SyncListener() { ... });
 *   manager.sync();
 * </pre>
 *
 * @author abner-l
 * @since 0.7.5
 */
public class NotionSyncManager {

    /** 同步方向：本地 → Notion */
    public static final int SYNC_TO_NOTION = 0;
    /** 同步方向：Notion → 本地 */
    public static final int SYNC_FROM_NOTION = 1;
    /** 同步方向：双向同步 */
    public static final int SYNC_BIDIRECTIONAL = 2;

    /** 单次同步最多重试次数 */
    private static final int MAX_RETRY_COUNT = 3;

    private final NotionConfig config;
    private final NotionApiClient apiClient;
    private final NotionDatabaseValidator validator;
    private final SyncRepository syncRepository;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private SyncListener listener;

    /**
     * 同步进度/结果监听器
     */
    public interface SyncListener {
        /** 同步开始 */
        void onSyncStart();
        /** 同步进度更新 */
        void onSyncProgress(int current, int total, String message);
        /** 同步完成 */
        void onSyncComplete(int synced, int failed);
        /** 同步出错 */
        void onSyncError(String error);
    }

    /**
     * 构造同步管理器
     */
    public NotionSyncManager(Context context) {
        this.config = new NotionConfig(context);
        this.apiClient = new NotionApiClient();
        this.validator = new NotionDatabaseValidator();
        this.syncRepository = new SyncRepository();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());

        if (config.isConfigured()) {
            apiClient.setCredentials(config.getNotionToken(), config.getDatabaseId());
        }
    }

    /**
     * 设置同步进度监听器
     */
    public void setSyncListener(SyncListener l) {
        this.listener = l;
    }

    /**
     * 检查 Notion 配置是否完整
     */
    public boolean isConfigured() {
        return config.isConfigured();
    }

    /**
     * 获取配置管理器引用
     */
    public NotionConfig getConfig() {
        return config;
    }

    /**
     * 更新 Notion 凭证
     */
    public void updateCredentials(String token, String databaseId) {
        config.setNotionToken(token);
        config.setDatabaseId(databaseId);
        apiClient.setCredentials(token, databaseId);
    }

    /**
     * 测试 Notion 连接并校验数据库字段模板
     */
    public void testConnection(ConnectionTestCallback callback) {
        if (!config.isConfigured()) {
            notifyError("请先填写 Token 和 Database ID");
            return;
        }
        if (listener != null) {
            mainHandler.post(() -> listener.onSyncStart());
        }

        validator.validate(config.getNotionToken(), config.getDatabaseId(),
                new NotionDatabaseValidator.ValidationCallback() {
            @Override
            public void onResult(NotionDatabaseValidator.ValidationResult result) {
                mainHandler.post(() -> {
                    if (result.valid) {
                        notifyComplete(0, 0);
                    } else {
                        notifyError(result.summary);
                    }
                    callback.onResult(result);
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    notifyError(error);
                    callback.onError(error);
                });
            }
        });
    }

    /**
     * 发起同步，根据配置的同步方向分发到对应方法
     */
    public void sync() {
        if (!config.isConfigured()) {
            if (listener != null) {
                mainHandler.post(() -> listener.onSyncError("请先配置 Notion API Token 和 Database ID"));
            }
            return;
        }

        if (listener != null) {
            mainHandler.post(() -> listener.onSyncStart());
            mainHandler.post(() -> listener.onSyncProgress(0, 1, "正在校验数据库模板..."));
        }

        validator.validate(config.getNotionToken(), config.getDatabaseId(),
                new NotionDatabaseValidator.ValidationCallback() {
            @Override
            public void onResult(NotionDatabaseValidator.ValidationResult result) {
                if (!result.valid) {
                    mainHandler.post(() -> listener.onSyncError(result.summary));
                    return;
                }
                mainHandler.post(() -> listener.onSyncProgress(0, 1, "模板校验通过，开始同步..."));
                doSync();
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> listener.onSyncError(error));
            }
        });
    }

    private void doSync() {
        switch (config.getSyncDirection()) {
            case SYNC_TO_NOTION:
                syncToNotion();
                break;
            case SYNC_FROM_NOTION:
                syncFromNotion();
                break;
            case SYNC_BIDIRECTIONAL:
                syncBidirectional();
                break;
            default:
                if (listener != null) {
                    mainHandler.post(() -> listener.onSyncError("未知的同步方向: " + config.getSyncDirection()));
                }
                break;
        }
    }

    // ==================== 同步方向实现 ====================

    private void syncToNotion() {
        executor.execute(() -> {
            List<TallyRecord> localRecords = syncRepository.queryUnsyncedRecords();
            if (localRecords.isEmpty()) {
                config.setLastSyncTime(System.currentTimeMillis());
                notifyComplete(0, 0);
                return;
            }

            int total = localRecords.size();
            AtomicInteger synced = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(total);

            for (int i = 0; i < total; i++) {
                final TallyRecord r = localRecords.get(i);
                final int index = i;
                notifyProgress(index + 1, total, "正在上传: " + (r.getRemark() != null ? r.getRemark() : "(无备注)"));
                uploadWithRetry(r, 0, synced, failed, latch);
            }

            try {
                latch.await(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            config.setLastSyncTime(System.currentTimeMillis());
            notifyComplete(synced.get(), failed.get());
        });
    }

    private void uploadWithRetry(TallyRecord record, int retryCount,
                                 AtomicInteger synced, AtomicInteger failed, CountDownLatch latch) {
        apiClient.createRecord(record, new NotionApiClient.NotionCallback<String>() {
            @Override
            public void onSuccess(String notionId) {
                syncRepository.markAsSynced(record.getId(), notionId);
                synced.incrementAndGet();
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                if (retryCount < MAX_RETRY_COUNT) {
                    uploadWithRetry(record, retryCount + 1, synced, failed, latch);
                } else {
                    failed.incrementAndGet();
                    latch.countDown();
                }
            }
        });
    }

    private void syncFromNotion() {
        apiClient.queryDatabase(new NotionApiClient.NotionCallback<List<TallyRecord>>() {
            @Override
            public void onSuccess(List<TallyRecord> records) {
                executor.execute(() -> {
                    int added = 0;
                    int updated = 0;
                    int total = records.size();

                    for (int i = 0; i < total; i++) {
                        TallyRecord nr = records.get(i);
                        notifyProgress(i + 1, total, "处理: " + nr.getRemark());

                        TallyRecord local = syncRepository.findByNotionId(nr.getNotionId());
                        if (local == null) {
                            syncRepository.insertFromNotion(nr);
                            added++;
                        } else if (nr.getLastModified() > local.getLastModified()) {
                            syncRepository.updateFromNotion(nr);
                            updated++;
                        }
                    }

                    config.setLastSyncTime(System.currentTimeMillis());
                    notifyComplete(added, updated);
                });
            }

            @Override
            public void onError(String error) {
                notifyError(error);
            }
        });
    }

    private void syncBidirectional() {
        apiClient.queryDatabase(new NotionApiClient.NotionCallback<List<TallyRecord>>() {
            @Override
            public void onSuccess(List<TallyRecord> notionRecords) {
                executor.execute(() -> mergeRecords(notionRecords));
            }

            @Override
            public void onError(String error) {
                notifyError(error);
            }
        });
    }

    private void mergeRecords(List<TallyRecord> notionRecords) {
        int localUpdated = 0;
        int notionUpdated = 0;
        int total = notionRecords.size();
        List<TallyRecord> toUpload = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            TallyRecord nr = notionRecords.get(i);
            notifyProgress(i + 1, total, "合并: " + nr.getRemark());

            TallyRecord local = syncRepository.findByNotionId(nr.getNotionId());
            if (local == null) {
                syncRepository.insertFromNotion(nr);
                localUpdated++;
            } else if (nr.getLastModified() > local.getLastModified()) {
                syncRepository.updateFromNotion(nr);
                localUpdated++;
            } else if (local.getLastModified() > nr.getLastModified() && !local.isSynced()) {
                toUpload.add(local);
            }
        }

        if (!toUpload.isEmpty()) {
            AtomicInteger successCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(toUpload.size());

            for (TallyRecord r : toUpload) {
                apiClient.updateRecord(r.getNotionId(), r, new NotionApiClient.NotionCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (result) {
                            syncRepository.markAsSynced(r.getId(), r.getNotionId());
                            successCount.incrementAndGet();
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onError(String error) {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await(5, TimeUnit.MINUTES);
                notionUpdated = successCount.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        config.setLastSyncTime(System.currentTimeMillis());
        notifyComplete(localUpdated, notionUpdated);
    }

    // ==================== 回调辅助 ====================

    private void notifyProgress(int current, int total, String message) {
        if (listener != null) {
            mainHandler.post(() -> listener.onSyncProgress(current, total, message));
        }
    }

    private void notifyComplete(int synced, int failed) {
        if (listener != null) {
            mainHandler.post(() -> listener.onSyncComplete(synced, failed));
        }
    }

    private void notifyError(String error) {
        if (listener != null) {
            mainHandler.post(() -> listener.onSyncError(error));
        }
    }

    /**
     * 连接测试结果回调
     */
    public interface ConnectionTestCallback {
        void onResult(NotionDatabaseValidator.ValidationResult result);
        void onError(String error);
    }

    /**
     * 释放资源
     */
    public void release() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}

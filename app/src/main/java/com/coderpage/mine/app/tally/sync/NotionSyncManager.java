package com.coderpage.mine.app.tally.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.coderpage.mine.app.tally.config.NotionConfig;
import com.coderpage.mine.persistence.entity.TallyRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
 *   <li>处理双向同步中的数据冲突</li>
 * </ul>
 *
 * <b>同步流程：</b>
 * 1. 检查配置完整性 → 2. 校验数据库字段模板 → 3. 执行同步 → 4. 回调结果
 *
 * <b>冲突处理策略：</b>
 * <ul>
 *   <li>LOCAL_WINS: 本地数据优先，忽略 Notion 的修改</li>
 *   <li>NOTION_WINS: Notion 数据优先，覆盖本地修改</li>
 *   <li>MERGE_FIELDS: 字段级合并，各自保留不同字段的修改</li>
 *   <li>MANUAL: 记录冲突，等待用户手动处理</li>
 * </ul>
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

    /** Log tag */
    private static final String TAG = "NotionSyncManager";

    private final NotionConfig config;
    private final NotionApiClient apiClient;
    private final NotionDatabaseValidator validator;
    private final SyncRepository syncRepository;
    private final SyncHistoryManager historyManager;
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
        this.historyManager = new SyncHistoryManager(context);
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
            historyManager.logSync(System.currentTimeMillis(), "TO_NOTION", synced.get(), failed.get());
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
                    Log.e(TAG, "Upload failed after " + MAX_RETRY_COUNT + " retries: " + error);
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
                    historyManager.logSync(System.currentTimeMillis(), "FROM_NOTION", added, updated);
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

    // ==================== 冲突检测与合并 ====================

    /**
     * 冲突解决策略
     */
    public enum ConflictResolution {
        LOCAL_WINS,      // 本地优先
        NOTION_WINS,     // Notion优先
        MERGE_FIELDS,     // 字段级合并
        MANUAL           // 手动解决
    }

    /**
     * 冲突记录详情
     */
    public static class SyncConflict {
        public final String notionId;
        public final TallyRecord localRecord;
        public final TallyRecord notionRecord;
        public final List<String> conflictingFields;
        public final long localModified;
        public final long notionModified;
        public final ConflictResolution resolution;
        public TallyRecord resolvedRecord;

        private SyncConflict(Builder builder) {
            this.notionId = builder.notionId;
            this.localRecord = builder.localRecord;
            this.notionRecord = builder.notionRecord;
            this.conflictingFields = builder.conflictingFields;
            this.localModified = builder.localModified;
            this.notionModified = builder.notionModified;
            this.resolution = builder.resolution;
            this.resolvedRecord = builder.resolvedRecord;
        }

        public static class Builder {
            public String notionId;
            public TallyRecord localRecord;
            public TallyRecord notionRecord;
            public List<String> conflictingFields = new ArrayList<>();
            public long localModified;
            public long notionModified;
            public ConflictResolution resolution = ConflictResolution.NOTION_WINS;
            public TallyRecord resolvedRecord;

            public Builder notionId(String notionId) { this.notionId = notionId; return this; }
            public Builder localRecord(TallyRecord r) { this.localRecord = r; return this; }
            public Builder notionRecord(TallyRecord r) { this.notionRecord = r; return this; }
            public Builder conflictingFields(List<String> f) { this.conflictingFields = f; return this; }
            public Builder localModified(long t) { this.localModified = t; return this; }
            public Builder notionModified(long t) { this.notionModified = t; return this; }
            public Builder resolution(ConflictResolution r) { this.resolution = r; return this; }
            public Builder resolvedRecord(TallyRecord r) { this.resolvedRecord = r; return this; }
            public SyncConflict build() { return new SyncConflict(this); }
        }
    }

    /**
     * 检测记录间的冲突
     * 
     * @param local 本地记录
     * @param notion Notion记录
     * @return 冲突详情，如果无冲突返回null
     */
    private SyncConflict detectConflict(TallyRecord local, TallyRecord notion) {
        if (local == null || notion == null) return null;
        
        List<String> conflicts = new ArrayList<>();
        
        // 字段级冲突检测
        if (local.getAmount() != notion.getAmount()) {
            conflicts.add("amount");
        }
        if (!Objects.equals(local.getCategory(), notion.getCategory())) {
            conflicts.add("category");
        }
        if (!Objects.equals(local.getRemark(), notion.getRemark())) {
            conflicts.add("remark");
        }
        
        if (conflicts.isEmpty()) return null;
        
        Log.w(TAG, "Conflict detected for " + notion.getNotionId() + ": " + conflicts);
        
        return new SyncConflict.Builder()
            .notionId(notion.getNotionId())
            .localRecord(local)
            .notionRecord(notion)
            .conflictingFields(conflicts)
            .localModified(local.getLastModified())
            .notionModified(notion.getLastModified())
            .resolution(config.getConflictResolution())
            .build();
    }

    /**
     * 根据冲突策略解决冲突
     */
    private TallyRecord resolveConflict(SyncConflict conflict) {
        switch (conflict.resolution) {
            case LOCAL_WINS:
                // 返回本地记录，保持本地修改
                return conflict.localRecord;
                
            case NOTION_WINS:
                // 返回Notion记录，使用Notion的修改
                return conflict.notionRecord;
                
            case MERGE_FIELDS:
                // 字段级合并：取较新字段的值
                return mergeFields(conflict.localRecord, conflict.notionRecord);
                
            case MANUAL:
            default:
                // 手动模式：记录冲突但不自动解决
                historyManager.logConflict(conflict);
                return conflict.notionRecord; // 暂时用Notion版本
        }
    }

    /**
     * 字段级合并：对于每个字段，取修改时间较新的值
     */
    private TallyRecord mergeFields(TallyRecord local, TallyRecord notion) {
        // 复制本地记录作为基础
        TallyRecord merged = TallyRecord.fromEntity(local.getEntity());
        merged.setNotionId(notion.getNotionId());
        merged.setLastModified(System.currentTimeMillis());
        
        // 金额：取较新修改的值
        // 这里简化处理，实际可根据每个字段的修改时间
        if (notion.getLastModified() > local.getLastModified()) {
            merged.setAmount(notion.getAmount());
        }
        
        // 分类：同上
        if (notion.getLastModified() > local.getLastModified()) {
            merged.setCategory(notion.getCategory());
        }
        
        // 备注：同上
        if (notion.getLastModified() > local.getLastModified()) {
            merged.setRemark(notion.getRemark());
        }
        
        return merged;
    }

    /**
     * 改进的双向同步合并逻辑
     */
    private void mergeRecords(List<TallyRecord> notionRecords) {
        int localUpdated = 0;
        int notionUpdated = 0;
        int conflictsResolved = 0;
        int total = notionRecords.size();
        List<TallyRecord> toUpload = new ArrayList<>();
        List<SyncConflict> allConflicts = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            TallyRecord nr = notionRecords.get(i);
            notifyProgress(i + 1, total, "合并: " + nr.getRemark());

            TallyRecord local = syncRepository.findByNotionId(nr.getNotionId());
            
            if (local == null) {
                // Notion有新记录，本地没有，直接插入
                syncRepository.insertFromNotion(nr);
                localUpdated++;
            } else {
                // 检测冲突
                SyncConflict conflict = detectConflict(local, nr);
                
                if (conflict != null) {
                    // 存在冲突，按策略解决
                    TallyRecord resolved = resolveConflict(conflict);
                    conflict.resolvedRecord = resolved;
                    allConflicts.add(conflict);
                    
                    // 更新本地
                    syncRepository.updateFromNotion(resolved);
                    localUpdated++;
                    conflictsResolved++;
                    
                    // 如果resolved来自本地，需要上传到Notion
                    if (conflict.resolution == ConflictResolution.LOCAL_WINS 
                        || conflict.resolution == ConflictResolution.MERGE_FIELDS) {
                        if (!local.isSynced()) {
                            toUpload.add(local);
                        }
                    }
                } else if (nr.getLastModified() > local.getLastModified()) {
                    // 无冲突但Notion版本更新
                    syncRepository.updateFromNotion(nr);
                    localUpdated++;
                } else if (local.getLastModified() > nr.getLastModified() && !local.isSynced()) {
                    // 本地更新了但未同步到Notion
                    toUpload.add(local);
                }
            }
        }

        // 上传需要同步到Notion的记录
        if (!toUpload.isEmpty()) {
            AtomicInteger successCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(toUpload.size());

            for (TallyRecord r : toUpload) {
                notifyProgress(total, total, "上传: " + r.getRemark());
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
                        Log.e(TAG, "Update record failed: " + error);
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
        
        // 记录同步历史
        historyManager.logSync(
            System.currentTimeMillis(), 
            "BIDIRECTIONAL", 
            localUpdated, 
            notionUpdated
        );
        
        // 记录冲突历史
        if (!allConflicts.isEmpty()) {
            historyManager.logConflicts(allConflicts);
        }
        
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

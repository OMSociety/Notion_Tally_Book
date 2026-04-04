package com.coderpage.mine.app.tally.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.coderpage.mine.app.tally.config.NotionConfig;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.dao.RecordDao;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;
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
 *   <li>将 TallyRecord 与本地 RecordEntity 互相转换</li>
 *   <li>通过 NotionApiClient 与 Notion API 通信</li>
 *   <li>通过 RecordDao 操作本地 SQLite 数据库</li>
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

    private final NotionConfig config;
    private final NotionApiClient apiClient;
    private final RecordDao recordDao;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private SyncListener listener;

    /**
     * 同步进度/结果监听器
     */
    public interface SyncListener {
        /** 同步开始 */
        void onSyncStart();
        /** 同步进度更新
         * @param current 当前处理到第几条
         * @param total   总共要处理多少条
         * @param message 当前正在处理的内容描述
         */
        void onSyncProgress(int current, int total, String message);
        /** 同步完成
         * @param synced  成功同步的记录数
         * @param failed  失败的记录数
         */
        void onSyncComplete(int synced, int failed);
        /** 同步出错
         * @param error 错误描述
         */
        void onSyncError(String error);
    }

    /**
     * 构造同步管理器
     *
     * @param context Android Context
     */
    public NotionSyncManager(Context context) {
        this.config = new NotionConfig(context);
        this.apiClient = new NotionApiClient();
        this.recordDao = TallyDatabase.getInstance().recordDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());

        if (config.isConfigured()) {
            apiClient.setCredentials(config.getNotionToken(), config.getDatabaseId());
        }
    }

    /**
     * 设置同步进度监听器
     *
     * @param l 监听器实例，传入 null 可清除监听
     */
    public void setSyncListener(SyncListener l) {
        this.listener = l;
    }

    /**
     * 检查 Notion 配置是否完整（Token 和 Database ID 均已配置）
     *
     * @return true 表示已配置，可发起同步
     */
    public boolean isConfigured() {
        return config.isConfigured();
    }

    /**
     * 获取配置管理器引用（用于在 UI 层读取配置状态）
     *
     * @return NotionConfig 实例
     */
    public NotionConfig getConfig() {
        return config;
    }

    /**
     * 更新 Notion 凭证
     *
     * @param token      Notion Integration Token
     * @param databaseId Notion Database ID
     */
    public void updateCredentials(String token, String databaseId) {
        config.setNotionToken(token);
        config.setDatabaseId(databaseId);
        apiClient.setCredentials(token, databaseId);
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
        }
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

    /**
     * 同步方向：本地 → Notion
     * 遍历所有未同步的本地记录，逐条上传到 Notion
     */
    private void syncToNotion() {
        executor.execute(() -> {
            List<TallyRecord> localRecords = getLocalUnsyncedRecords();
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

                // 带重试的上传
                uploadWithRetry(r, 0, synced, failed, latch);
            }

            try {
                // 等待所有异步任务完成，最多等 5 分钟
                latch.await(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            config.setLastSyncTime(System.currentTimeMillis());
            notifyComplete(synced.get(), failed.get());
        });
    }

    /**
     * 带重试的 Notion 上传
     *
     * @param record   要上传的记录
     * @param retryCount 当前重试次数
     * @param synced   成功计数器
     * @param failed   失败计数器
     * @param latch    用于等待完成的 CountDownLatch
     */
    private void uploadWithRetry(TallyRecord record, int retryCount,
                                 AtomicInteger synced, AtomicInteger failed, CountDownLatch latch) {
        apiClient.createRecord(record, new NotionApiClient.NotionCallback<String>() {
            @Override
            public void onSuccess(String notionId) {
                // 将本地记录的 sync_id 更新为 Notion 返回的 page ID
                updateLocalRecordSynced(record.getId(), notionId);
                synced.incrementAndGet();
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                if (retryCount < MAX_RETRY_COUNT) {
                    // 重试一次
                    uploadWithRetry(record, retryCount + 1, synced, failed, latch);
                } else {
                    failed.incrementAndGet();
                    latch.countDown();
                }
            }
        });
    }

    /**
     * 同步方向：Notion → 本地
     * 从 Notion 查询所有记录，逐一写入或更新本地数据库
     */
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

                        TallyRecord local = findLocalByNotionId(nr.getNotionId());
                        if (local == null) {
                            // Notion 有、本地没有 → 新增
                            addLocalRecord(nr);
                            added++;
                        } else if (nr.getLastModified() > local.getLastModified()) {
                            // Notion 更新更新 → 覆盖本地
                            updateLocalRecord(nr);
                            updated++;
                        }
                        // else: 本地更新更新 → 保留本地（不处理）
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

    /**
     * 同步方向：双向同步
     * <ol>
     *   <li>拉取 Notion 所有记录</li>
     *   <li>与本地记录按 lastModified 时间戳合并</li>
     *   <li>将本地更新的记录推送回 Notion</li>
     * </ol>
     */
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

    /**
     * 双向同步核心逻辑：合并本地与 Notion 记录
     *
     * <b>冲突策略</b>：以 lastModified 为准，保留较新的那条记录。
     * <ul>
     *   <li>本地没有、Notion 有 → 从 Notion 导入</li>
     *   <li>都有，Notion 较新 → 更新本地</li>
     *   <li>都有，本地较新且未同步 → 推送至 Notion</li>
     * </ul>
     *
     * @param notionRecords 从 Notion 拉取的记录列表
     */
    private void mergeRecords(List<TallyRecord> notionRecords) {
        int localUpdated = 0;
        int notionUpdated = 0;
        int total = notionRecords.size();
        List<TallyRecord> toUpload = new ArrayList<>();

        // 第一轮：处理 Notion → 本地
        for (int i = 0; i < total; i++) {
            TallyRecord nr = notionRecords.get(i);
            notifyProgress(i + 1, total, "合并: " + nr.getRemark());

            TallyRecord local = findLocalByNotionId(nr.getNotionId());
            if (local == null) {
                // Notion 独有 → 新增到本地
                addLocalRecord(nr);
                localUpdated++;
            } else if (nr.getLastModified() > local.getLastModified()) {
                // Notion 更新更新 → 覆盖本地
                updateLocalRecord(nr);
                localUpdated++;
            } else if (local.getLastModified() > nr.getLastModified() && !local.isSynced()) {
                // 本地更新更新且未同步 → 等待上传到 Notion
                toUpload.add(local);
            }
        }

        // 第二轮：上传本地较新的未同步记录到 Notion
        if (!toUpload.isEmpty()) {
            AtomicInteger successCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(toUpload.size());

            for (TallyRecord r : toUpload) {
                apiClient.updateRecord(r.getNotionId(), r, new NotionApiClient.NotionCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (result) {
                            // 更新本地记录的同步状态为已同步
                            updateLocalRecordSynced(r.getId(), r.getNotionId());
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

    // ==================== 本地数据库操作 ====================

    /**
     * 查询本地所有未同步到 Notion 的记录
     *
     * 筛选条件：record_sync_id 为空或 record_sync_status != 1
     *
     * @return 未同步的记录列表（按时间升序）
     */
    private List<TallyRecord> getLocalUnsyncedRecords() {
        List<RecordEntity> entities = recordDao.queryAll();
        List<TallyRecord> result = new ArrayList<>();
        if (entities == null) return result;

        for (RecordEntity e : entities) {
            // record_sync_status == 0 表示未同步
            if (e.getSyncStatus() == 0) {
                result.add(entityToTallyRecord(e));
            }
        }
        return result;
    }

    /**
     * 将已成功上传到 Notion 的本地记录标记为已同步
     *
     * @param localId   本地记录 ID（record_id）
     * @param notionId  Notion Page ID（写入 record_sync_id）
     */
    private void updateLocalRecordSynced(long localId, String notionId) {
        RecordEntity entity = recordDao.queryById(localId);
        if (entity == null) return;
        entity.setSyncId(notionId);
        entity.setSyncStatus(1); // 已同步
        recordDao.update(entity);
    }

    /**
     * 根据 Notion Page ID 查询本地对应记录
     *
     * @param notionId Notion Page ID（即 record_sync_id）
     * @return 对应的本地记录，不存在则返回 null
     */
    private TallyRecord findLocalByNotionId(String notionId) {
        if (notionId == null || notionId.isEmpty()) return null;
        List<RecordEntity> all = recordDao.queryAll();
        if (all == null) return null;
        for (RecordEntity e : all) {
            if (notionId.equals(e.getSyncId())) {
                return entityToTallyRecord(e);
            }
        }
        return null;
    }

    /**
     * 将 Notion 拉取的记录写入本地数据库
     *
     * @param record 从 Notion 解析出的 TallyRecord
     * @return 新增记录的本地 ID
     */
    private long addLocalRecord(TallyRecord record) {
        RecordEntity entity = tallyRecordToEntity(record);
        // 设置 syncStatus 为已同步，避免重复拉取
        entity.setSyncStatus(1);
        return recordDao.insert(entity);
    }

    /**
     * 用 Notion 数据覆盖更新本地记录
     *
     * @param record 从 Notion 解析出的 TallyRecord（需含 notionId）
     */
    private void updateLocalRecord(TallyRecord record) {
        TallyRecord local = findLocalByNotionId(record.getNotionId());
        if (local == null) return;

        RecordEntity entity = recordDao.queryById(local.getId());
        if (entity == null) return;

        entity.setAmount(record.getAmount());
        entity.setTime(record.getTime());
        entity.setCategoryUniqueName(record.getCategory());
        entity.setDesc(record.getRemark() != null ? record.getRemark() : "");
        entity.setType(record.getType());
        entity.setSyncStatus(1);
        recordDao.update(entity);
    }

    // ==================== 实体转换工具 ====================

    /**
     * 将本地 RecordEntity 转换为同步模块使用的 TallyRecord
     *
     * @param entity 数据库实体
     * @return TallyRecord（用于 API 调用）
     */
    private TallyRecord entityToTallyRecord(RecordEntity entity) {
        if (entity == null) return null;
        TallyRecord record = new TallyRecord();
        record.setId(entity.getId());
        record.setNotionId(entity.getSyncId());
        record.setTime(entity.getTime());
        record.setAmount(entity.getAmount());
        record.setCategory(entity.getCategoryUniqueName());
        record.setRemark(entity.getDesc());
        record.setType(entity.getType());
        record.setSynced(entity.getSyncStatus() == 1);
        // 使用 record_time 作为 lastModified 的近似值
        record.setLastModified(entity.getTime());
        return record;
    }

    /**
     * 将 TallyRecord（Notion 数据模型）转换为数据库 RecordEntity
     *
     * @param record TallyRecord
     * @return 准备写入数据库的 RecordEntity
     */
    private RecordEntity tallyRecordToEntity(TallyRecord record) {
        RecordEntity entity = new RecordEntity();
        // notionId 作为 syncId 存储
        entity.setSyncId(record.getNotionId() != null ? record.getNotionId() : "");
        entity.setTime(record.getTime());
        entity.setAmount(record.getAmount());
        entity.setCategoryUniqueName(record.getCategory() != null ? record.getCategory() : "other");
        entity.setDesc(record.getRemark() != null ? record.getRemark() : "");
        entity.setType(record.getType());
        entity.setSyncStatus(record.isSynced() ? 1 : 0);
        return entity;
    }

    // ==================== 回调辅助 ====================

    /** 在主线程回调 onSyncProgress */
    private void notifyProgress(int current, int total, String message) {
        if (listener != null) {
            mainHandler.post(() -> listener.onSyncProgress(current, total, message));
        }
    }

    /** 在主线程回调 onSyncComplete */
    private void notifyComplete(int synced, int failed) {
        if (listener != null) {
            mainHandler.post(() -> listener.onSyncComplete(synced, failed));
        }
    }

    /** 在主线程回调 onSyncError */
    private void notifyError(String error) {
        if (listener != null) {
            mainHandler.post(() -> listener.onSyncError(error));
        }
    }

    /**
     * 释放资源（Activity/Fragment 销毁时调用）
     */
    public void release() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}

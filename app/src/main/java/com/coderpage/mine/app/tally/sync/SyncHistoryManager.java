package com.coderpage.mine.app.tally.sync;

import android.content.Context;
import timber.log.Timber;

import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.dao.SyncHistoryDao;
import com.coderpage.mine.app.tally.persistence.sql.entity.SyncHistoryEntity;
import com.coderpage.mine.app.tally.sync.NotionSyncManager.SyncConflict;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步历史管理器
 *
 * 底层使用 Room SyncHistoryDao 存储同步记录，
 * 对外保持与 NotionSyncManager 一致的接口。
 *
 * @author abner-l
 * @since 0.7.5
 */
public class SyncHistoryManager {

    private static final String TAG = "SyncHistoryManager";
    private static final int MAX_HISTORY_SIZE = 50;

    private final SyncHistoryDao dao;

    public SyncHistoryManager(Context context) {
        this.dao = TallyDatabase.getInstance().syncHistoryDao();
    }

    /**
     * 记录一次同步操作
     *
     * @param timestamp     同步时间戳
     * @param direction     同步方向描述（TO_NOTION / FROM_NOTION / BIDIRECTIONAL）
     * @param localUpdated  本地更新数量
     * @param notionUpdated Notion 更新数量
     */
    public void logSync(long timestamp, String direction, int localUpdated, int notionUpdated) {
        try {
            SyncHistoryEntity entity = new SyncHistoryEntity();
            entity.setSyncTime(timestamp);
            entity.setSyncDirection(parseDirection(direction));
            entity.setUploaded(localUpdated);
            entity.setPulled(notionUpdated);
            entity.setFailed(0);
            entity.setStatus(1); // 成功
            dao.insert(entity);

            // 自动清理，只保留最近 MAX_HISTORY_SIZE 条
            dao.keepRecent(MAX_HISTORY_SIZE);

            Timber.i(TAG, "Sync logged: " + direction
                    + " (uploaded=" + localUpdated + ", pulled=" + notionUpdated + ")");
        } catch (Exception e) {
            Timber.e(TAG, "Failed to log sync", e);
        }
    }

    /**
     * 记录一次同步失败
     *
     * @param timestamp   同步时间戳
     * @param direction   同步方向描述
     * @param failedCount 失败数量
     * @param errorMsg    错误信息
     */
    public void logSyncFailure(long timestamp, String direction, int failedCount, String errorMsg) {
        try {
            SyncHistoryEntity entity = new SyncHistoryEntity();
            entity.setSyncTime(timestamp);
            entity.setSyncDirection(parseDirection(direction));
            entity.setUploaded(0);
            entity.setPulled(0);
            entity.setFailed(failedCount);
            entity.setErrorMessage(errorMsg);
            entity.setStatus(2); // 失败
            dao.insert(entity);
            dao.keepRecent(MAX_HISTORY_SIZE);
            Timber.e(TAG, "Sync failure logged: " + direction + " - " + errorMsg);
        } catch (Exception e) {
            Timber.e(TAG, "Failed to log sync failure", e);
        }
    }

    /**
     * 记录冲突信息（用 errorMessage 字段存储冲突摘要）
     */
    public void logConflict(SyncConflict conflict) {
        try {
            SyncHistoryEntity entity = new SyncHistoryEntity();
            entity.setSyncTime(System.currentTimeMillis());
            entity.setSyncDirection(2); // 双向同步场景
            entity.setUploaded(0);
            entity.setPulled(0);
            entity.setFailed(1);
            entity.setErrorMessage("Conflict: notionId=" + conflict.notionId
                    + " resolution=" + conflict.resolution.name()
                    + " fields=" + conflict.conflictingFields);
            entity.setStatus(1);
            dao.insert(entity);
            Timber.w(TAG, "Conflict logged: " + conflict.notionId);
        } catch (Exception e) {
            Timber.e(TAG, "Failed to log conflict", e);
        }
    }

    /**
     * 批量记录冲突
     */
    public void logConflicts(List<SyncConflict> conflicts) {
        for (SyncConflict conflict : conflicts) {
            logConflict(conflict);
        }
    }

    /**
     * 获取上次同步时间
     */
    public long getLastSyncTime() {
        SyncHistoryEntity last = dao.getLastSync();
        return last != null ? last.getSyncTime() : 0;
    }

    /**
     * 获取最近的同步历史
     */
    public List<SyncHistoryEntity> getSyncHistory() {
        return dao.getRecentHistory(MAX_HISTORY_SIZE);
    }

    /**
     * 获取最近的失败记录
     */
    public List<SyncHistoryEntity> getFailedHistory() {
        return dao.getFailedHistory(MAX_HISTORY_SIZE);
    }

    /**
     * 统计未解决的冲突数（resolution=MANUAL 的记录）
     */
    public int getUnresolvedConflictCount() {
        List<SyncHistoryEntity> all = dao.getAllHistory();
        int count = 0;
        for (SyncHistoryEntity entity : all) {
            if (entity.getErrorMessage() != null
                    && entity.getErrorMessage().startsWith("Conflict:")
                    && entity.getErrorMessage().contains("resolution=MANUAL")) {
                count++;
            }
        }
        return count;
    }

    /**
     * 清空所有历史记录
     */
    public void clearHistory() {
        dao.deleteAll();
    }

    /**
     * 解析方向描述为数字编码
     */
    private int parseDirection(String direction) {
        if (direction == null) return 2;
        switch (direction) {
            case "TO_NOTION": return 0;
            case "FROM_NOTION": return 1;
            case "BIDIRECTIONAL": return 2;
            default: return 2;
        }
    }
}

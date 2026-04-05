package com.coderpage.mine.app.tally.sync;

import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.dao.SyncHistoryDao;
import com.coderpage.mine.app.tally.persistence.sql.entity.SyncHistoryEntity;

import java.util.List;

/**
 * 同步历史管理器
 *
 * 管理同步历史记录，提供查询、重试等功能
 */
public class SyncHistoryManager {

    /** 默认保留的历史记录数量 */
    private static final int DEFAULT_KEEP_COUNT = 50;

    private final SyncHistoryDao syncHistoryDao;

    public SyncHistoryManager() {
        this.syncHistoryDao = TallyDatabase.getInstance().syncHistoryDao();
    }

    /**
     * 创建新的同步历史记录
     */
    public SyncHistoryEntity createHistory(int direction) {
        SyncHistoryEntity history = new SyncHistoryEntity();
        history.setSyncDirection(direction);
        history.setSyncTime(System.currentTimeMillis());
        long id = syncHistoryDao.insert(history);
        history.setId(id);
        return history;
    }

    /**
     * 更新同步历史记录
     */
    public void updateHistory(SyncHistoryEntity history) {
        syncHistoryDao.update(history);
    }

    /**
     * 标记同步成功
     */
    public void markSuccess(SyncHistoryEntity history, int uploaded, int pulled) {
        history.setUploaded(uploaded);
        history.setPulled(pulled);
        history.setFailed(0);
        history.setStatus(1);
        syncHistoryDao.update(history);
    }

    /**
     * 标记同步失败
     */
    public void markFailed(SyncHistoryEntity history, String error) {
        history.setErrorMessage(error);
        history.setStatus(2);
        syncHistoryDao.update(history);
    }

    /**
     * 获取最近的同步历史
     */
    public List<SyncHistoryEntity> getRecentHistory(int limit) {
        return syncHistoryDao.getRecentHistory(limit);
    }

    /**
     * 获取最近的失败记录
     */
    public List<SyncHistoryEntity> getFailedHistory(int limit) {
        return syncHistoryDao.getFailedHistory(limit);
    }

    /**
     * 获取最近一次同步记录
     */
    public SyncHistoryEntity getLastSync() {
        List<SyncHistoryEntity> list = syncHistoryDao.getRecentHistory(1);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 删除指定历史记录
     */
    public void deleteHistory(long id) {
        syncHistoryDao.deleteById(id);
    }

    /**
     * 清空所有历史
     */
    public void clearAll() {
        syncHistoryDao.deleteAll();
    }

    /**
     * 清理旧的历史记录，只保留最近N条
     */
    public void cleanupOldHistory() {
        syncHistoryDao.keepRecent(DEFAULT_KEEP_COUNT);
    }

    /**
     * 格式化历史记录为友好文本
     */
    public static String formatHistory(SyncHistoryEntity history) {
        StringBuilder sb = new StringBuilder();
        sb.append(history.getDirectionText()).append(" - ");
        sb.append(history.getStatusText()).append("\n");

        if (history.getStatus() == 1) {
            if (history.getUploaded() > 0) {
                sb.append("上传 ").append(history.getUploaded()).append(" 条");
            }
            if (history.getPulled() > 0) {
                if (history.getUploaded() > 0) sb.append(", ");
                sb.append("拉取 ").append(history.getPulled()).append(" 条");
            }
            if (history.getUploaded() == 0 && history.getPulled() == 0) {
                sb.append("无数据变动");
            }
        } else if (history.getStatus() == 2) {
            sb.append(history.getErrorMessage() != null ? history.getErrorMessage() : "未知错误");
        }

        return sb.toString();
    }
}

package com.coderpage.mine.app.tally.persistence.sql.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.coderpage.mine.app.tally.persistence.sql.entity.SyncHistoryEntity;

import java.util.List;

/**
 * 同步历史数据访问对象
 */
@Dao
public interface SyncHistoryDao {

    /**
     * 插入同步历史记录
     */
    @Insert
    long insert(SyncHistoryEntity history);

    /**
     * 更新同步历史记录
     */
    @Update
    void update(SyncHistoryEntity history);

    /**
     * 获取最近的N条同步历史
     */
    @Query("SELECT * FROM sync_history ORDER BY syncTime DESC LIMIT :limit")
    List<SyncHistoryEntity> getRecentHistory(int limit);

    /**
     * 获取所有同步历史
     */
    @Query("SELECT * FROM sync_history ORDER BY syncTime DESC")
    List<SyncHistoryEntity> getAllHistory();

    /**
     * 获取最近的失败同步记录
     */
    @Query("SELECT * FROM sync_history WHERE status = 2 ORDER BY syncTime DESC LIMIT :limit")
    List<SyncHistoryEntity> getFailedHistory(int limit);

    /**
     * 获取最近一次同步记录
     */
    @Query("SELECT * FROM sync_history ORDER BY syncTime DESC LIMIT 1")
    SyncHistoryEntity getLastSync();

    /**
     * 删除指定ID的历史记录
     */
    @Query("DELETE FROM sync_history WHERE id = :id")
    void deleteById(long id);

    /**
     * 清空所有历史记录
     */
    @Query("DELETE FROM sync_history")
    void deleteAll();

    /**
     * 只保留最近N条记录
     */
    @Query("DELETE FROM sync_history WHERE id NOT IN (SELECT id FROM sync_history ORDER BY syncTime DESC LIMIT :keepCount)")
    void keepRecent(int keepCount);
}

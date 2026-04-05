package com.coderpage.mine.app.tally.sync;

import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.dao.RecordDao;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;
import com.coderpage.mine.persistence.entity.TallyRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步专用 Repository
 * 
 * 封装所有与 Notion 同步相关的数据库操作，
 * 遵循分层架构原则，由 SyncManager 调用，而非直接操作 DAO。
 * 
 * @author abner-l
 * @since 0.7.5
 */
public class SyncRepository {

    private final RecordDao recordDao;

    public SyncRepository() {
        this.recordDao = TallyDatabase.getInstance().recordDao();
    }

    /**
     * 查询所有未同步到 Notion 的记录
     */
    public List<TallyRecord> queryUnsyncedRecords() {
        List<RecordEntity> entities = recordDao.queryAll();
        List<TallyRecord> result = new ArrayList<>();
        if (entities == null) return result;
        for (RecordEntity e : entities) {
            if (e.getSyncStatus() == 0) {
                result.add(TallyRecord.fromEntity(e));
            }
        }
        return result;
    }

    /**
     * 根据 Notion Page ID 查询本地对应记录
     */
    public TallyRecord findByNotionId(String notionId) {
        if (notionId == null || notionId.isEmpty()) return null;
        List<RecordEntity> all = recordDao.queryAll();
        if (all == null) return null;
        for (RecordEntity e : all) {
            if (notionId.equals(e.getSyncId())) {
                return TallyRecord.fromEntity(e);
            }
        }
        return null;
    }

    /**
     * 将已成功上传的本地记录标记为已同步
     */
    public void markAsSynced(long localId, String notionId) {
        RecordEntity entity = recordDao.queryById(localId);
        if (entity == null) return;
        entity.setSyncId(notionId);
        entity.setSyncStatus(1);
        recordDao.update(entity);
    }

    /**
     * 将 Notion 记录写入本地数据库
     */
    public long insertFromNotion(TallyRecord record) {
        RecordEntity entity = record.toEntity();
        entity.setSyncStatus(1);
        return recordDao.insert(entity);
    }

    /**
     * 用 Notion 数据覆盖更新本地记录
     */
    public void updateFromNotion(TallyRecord record) {
        TallyRecord local = findByNotionId(record.getNotionId());
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

    /**
     * 查询所有记录
     */
    public List<TallyRecord> queryAll() {
        List<RecordEntity> entities = recordDao.queryAll();
        List<TallyRecord> result = new ArrayList<>();
        if (entities == null) return result;
        for (RecordEntity e : entities) {
            result.add(TallyRecord.fromEntity(e));
        }
        return result;
    }
}

package com.coderpage.mine.app.tally.sync;

import com.coderpage.mine.app.tally.persistence.sql.dao.RecordDao;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步仓储，负责本地记录读写
 */
public class SyncRepository {

    private final RecordDao recordDao;

    public SyncRepository(RecordDao recordDao) {
        this.recordDao = recordDao;
    }

    public List<ConflictResolver.Record> getLocalRecords() {
        try {
            List<com.coderpage.mine.app.tally.persistence.model.Record> modelRecords = recordDao.queryAll();
            return RecordConverter.toSyncRecords(modelRecords);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void updateLocalRecord(ConflictResolver.Record record) {
        try {
            if (record.id == null || record.id.isEmpty()) {
                return;
            }
            long id = Long.parseLong(record.id);
            com.coderpage.mine.app.tally.persistence.model.Record model = recordDao.queryById(id);
            if (model != null) {
                model.setAmount(record.amount);
                model.setTime(record.time);
                model.setDesc(record.remark != null ? record.remark : "");
                model.setType("expense".equals(record.type) ? RecordEntity.TYPE_EXPENSE : RecordEntity.TYPE_INCOME);
                model.setCategoryUniqueName(record.category != null ? record.category : "");
                if (record.notionPageId != null) {
                    model.setSyncId("notion:" + record.notionPageId);
                }
                recordDao.update(model.createEntity());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveRemoteRecord(ConflictResolver.Record record) {
        try {
            List<com.coderpage.mine.app.tally.persistence.model.Record> existing = recordDao.queryAll();
            for (com.coderpage.mine.app.tally.persistence.model.Record existingRecord : existing) {
                if (record.notionPageId != null &&
                        record.notionPageId.equals(existingRecord.getSyncId().replace("notion:", ""))) {
                    existingRecord.setAmount(record.amount);
                    existingRecord.setTime(record.time);
                    existingRecord.setDesc(record.remark != null ? record.remark : "");
                    existingRecord.setType("expense".equals(record.type) ? RecordEntity.TYPE_EXPENSE : RecordEntity.TYPE_INCOME);
                    existingRecord.setCategoryUniqueName(record.category != null ? record.category : "");
                    existingRecord.setSyncId("notion:" + record.notionPageId);
                    recordDao.update(existingRecord.createEntity());
                    return;
                }
            }
            com.coderpage.mine.app.tally.persistence.model.Record newRecord = new com.coderpage.mine.app.tally.persistence.model.Record();
            newRecord.setAmount(record.amount);
            newRecord.setTime(record.time);
            newRecord.setDesc(record.remark != null ? record.remark : "");
            newRecord.setType("expense".equals(record.type) ? RecordEntity.TYPE_EXPENSE : RecordEntity.TYPE_INCOME);
            newRecord.setCategoryUniqueName(record.category != null ? record.category : "");
            newRecord.setSyncId(record.notionPageId != null ? "notion:" + record.notionPageId : "");
            recordDao.insert(newRecord.createEntity());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

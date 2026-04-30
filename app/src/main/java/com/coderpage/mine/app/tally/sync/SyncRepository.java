package com.coderpage.mine.app.tally.sync;

import com.coderpage.mine.app.tally.persistence.sql.dao.RecordDao;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步仓储，负责本地记录读写
 */
public class SyncRepository {

    private static final String TAG = "SyncRepository";
    private static final String NOTION_SYNC_PREFIX = "notion:";

    private final RecordDao recordDao;

    public SyncRepository(RecordDao recordDao) {
        this.recordDao = recordDao;
    }

    public List<ConflictResolver.Record> getLocalRecords() {
        try {
            List<com.coderpage.mine.app.tally.persistence.model.Record> modelRecords = recordDao.queryAll();
            return RecordConverter.toSyncRecords(modelRecords);
        } catch (Exception e) {
            Log.e(TAG, "getLocalRecords failed", e);
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
                applyRecordFields(model, record);
                recordDao.update(model.createEntity());
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "updateLocalRecord: invalid id format: " + record.id, e);
        } catch (Exception e) {
            Log.e(TAG, "updateLocalRecord failed for id=" + record.id, e);
            throw new RuntimeException("Failed to update local record: " + record.id, e);
        }
    }

    public void saveRemoteRecord(ConflictResolver.Record record) {
        try {
            if (record.notionPageId != null && !record.notionPageId.isEmpty()) {
                com.coderpage.mine.app.tally.persistence.model.Record existingRecord =
                        recordDao.queryBySyncId(makeNotionSyncId(record.notionPageId));
                if (existingRecord != null) {
                    applyRecordFields(existingRecord, record);
                    recordDao.update(existingRecord.createEntity());
                    return;
                }
            }
            com.coderpage.mine.app.tally.persistence.model.Record newRecord = new com.coderpage.mine.app.tally.persistence.model.Record();
            applyRecordFields(newRecord, record);
            recordDao.insert(newRecord.createEntity());
        } catch (Exception e) {
            Log.e(TAG, "saveRemoteRecord failed for notionPageId=" + record.notionPageId, e);
            throw new RuntimeException("Failed to save remote record: " + record.notionPageId, e);
        }
    }

    private void applyRecordFields(com.coderpage.mine.app.tally.persistence.model.Record target,
                                   ConflictResolver.Record source) {
        target.setAmount(source.amount);
        target.setTime(source.time);
        target.setDesc(source.remark != null ? source.remark : "");
        boolean isExpense = "expense".equals(source.type) || "支出".equals(source.type);
        target.setType(isExpense ? RecordEntity.TYPE_EXPENSE : RecordEntity.TYPE_INCOME);
        target.setCategoryUniqueName(source.category != null ? source.category : "");
        if (source.notionPageId != null && !source.notionPageId.isEmpty()
                && (target.getSyncId() == null || target.getSyncId().isEmpty())) {
            target.setSyncId(makeNotionSyncId(source.notionPageId));
        }
    }

    private String makeNotionSyncId(String notionPageId) {
        if (notionPageId == null || notionPageId.isEmpty()) {
            return "";
        }
        return NOTION_SYNC_PREFIX + notionPageId;
    }
}

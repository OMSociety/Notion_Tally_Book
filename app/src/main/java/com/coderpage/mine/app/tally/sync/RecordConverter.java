package com.coderpage.mine.app.tally.sync;

import com.coderpage.mine.app.tally.persistence.model.Record;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Record 与 RecordEntity 转换器
 * 
 * @author Flandre Scarlet
 */
public class RecordConverter {

    /**
     * Record 转换为 ConflictResolver.Record
     */
    public static ConflictResolver.Record toSyncRecord(Record model) {
        if (model == null) {
            return null;
        }

        ConflictResolver.Record record = new ConflictResolver.Record();
        record.id = String.valueOf(model.getId());
        record.amount = model.getAmount() != null ? model.getAmount().doubleValue() : 0.0;
        record.time = model.getTime();
        record.remark = model.getDesc();
        record.lastModified = model.getTime();
        // 类型转换: 0=支出, 1=收入
        if (model.getType() == RecordEntity.TYPE_EXPENSE) {
            record.type = "expense";
        } else {
            record.type = "income";
        }

        // 分类
        record.category = model.getCategoryUniqueName();

        // notionPageId 存储在 syncId 中（如果有的话，格式为 notion:pageId）
        if (model.getSyncId() != null && model.getSyncId().startsWith("notion:")) {
            record.notionPageId = model.getSyncId().substring(7);
        }

        return record;
    }

    /**
     * ConflictResolver.Record 转换为 Record（新建）
     */
    public static Record toRecord(ConflictResolver.Record record) {
        return toRecord(record, new Record());
    }

    /**
     * ConflictResolver.Record 转换为 Record，保留已有 Record 的 syncId
     */
    public static Record toRecord(ConflictResolver.Record record, Record existing) {
        if (record == null) {
            return null;
        }

        Record model = existing;

        // ID
        if (record.id != null && !record.id.isEmpty()) {
            try {
                model.setId(Long.parseLong(record.id));
            } catch (NumberFormatException e) {
                // 新记录
            }
        }

        model.setAmount(java.math.BigDecimal.valueOf(record.amount));
        model.setTime(record.time);
        model.setDesc(record.remark != null ? record.remark : "");

        // 类型转换
        if ("expense".equals(record.type)) {
            model.setType(RecordEntity.TYPE_EXPENSE);
        } else {
            model.setType(RecordEntity.TYPE_INCOME);
        }

        // 分类
        model.setCategoryUniqueName(record.category != null ? record.category : "");

        // notionPageId 存储在 syncId 中（仅在无现有 syncId 且有 notionPageId 时写入，避免覆盖本地 syncId）
        if (record.notionPageId != null && !record.notionPageId.isEmpty()
                && (model.getSyncId() == null || model.getSyncId().isEmpty())) {
            model.setSyncId("notion:" + record.notionPageId);
        }

        return model;
    }

    /**
     * List<Record> 转换为 List<ConflictResolver.Record>
     */
    public static List<ConflictResolver.Record> toSyncRecords(List<Record> models) {
        List<ConflictResolver.Record> records = new ArrayList<>();
        if (models == null) {
            return records;
        }

        for (Record model : models) {
            ConflictResolver.Record record = toSyncRecord(model);
            if (record != null) {
                records.add(record);
            }
        }

        return records;
    }
}

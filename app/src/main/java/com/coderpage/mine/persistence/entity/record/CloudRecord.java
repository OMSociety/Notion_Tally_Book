package com.coderpage.mine.persistence.entity.record;

import com.coderpage.mine.persistence.entity.TallyRecord;

/**
 * Notion 云端记录模型
 * 
 * 代表 Notion Database 中的记录状态。
 * 用于同步冲突检测时的云端数据封装。
 * 
 * @author Flandre Scarlet
 * @since 1.0.0
 */
public class CloudRecord {
    
    private final String notionId;
    private final double amount;
    private final String category;
    private final String remark;
    private final long time;
    private final long lastEditedTime;
    
    public CloudRecord(String notionId, double amount, String category,
                       String remark, long time, long lastEditedTime) {
        this.notionId = notionId != null ? notionId : "";
        this.amount = amount;
        this.category = category != null ? category : "other";
        this.remark = remark != null ? remark : "";
        this.time = time;
        this.lastEditedTime = lastEditedTime;
    }
    
    public static CloudRecord fromTallyRecord(TallyRecord record) {
        return new CloudRecord(
            record.getNotionId(),
            record.getAmount(),
            record.getCategory(),
            record.getRemark(),
            record.getTime(),
            record.getLastModified()
        );
    }
    
    public String getNotionId() { return notionId; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getRemark() { return remark; }
    public long getTime() { return time; }
    public long getLastEditedTime() { return lastEditedTime; }
}

package com.coderpage.mine.persistence.entity.record;

import com.coderpage.mine.persistence.entity.TallyRecord;

/**
 * 本地账本记录模型
 * 
 * 代表本地 SQLite 数据库中的记录状态。
 * 用于同步冲突检测时的本地端数据封装。
 * 
 * @author Flandre Scarlet
 * @since 1.0.0
 */
public class LocalRecord {
    
    private final long id;
    private final double amount;
    private final String category;
    private final String remark;
    private final long time;
    private final int syncStatus;
    private final long lastModified;
    
    public LocalRecord(long id, double amount, String category, 
                       String remark, long time, int syncStatus, long lastModified) {
        this.id = id;
        this.amount = amount;
        this.category = category != null ? category : "other";
        this.remark = remark != null ? remark : "";
        this.time = time;
        this.syncStatus = syncStatus;
        this.lastModified = lastModified;
    }
    
    public static LocalRecord fromTallyRecord(TallyRecord record) {
        return new LocalRecord(
            record.getId(),
            record.getAmount(),
            record.getCategory(),
            record.getRemark(),
            record.getTime(),
            record.getSyncStatus(),
            record.getLastModified()
        );
    }
    
    public long getId() { return id; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getRemark() { return remark; }
    public long getTime() { return time; }
    public int getSyncStatus() { return syncStatus; }
    public long getLastModified() { return lastModified; }
    public boolean isSynced() { return syncStatus == 1; }
}

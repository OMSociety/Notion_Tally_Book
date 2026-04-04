package com.coderpage.mine.persistence.entity;
import java.io.Serializable;

/** 记账记录实体 */
public class TallyRecord implements Serializable {
    private long id;
    private String notionId;
    private long time;
    private double amount;
    private String category;
    private String remark;
    private int type; // 0-支出, 1-收入
    private boolean synced;
    private long lastModified;

    public TallyRecord() {}
    public TallyRecord(long id, long time, double amount, String category, String remark, int type) {
        this.id = id; this.time = time; this.amount = amount;
        this.category = category; this.remark = remark; this.type = type;
        this.synced = false; this.lastModified = System.currentTimeMillis();
    }

    public long getId() { return id; } public void setId(long id) { this.id = id; }
    public String getNotionId() { return notionId; } public void setNotionId(String notionId) { this.notionId = notionId; }
    public long getTime() { return time; } public void setTime(long time) { this.time = time; }
    public double getAmount() { return amount; } public void setAmount(double amount) { this.amount = amount; }
    public String getCategory() { return category; } public void setCategory(String category) { this.category = category; }
    public String getRemark() { return remark; } public void setRemark(String remark) { this.remark = remark; }
    public int getType() { return type; } public void setType(int type) { this.type = type; }
    public boolean isSynced() { return synced; } public void setSynced(boolean synced) { this.synced = synced; }
    public long getLastModified() { return lastModified; } public void setLastModified(long lastModified) { this.lastModified = lastModified; }
}

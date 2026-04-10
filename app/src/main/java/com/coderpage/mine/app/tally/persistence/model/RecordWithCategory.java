package com.coderpage.mine.app.tally.persistence.model;

import androidx.room.ColumnInfo;

/**
 * RecordWithCategory - JOIN 查询结果 POJO
 * 包含 record 表和 category 表的字段
 */
public class RecordWithCategory {

    // ===== record 表字段 =====
    @ColumnInfo(name = "record_id")
    private long id;

    @ColumnInfo(name = "record_account_id")
    private long accountId;

    @ColumnInfo(name = "record_time")
    private long time;

    @ColumnInfo(name = "record_amount")
    private long amount;

    @ColumnInfo(name = "record_category_unique_name")
    private String categoryUniqueName;

    @ColumnInfo(name = "record_desc")
    private String desc;

    @ColumnInfo(name = "record_sync_id")
    private String syncId;

    @ColumnInfo(name = "record_sync_status")
    private int syncStatus;

    @ColumnInfo(name = "record_delete")
    private int delete;

    @ColumnInfo(name = "record_type")
    private int type;

    // ===== category 表字段 (JOIN 获取) =====
    @ColumnInfo(name = "category_name")
    private String categoryName;

    @ColumnInfo(name = "category_icon")
    private String categoryIcon;

    @ColumnInfo(name = "category_order")
    private int categoryOrder;

    // ===== Getters =====
    public long getId() { return id; }
    public long getAccountId() { return accountId; }
    public long getTime() { return time; }
    public long getAmount() { return amount; }
    public String getCategoryUniqueName() { return categoryUniqueName; }
    public String getDesc() { return desc; }
    public String getSyncId() { return syncId; }
    public int getSyncStatus() { return syncStatus; }
    public int getDelete() { return delete; }
    public int getType() { return type; }
    public String getCategoryName() { return categoryName; }
    public String getCategoryIcon() { return categoryIcon; }
    public int getCategoryOrder() { return categoryOrder; }

    // ===== Setters =====
    public void setId(long id) { this.id = id; }
    public void setAccountId(long accountId) { this.accountId = accountId; }
    public void setTime(long time) { this.time = time; }
    public void setAmount(long amount) { this.amount = amount; }
    public void setCategoryUniqueName(String categoryUniqueName) { this.categoryUniqueName = categoryUniqueName; }
    public void setDesc(String desc) { this.desc = desc; }
    public void setSyncId(String syncId) { this.syncId = syncId; }
    public void setSyncStatus(int syncStatus) { this.syncStatus = syncStatus; }
    public void setDelete(int delete) { this.delete = delete; }
    public void setType(int type) { this.type = type; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }
    public void setCategoryOrder(int categoryOrder) { this.categoryOrder = categoryOrder; }
}

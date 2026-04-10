package com.coderpage.mine.app.tally.persistence.sql.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import com.coderpage.mine.app.tally.persistence.sql.DatabaseConstants;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * @author lc.
 * @since 0.6.0
 */
@Entity(tableName = DatabaseConstants.TABLE_RECORD, indices = {@Index(value = {DatabaseConstants.COLUMN_RECORD_SYNC_ID}, unique = true)})
public class RecordEntity {

    /** 记录类型: 支出 */
    public static final int TYPE_EXPENSE = 0;
    /** 记录类型: 收入 */
    public static final int TYPE_INCOME = 1;

    /** 数据表自增 ID */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "record_id")
    private long id;

    /** 账户 ID */
    @ColumnInfo(name = "record_account_id")
    private long accountId;

    /** 记录时间（UNIX TIME） */
    @ColumnInfo(name = "record_time")
    private long time;

    /** 金额 */
    @ColumnInfo(name = "record_amount")
    private double amount;

    /** 分类唯一名称，不可变 */
    @ColumnInfo(name = "record_category_unique_name")
    private String categoryUniqueName;

    /** 备注 */
    @NonNull
    @ColumnInfo(name = "record_desc")
    private String desc = "";

    /** 同步 ID */
    @NonNull
    @ColumnInfo(name = "record_sync_id")
    private String syncId = "";

    /** 同步状态 */
    @ColumnInfo(name = "record_sync_status")
    private int syncStatus;

    /** 是否删除 */
    @ColumnInfo(name = "record_delete")
    private int delete;

    /** 记录类型 */
    @ColumnInfo(name = "record_type")
    private int type;

    // ===== 以下字段用于 JOIN category 表后的数据映射（Room 自动填充） =====

    /** 分类名称（JOIN category 表获得） */
    @ColumnInfo(name = "category_name")
    private String categoryName = "";

    /** 分类图标（JOIN category 表获得） */
    @ColumnInfo(name = "category_icon")
    private String categoryIcon = "";

    /** 分类排序（JOIN category 表获得） */
    @ColumnInfo(name = "category_order")
    private int categoryOrder = 0;
    /** 分类ID（JOIN category 表获得） */
    @ColumnInfo(name = "category_id")
    private long categoryId = 0;

    /** 分类类型（JOIN category 表获得） */
    @ColumnInfo(name = "category_type")
    private int categoryType = 0;

    /** 分类账户ID（JOIN category 表获得） */
    @ColumnInfo(name = "category_account_id")
    private long categoryAccountId = 0;

    /** 分类同步状态（JOIN category 表获得） */
    @ColumnInfo(name = "category_sync_status")
    private int categorySyncStatus = 0;

    /** 分类唯一名称（JOIN category 表获得，用于区分 record 的 category_unique_name） */
    @ColumnInfo(name = "category_unique_name")
    @Entity.java.foreignKey
    private String joinedCategoryUniqueName = "";



    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategoryUniqueName() {
        return categoryUniqueName;
    }

    public void setCategoryUniqueName(String categoryUniqueName) {
        this.categoryUniqueName = categoryUniqueName;
    }

    @NonNull
    public String getDesc() {
        return desc;
    }

    public void setDesc(@NonNull String desc) {
        this.desc = desc;
    }

    @NonNull
    public String getSyncId() {
        return syncId;
    }

    public void setSyncId(@NonNull String syncId) {
        this.syncId = syncId;
    }

    public int getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(int syncStatus) {
        this.syncStatus = syncStatus;
    }

    public int getDelete() {
        return delete;
    }

    public void setDelete(int delete) {
        this.delete = delete;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName != null ? categoryName : "";
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon != null ? categoryIcon : "";
    }

    public int getCategoryOrder() {
        return categoryOrder;
    }

    public void setCategoryOrder(int categoryOrder) {
        this.categoryOrder = categoryOrder;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public int getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(int categoryType) {
        this.categoryType = categoryType;
    }

    public long getCategoryAccountId() {
        return categoryAccountId;
    }

    public void setCategoryAccountId(long categoryAccountId) {
        this.categoryAccountId = categoryAccountId;
    }

    public int getCategorySyncStatus() {
        return categorySyncStatus;
    }

    public void setCategorySyncStatus(int categorySyncStatus) {
        this.categorySyncStatus = categorySyncStatus;
    }

    public String getJoinedCategoryUniqueName() {
        return joinedCategoryUniqueName;
    }

    public void setJoinedCategoryUniqueName(String joinedCategoryUniqueName) {
        this.joinedCategoryUniqueName = joinedCategoryUniqueName != null ? joinedCategoryUniqueName : "";
    }


}

package com.coderpage.mine.persistence.entity;

import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;

/**
 * Notion 同步专用数据传输对象（DTO）
 * 
 * 继承自 RecordEntity，复用所有数据库字段。
 * 提供 Notion API 专用的别名方法，减少 entityToTallyRecord / tallyRecordToEntity 转换。
 * 
 * 字段映射关系：
 * - notionId     → syncId（Notion Page ID）
 * - category     → categoryUniqueName（分类唯一名称）
 * - remark       → desc（备注）
 * - synced       → syncStatus == 1
 * - lastModified → 由 Notion last_edited_time 设置
 * 
 * @author abner-l
 * @since 0.7.5
 */
public class TallyRecord extends RecordEntity {

    /** Notion Page ID */
    private String notionId;

    /** 分类名称（展示用，与 categoryUniqueName 配合） */
    private String category;

    /** 备注 */
    private String remark;

    /** 是否已同步到 Notion */
    private boolean synced;

    /** 最后修改时间（毫秒时间戳） */
    private long lastModified;

    /**
     * 默认构造器
     */
    public TallyRecord() {
        super();
    }

    /**
     * 从 RecordEntity 构造（用于 Notion→本地同步时转换）
     * 
     * @param entity 数据库实体
     */
    public TallyRecord(RecordEntity entity) {
        if (entity == null) return;
        setId(entity.getId());
        setAccountId(entity.getAccountId());
        setTime(entity.getTime());
        setAmount(entity.getAmount());
        setCategoryUniqueName(entity.getCategoryUniqueName());
        setDesc(entity.getDesc());
        setSyncId(entity.getSyncId());
        setSyncStatus(entity.getSyncStatus());
        setDelete(entity.getDelete());
        setType(entity.getType());
        
        // Notion 专用字段
        this.notionId = entity.getSyncId();
        this.category = entity.getCategoryUniqueName();
        this.remark = entity.getDesc();
        this.synced = entity.getSyncStatus() == 1;
        this.lastModified = entity.getTime();
    }

    // ==================== Notion 专用 getter/setter（别名方法） ====================

    public String getNotionId() {
        return notionId;
    }

    public void setNotionId(String notionId) {
        this.notionId = notionId;
        // 同时更新父类的 syncId
        setSyncId(notionId != null ? notionId : "");
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
        // 同时更新父类的 categoryUniqueName
        setCategoryUniqueName(category != null ? category : "other");
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
        // 同时更新父类的 desc
        setDesc(remark != null ? remark : "");
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
        // 同时更新父类的 syncStatus
        setSyncStatus(synced ? 1 : 0);
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    // ==================== 便捷转换方法 ====================

    /**
     * 将当前对象转换为数据库实体
     * 用于本地→Notion 同步时转换
     * 
     * @return RecordEntity 实例
     */
    public RecordEntity toEntity() {
        RecordEntity entity = new RecordEntity();
        entity.setId(getId());
        entity.setAccountId(getAccountId());
        entity.setTime(getTime());
        entity.setAmount(getAmount());
        entity.setCategoryUniqueName(getCategory() != null ? getCategory() : "other");
        entity.setDesc(getRemark() != null ? getRemark() : "");
        entity.setSyncId(getNotionId() != null ? getNotionId() : "");
        entity.setSyncStatus(isSynced() ? 1 : 0);
        entity.setDelete(0);
        entity.setType(getType());
        return entity;
    }

    /**
     * 静态工厂方法：从 RecordEntity 创建 TallyRecord
     * 
     * @param entity 数据库实体
     * @return TallyRecord 实例
     */
    public static TallyRecord fromEntity(RecordEntity entity) {
        return new TallyRecord(entity);
    }
}

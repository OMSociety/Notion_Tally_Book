package com.coderpage.mine.persistence.entity;

import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;

/**
 * Notion 同步专用数据传输对象（DTO）
 * 
 * 使用组合模式替代继承，消除数据冗余。
 * 持有 RecordEntity 的引用，通过代理方法提供 Notion 友好的命名。
 * 
 * 字段映射关系：
 * - getNotionId()     → entity.getSyncId()（Notion Page ID）
 * - getCategory()     → entity.getCategoryUniqueName()（分类唯一名称）
 * - getRemark()       → entity.getDesc()（备注）
 * - isSynced()        → entity.getSyncStatus() == 1
 * - getLastModified() → Notion last_edited_time 或本地 time
 * 
 * @author abner-l
 * @since 0.7.5
 */
public class TallyRecord {

    /** 持有数据库实体的引用（组合） */
    private final RecordEntity entity;
    
    /** Notion Page ID */
    private String notionId;
    
    /** 最后修改时间（毫秒时间戳，来自 Notion 或本地） */
    private long lastModified;

    /**
     * 默认构造器（用于反序列化场景）
     */
    public TallyRecord() {
        this.entity = new RecordEntity();
        this.notionId = "";
        this.lastModified = System.currentTimeMillis();
    }

    /**
     * 从 RecordEntity 构造（用于本地→Notion 同步）
     * 
     * @param entity 数据库实体
     */
    public TallyRecord(RecordEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity cannot be null");
        }
        this.entity = entity;
        this.notionId = entity.getSyncId();
        this.lastModified = entity.getTime();
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 从 RecordEntity 创建 TallyRecord
     * 
     * @param entity 数据库实体
     * @return TallyRecord 实例
     */
    public static TallyRecord fromEntity(RecordEntity entity) {
        if (entity == null) return null;
        return new TallyRecord(entity);
    }

    /**
     * 创建新的空记录
     */
    public static TallyRecord create() {
        return new TallyRecord();
    }

    // ==================== 转换为数据库实体 ====================

    /**
     * 将当前对象转换为数据库实体
     * 用于本地→Notion 同步时保存到本地数据库
     * 
     * @return RecordEntity 实例
     */
    public RecordEntity toEntity() {
        entity.setSyncId(notionId != null ? notionId : "");
        entity.setTime(lastModified);
        return entity;
    }

    // ==================== Notion 专用 getter/setter ====================

    /**
     * 获取 Notion Page ID
     */
    public String getNotionId() {
        return notionId;
    }

    /**
     * 设置 Notion Page ID
     */
    public void setNotionId(String notionId) {
        this.notionId = notionId;
    }

    /**
     * 获取最后修改时间（毫秒时间戳）
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * 设置最后修改时间
     */
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    // ==================== 代理方法（Notion 友好命名）====================

    /**
     * 获取分类名称（代理 entity.getCategoryUniqueName）
     */
    public String getCategory() {
        return entity.getCategoryUniqueName();
    }

    /**
     * 设置分类名称
     */
    public void setCategory(String category) {
        entity.setCategoryUniqueName(category != null ? category : "other");
    }

    /**
     * 获取备注（代理 entity.getDesc）
     */
    public String getRemark() {
        return entity.getDesc();
    }

    /**
     * 设置备注
     */
    public void setRemark(String remark) {
        entity.setDesc(remark != null ? remark : "");
    }

    /**
     * 是否已同步到 Notion
     */
    public boolean isSynced() {
        return entity.getSyncStatus() == 1;
    }

    /**
     * 设置同步状态
     */
    public void setSynced(boolean synced) {
        entity.setSyncStatus(synced ? 1 : 0);
    }

    // ==================== 直接访问底层实体（谨慎使用）====================

    /**
     * 获取底层的 RecordEntity
     * 供内部组件使用，不建议外部代码直接调用
     */
    public RecordEntity getEntity() {
        return entity;
    }

    // ==================== RecordEntity 字段的便捷访问方法 ====================

    public long getId() {
        return entity.getId();
    }

    public void setId(long id) {
        entity.setId(id);
    }

    public long getAccountId() {
        return entity.getAccountId();
    }

    public void setAccountId(long accountId) {
        entity.setAccountId(accountId);
    }

    public long getTime() {
        return entity.getTime();
    }

    public void setTime(long time) {
        entity.setTime(time);
    }

    public double getAmount() {
        return entity.getAmount();
    }

    public void setAmount(double amount) {
        entity.setAmount(amount);
    }

    public String getCategoryUniqueName() {
        return entity.getCategoryUniqueName();
    }

    public String getDesc() {
        return entity.getDesc();
    }

    public String getSyncId() {
        return notionId;
    }

    public int getSyncStatus() {
        return entity.getSyncStatus();
    }

    /**
     * 获取删除状态（软删除标志，用于 Notion 同步）
     * 0 = 未删除（活跃），1 = 已删除
     */
    public int getDeleteStatus() {
        return entity.getDelete();
    }

    /**
     * 设置删除状态（软删除标志，用于 Notion 同步）
     * @param status 0 = 未删除（活跃），1 = 已删除
     */
    public void setDeleteStatus(int status) {
        entity.setDelete(status);
    }

    public int getType() {
        return entity.getType();
    }

    public void setType(int type) {
        entity.setType(type);
    }

    /**
     * 获取同步ID（兼容旧代码）
     * @deprecated use {@link #getNotionId()} instead
     */
    @Deprecated
    public String getSyncIdLegacy() {
        return entity.getSyncId();
    }
}

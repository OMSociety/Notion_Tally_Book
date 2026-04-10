package com.coderpage.mine.persistence.entity.record;

/**
 * 同步冲突解决策略
 * 
 * 当本地记录与云端记录发生冲突时，采用何种策略解决。
 * 
 * @author Flandre Scarlet
 * @since 1.0.0
 */
public enum SyncConflictStrategy {
    
    /** 拒绝同步，保留本地版本 */
    REJECT_LOCAL,
    
    /** 拒绝同步，保留云端版本 */
    REJECT_CLOUD,
    
    /** 智能合并（字段级） */
    MERGE,
    
    /** 创建备份副本 */
    BACKUP
}

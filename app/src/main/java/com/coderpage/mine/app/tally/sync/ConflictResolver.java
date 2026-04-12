package com.coderpage.mine.app.tally.sync;

import com.coderpage.mine.app.tally.config.NotionConfig;

/**
 * 冲突处理策略
 * 
 * @author Flandre Scarlet
 */
public class ConflictResolver {
    
    public static final int STRATEGY_KEEP_LOCAL = 0;
    public static final int STRATEGY_KEEP_REMOTE = 1;
    public static final int STRATEGY_KEEP_NEWEST = 2;
    public static final int STRATEGY_MERGE = 3;
    
    private int defaultStrategy;
    
    public ConflictResolver() {
        this.defaultStrategy = STRATEGY_KEEP_NEWEST;
    }
    
    public ConflictResolver(int defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }
    
    /**
     * 解决冲突
     * 
     * @param localRecord 本地记录
     * @param remoteRecord 远程记录
     * @return 解决后的记录（返回 null 表示需要用户手动选择）
     */
    public Record resolve(Record localRecord, Record remoteRecord) {
        if (localRecord == null) {
            return remoteRecord;
        }
        if (remoteRecord == null) {
            return localRecord;
        }

        switch (defaultStrategy) {
            case STRATEGY_KEEP_LOCAL:
                return localRecord;
                
            case STRATEGY_KEEP_REMOTE:
                return remoteRecord;
                
            case STRATEGY_KEEP_NEWEST:
                if (localRecord.lastModified >= remoteRecord.lastModified) {
                    return localRecord;
                } else {
                    return remoteRecord;
                }
                
            case STRATEGY_MERGE:
                return mergeRecords(localRecord, remoteRecord);
                
            default:
                return remoteRecord;
        }
    }
    
    /**
     * 合并记录（智能合并）
     * 只合并不同字段，相同字段保留最新的
     */
    private Record mergeRecords(Record local, Record remote) {
        if (local == null) {
            return remote;
        }
        if (remote == null) {
            return local;
        }

        Record merged = new Record();
        
        // 基础信息
        merged.id = local.id;
        merged.notionPageId = remote.notionPageId;
        
        // 金额 - 保留本地的（用户手动输入）
        merged.amount = local.amount;
        
        // 类型 - 保留本地的
        merged.type = local.type;
        
        // 分类 - 以本地的为准
        merged.category = local.category;
        
        // 时间 - 保留最新的
        if (local.lastModified > remote.lastModified) {
            merged.time = local.time;
        } else {
            merged.time = remote.time;
        }
        
        // 备注 - 合并两个备注
        String localRemark = local.remark == null ? "" : local.remark.trim();
        String remoteRemark = remote.remark == null ? "" : remote.remark.trim();
        if (!localRemark.isEmpty() && !remoteRemark.isEmpty()) {
            merged.remark = localRemark.equals(remoteRemark)
                    ? localRemark
                    : localRemark + " | " + remoteRemark;
        } else if (!localRemark.isEmpty()) {
            merged.remark = localRemark;
        } else {
            merged.remark = remoteRemark;
        }
        
        merged.lastModified = Math.max(local.lastModified, remote.lastModified);
        
        return merged;
    }
    
    /**
     * 记录数据结构
     */
    public static class Record {
        public String id;
        public String notionPageId;
        public double amount;
        public String type; // "expense" 或 "income"
        public String category;
        public long time;
        public String remark;
        public long lastModified;
    }
}

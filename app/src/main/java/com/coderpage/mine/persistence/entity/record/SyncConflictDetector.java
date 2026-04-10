package com.coderpage.mine.persistence.entity.record;

/**
 * 同步冲突检测器
 * 
 * 检测本地与云端记录是否存在冲突，并提供解决策略建议。
 * 
 * @author Flandre Scarlet
 * @since 1.0.0
 */
public class SyncConflictDetector {
    
    private static final long TIME_TOLERANCE_MS = 1000; // 1秒容差
    
    /**
     * 检测是否存在冲突
     */
    public static boolean hasConflict(LocalRecord local, CloudRecord cloud) {
        if (local == null || cloud == null) return false;
        
        // 时间戳差异超过容差且任一方已修改
        long timeDiff = Math.abs(local.getLastModified() - cloud.getLastEditedTime());
        boolean bothModified = timeDiff > TIME_TOLERANCE_MS;
        
        // 内容差异
        boolean contentDiff = !equals(local.getAmount(), cloud.getAmount())
            || !equals(local.getCategory(), cloud.getCategory())
            || !equals(local.getRemark(), cloud.getRemark());
        
        return bothModified && contentDiff;
    }
    
    /**
     * 建议解决策略
     */
    public static SyncConflictStrategy suggestStrategy(LocalRecord local, CloudRecord cloud) {
        if (!hasConflict(local, cloud)) {
            return SyncConflictStrategy.MERGE; // 无冲突直接合并
        }
        
        // 本地更新 → 保留本地
        if (local.getLastModified() > cloud.getLastEditedTime()) {
            return SyncConflictStrategy.REJECT_LOCAL;
        }
        
        // 云端更新 → 保留云端
        if (cloud.getLastEditedTime() > local.getLastModified()) {
            return SyncConflictStrategy.REJECT_CLOUD;
        }
        
        // 同时修改 → 创建备份
        return SyncConflictStrategy.BACKUP;
    }
    
    private static boolean equals(double a, double b) {
        return Math.abs(a - b) < 0.001;
    }
    
    private static boolean equals(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }
}

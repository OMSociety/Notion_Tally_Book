package com.coderpage.mine.app.tally.persistence.sql.entity;

import androidx.room.Entity;
import com.coderpage.mine.app.tally.persistence.sql.DatabaseConstants;
import androidx.room.PrimaryKey;

/**
 * 同步历史记录实体
 *
 * 记录每次同步操作的详细信息
 */
@Entity(tableName = DatabaseConstants.TABLE_SYNC_HISTORY)
public class SyncHistoryEntity {

    /** 同步历史ID */
    @PrimaryKey(autoGenerate = true)
    private long id;

    /** 同步时间（时间戳） */
    private long syncTime;

    /** 同步方向：0=本地→Notion，1=Notion→本地，2=双向同步 */
    private int syncDirection;

    /** 上传数量 */
    private int uploaded;

    /** 拉取数量 */
    private int pulled;

    /** 失败数量 */
    private int failed;

    /** 错误信息（如果有） */
    private String errorMessage;

    /** 同步状态：0=进行中，1=成功，2=失败 */
    private int status;

    public SyncHistoryEntity() {
        this.syncTime = System.currentTimeMillis();
        this.status = 0;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSyncTime() { return syncTime; }
    public void setSyncTime(long syncTime) { this.syncTime = syncTime; }

    public int getSyncDirection() { return syncDirection; }
    public void setSyncDirection(int syncDirection) { this.syncDirection = syncDirection; }

    public int getUploaded() { return uploaded; }
    public void setUploaded(int uploaded) { this.uploaded = uploaded; }

    public int getPulled() { return pulled; }
    public void setPulled(int pulled) { this.pulled = pulled; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    /** 获取同步方向描述 */
    public String getDirectionText() {
        switch (syncDirection) {
            case 0: return "本地→Notion";
            case 1: return "Notion→本地";
            case 2: return "双向同步";
            default: return "未知";
        }
    }

    /** 获取同步状态描述 */
    public String getStatusText() {
        switch (status) {
            case 0: return "进行中";
            case 1: return "成功";
            case 2: return "失败";
            default: return "未知";
        }
    }
}

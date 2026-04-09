package com.coderpage.mine.app.tally.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.coderpage.mine.app.tally.sync.NotionApiClient;
import com.coderpage.mine.app.tally.sync.NotionSyncManager;

/**
 * Notion 同步配置管理
 *
 * 统一管理所有 Notion 同步相关的配置项，基于 SharedPreferences。
 *
 * <b>配置项一览：</b>
 * <table border="1">
 *   <tr><th>配置项</th><th>Key</th><th>默认值</th></tr>
 *   <tr><td>Integration Token</td><td>notion_token</td><td>空字符串</td></tr>
 *   <tr><td>Database ID</td><td>database_id</td><td>空字符串</td></tr>
 *   <tr><td>同步开关</td><td>sync_enabled</td><td>false</td></tr>
 *   <tr><td>自动同步</td><td>auto_sync</td><td>false</td></tr>
 *   <tr><td>同步方向</td><td>sync_direction</td><td>双向(SYNC_BIDIRECTIONAL)</td></tr>
 *   <tr><td>冲突解决策略</td><td>conflict_resolution</td><td>MERGE_FIELDS</td></tr>
 *   <tr><td>上次同步时间</td><td>last_sync_time</td><td>0</td></tr>
 * </table>
 *
 * @author abner-l
 * @since 0.7.5
 */
public class NotionConfig {

    /** SharedPreferences 文件名 */
    private static final String PREF_NAME = "notion_sync_config";

    private static final String KEY_NOTION_TOKEN    = "notion_token";
    private static final String KEY_DATABASE_ID     = "database_id";
    private static final String KEY_SYNC_ENABLED    = "sync_enabled";
    private static final String KEY_LAST_SYNC_TIME  = "last_sync_time";
    private static final String KEY_AUTO_SYNC       = "auto_sync";
    private static final String KEY_SYNC_DIRECTION  = "sync_direction";
    private static final String KEY_CONFLICT_RESOLUTION = "conflict_resolution";

    /** 同步方向：本地 → Notion */
    public static final int SYNC_TO_NOTION       = 0;
    /** 同步方向：Notion → 本地 */
    public static final int SYNC_FROM_NOTION     = 1;
    /** 同步方向：双向同步 */
    public static final int SYNC_BIDIRECTIONAL   = 2;

    private final SharedPreferences prefs;

    /**
     * 构造配置管理器
     *
     * @param context Android Context（Application Context 最佳）
     */
    public NotionConfig(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ==================== Token 与 Database ID ====================

    /**
     * 设置 Notion Integration Token
     *
     * @param token secret_xxx 格式的 Token
     */
    public void setNotionToken(String token) {
        prefs.edit().putString(KEY_NOTION_TOKEN, token != null ? token : "").apply();
    }

    /**
     * 获取 Notion Integration Token
     *
     * @return Token 字符串，未设置返回空字符串
     */
    public String getNotionToken() {
        return prefs.getString(KEY_NOTION_TOKEN, "");
    }

    /**
     * 设置 Notion Database ID
     *
     * @param id 32位字母数字组合的 Database ID
     */
    public void setDatabaseId(String id) {
        prefs.edit().putString(KEY_DATABASE_ID, id != null ? id : "").apply();
    }

    /**
     * 获取 Notion Database ID
     *
     * @return Database ID，未设置返回空字符串
     */
    public String getDatabaseId() {
        return prefs.getString(KEY_DATABASE_ID, "");
    }

    // ==================== 同步开关 ====================

    /**
     * 设置同步总开关
     *
     * @param enabled true=启用，false=禁用
     */
    public void setSyncEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply();
    }

    /**
     * 获取同步总开关状态
     *
     * @return true=已启用，false=未启用（默认）
     */
    public boolean isSyncEnabled() {
        return prefs.getBoolean(KEY_SYNC_ENABLED, false);
    }

    /**
     * 设置是否自动同步
     *
     * @param auto true=开启自动同步，false=手动同步
     */
    public void setAutoSync(boolean auto) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, auto).apply();
    }

    /**
     * 获取自动同步开关状态
     *
     * @return true=已开启自动同步，false=手动同步（默认）
     */
    public boolean isAutoSync() {
        return prefs.getBoolean(KEY_AUTO_SYNC, false);
    }

    // ==================== 同步方向 ====================

    /**
     * 设置同步方向
     *
     * @param direction 方向常量，取值：
     *                  {@link #SYNC_TO_NOTION}（本地→Notion）
     *                  {@link #SYNC_FROM_NOTION}（Notion→本地）
     *                  {@link #SYNC_BIDIRECTIONAL}（双向，默认值）
     */
    public void setSyncDirection(int direction) {
        prefs.edit().putInt(KEY_SYNC_DIRECTION, direction).apply();
    }

    /**
     * 获取当前同步方向
     *
     * @return 同步方向常量，默认为 {@link #SYNC_BIDIRECTIONAL}
     */
    public int getSyncDirection() {
        return prefs.getInt(KEY_SYNC_DIRECTION, SYNC_BIDIRECTIONAL);
    }

    /**
     * 获取同步方向的中文描述
     *
     * @return 方向描述字符串，用于 UI 显示
     */
    public String getSyncDirectionLabel() {
        switch (getSyncDirection()) {
            case SYNC_TO_NOTION:   return "本地 → Notion";
            case SYNC_FROM_NOTION: return "Notion → 本地";
            case SYNC_BIDIRECTIONAL: return "双向同步";
            default: return "未知方向";
        }
    }

    // ==================== 冲突解决策略 ====================

    /**
     * 设置冲突解决策略
     *
     * @param resolution 冲突解决策略常量，取值：
     *                  {@link NotionSyncManager.ConflictResolution#LOCAL_WINS}（本地优先）
     *                  {@link NotionSyncManager.ConflictResolution#NOTION_WINS}（Notion优先）
     *                  {@link NotionSyncManager.ConflictResolution#MERGE_FIELDS}（字段级合并，默认值）
     *                  {@link NotionSyncManager.ConflictResolution#MANUAL}（手动解决）
     */
    public void setConflictResolution(NotionSyncManager.ConflictResolution resolution) {
        prefs.edit().putString(KEY_CONFLICT_RESOLUTION, resolution.name()).apply();
    }

    /**
     * 获取冲突解决策略
     *
     * @return 冲突解决策略，默认为 {@link NotionSyncManager.ConflictResolution#MERGE_FIELDS}
     */
    public NotionSyncManager.ConflictResolution getConflictResolution() {
        String name = prefs.getString(KEY_CONFLICT_RESOLUTION, 
                NotionSyncManager.ConflictResolution.MERGE_FIELDS.name());
        try {
            return NotionSyncManager.ConflictResolution.valueOf(name);
        } catch (IllegalArgumentException e) {
            return NotionSyncManager.ConflictResolution.MERGE_FIELDS;
        }
    }

    /**
     * 获取冲突解决策略的中文描述
     *
     * @return 策略描述字符串，用于 UI 显示
     */
    public String getConflictResolutionLabel() {
        switch (getConflictResolution()) {
            case LOCAL_WINS:    return "本地优先";
            case NOTION_WINS:  return "Notion 优先";
            case MERGE_FIELDS: return "字段级合并";
            case MANUAL:       return "手动解决";
            default:           return "未知策略";
        }
    }

    // ==================== 上次同步时间 ====================

    /**
     * 设置上次同步时间
     *
     * @param time 毫秒级时间戳
     */
    public void setLastSyncTime(long time) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, time).apply();
    }

    /**
     * 获取上次同步时间
     *
     * @return 毫秒级时间戳，从未同步返回 0
     */
    public long getLastSyncTime() {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0);
    }

    /**
     * 获取上次同步时间的可读格式
     *
     * @return 格式化时间字符串，从未同步返回 "从未同步"
     */
    public String getLastSyncTimeLabel() {
        long time = getLastSyncTime();
        if (time == 0) return "从未同步";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(time));
    }

    // ==================== 便捷判断 ====================

    /**
     * 判断配置是否完整（Token 和 Database ID 均已设置）
     *
     * 这是发起同步前的必要检查条件
     *
     * @return true=配置完整，可以发起同步
     */
    public boolean isConfigured() {
        String token = getNotionToken();
        String dbId = getDatabaseId();
        return token != null && !token.isEmpty()
                && dbId != null && !dbId.isEmpty();
    }

    /**
     * 清空所有配置
     *
     * 等价于关闭同步+清除凭证，保留设置界面数据
     */
    public void clearConfig() {
        prefs.edit()
                .remove(KEY_NOTION_TOKEN)
                .remove(KEY_DATABASE_ID)
                .remove(KEY_SYNC_ENABLED)
                .remove(KEY_AUTO_SYNC)
                .remove(KEY_SYNC_DIRECTION)
                .remove(KEY_CONFLICT_RESOLUTION)
                .remove(KEY_LAST_SYNC_TIME)
                .apply();
    }

    /**
     * 重置所有配置为默认值
     *
     * 清除所有数据，恢复出厂状态
     */
    public void reset() {
        prefs.edit().clear().apply();
    }
}

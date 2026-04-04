package com.coderpage.mine.app.tally.config;

import android.content.Context;
import android.content.SharedPreferences;

/** Notion 同步配置管理 */
public class NotionConfig {
    private static final String PREF_NAME = "notion_sync_config";
    private static final String KEY_NOTION_TOKEN = "notion_token";
    private static final String KEY_DATABASE_ID = "database_id";
    private static final String KEY_SYNC_ENABLED = "sync_enabled";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    private static final String KEY_AUTO_SYNC = "auto_sync";
    private static final String KEY_SYNC_DIRECTION = "sync_direction";

    private final SharedPreferences prefs;

    public NotionConfig(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setNotionToken(String token) { prefs.edit().putString(KEY_NOTION_TOKEN, token).apply(); }
    public String getNotionToken() { return prefs.getString(KEY_NOTION_TOKEN, ""); }

    public void setDatabaseId(String id) { prefs.edit().putString(KEY_DATABASE_ID, id).apply(); }
    public String getDatabaseId() { return prefs.getString(KEY_DATABASE_ID, ""); }

    public void setSyncEnabled(boolean enabled) { prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply(); }
    public boolean isSyncEnabled() { return prefs.getBoolean(KEY_SYNC_ENABLED, false); }

    public void setAutoSync(boolean auto) { prefs.edit().putBoolean(KEY_AUTO_SYNC, auto).apply(); }
    public boolean isAutoSync() { return prefs.getBoolean(KEY_AUTO_SYNC, false); }

    public void setSyncDirection(int direction) { prefs.edit().putInt(KEY_SYNC_DIRECTION, direction).apply(); }
    public int getSyncDirection() { return prefs.getInt(KEY_SYNC_DIRECTION, 2); }

    public void setLastSyncTime(long time) { prefs.edit().putLong(KEY_LAST_SYNC_TIME, time).apply(); }
    public long getLastSyncTime() { return prefs.getLong(KEY_LAST_SYNC_TIME, 0); }

    public boolean isConfigured() { return !getNotionToken().isEmpty() && !getDatabaseId().isEmpty(); }

    public void clearConfig() { prefs.edit().clear().apply(); }
}

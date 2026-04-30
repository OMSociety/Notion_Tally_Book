package com.coderpage.mine.app.tally.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.coderpage.mine.app.tally.security.SensitiveDataCipher;

/**
 * Notion 同步配置
 * 
 * @author Flandre Scarlet
 */
public class NotionConfig {
    
    private static final String PREF_NAME = "notion_config";
    private static final String KEY_INTEGRATION_TOKEN = "integration_token";
    private static final String KEY_DATABASE_ID = "database_id";
    private static final String KEY_SYNC_MODE = "sync_mode";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    private static final String KEY_AUTO_SYNC = "auto_sync";
    private static final String KEY_SYNC_WIFI_ONLY = "sync_wifi_only";
    
    public static final int SYNC_MODE_LOCAL_TO_NOTION = 0;
    public static final int SYNC_MODE_NOTION_TO_LOCAL = 1;
    public static final int SYNC_MODE_BIDIRECTIONAL = 2;
    
    private final SharedPreferences prefs;
    private final Context appContext;
    private static NotionConfig instance;
    
    private NotionConfig(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized NotionConfig getInstance(Context context) {
        if (instance == null) {
            instance = new NotionConfig(context);
        }
        return instance;
    }
    
    // Integration Token
    public void setIntegrationToken(String token) {
        // 加密失败会抛出 RuntimeException，不会将明文写入存储
        String encrypted = SensitiveDataCipher.encrypt(appContext, token);
        prefs.edit()
                .putString(KEY_INTEGRATION_TOKEN, encrypted)
                .apply();
    }
    
    public String getIntegrationToken() {
        String stored = prefs.getString(KEY_INTEGRATION_TOKEN, "");
        String decrypted = "";
        try {
            decrypted = SensitiveDataCipher.decrypt(appContext, stored);
        } catch (SensitiveDataCipher.CipherException e) {
            android.util.Log.e("NotionConfig", "Failed to decrypt integration token", e);
        }
        if (!stored.isEmpty() && stored.startsWith("enc:") && decrypted.isEmpty()) {
            throw new SecurityException("Failed to decrypt integration token. " +
                    "The token was encrypted but cannot be decrypted — " +
                    "this typically happens after app reinstallation or data migration.");
        }
        return decrypted;
    }
    
    public boolean hasIntegrationToken() {
        return !getIntegrationToken().isEmpty();
    }
    
    // Database ID
    public void setDatabaseId(String databaseId) {
        prefs.edit().putString(KEY_DATABASE_ID, databaseId).apply();
    }
    
    public String getDatabaseId() {
        return prefs.getString(KEY_DATABASE_ID, "");
    }
    
    public boolean hasDatabaseId() {
        return !getDatabaseId().isEmpty();
    }
    
    // Sync Mode
    public void setSyncMode(int mode) {
        prefs.edit().putInt(KEY_SYNC_MODE, mode).apply();
    }
    
    public int getSyncMode() {
        return prefs.getInt(KEY_SYNC_MODE, SYNC_MODE_BIDIRECTIONAL);
    }
    
    // Last Sync Time
    public void setLastSyncTime(long timestamp) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply();
    }
    
    public long getLastSyncTime() {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0);
    }
    
    // Auto Sync
    public void setAutoSync(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply();
    }
    
    public boolean isAutoSyncEnabled() {
        return prefs.getBoolean(KEY_AUTO_SYNC, false);
    }
    
    // WiFi Only
    public void setSyncWifiOnly(boolean enabled) {
        prefs.edit().putBoolean(KEY_SYNC_WIFI_ONLY, enabled).apply();
    }
    
    public boolean isSyncWifiOnly() {
        return prefs.getBoolean(KEY_SYNC_WIFI_ONLY, true);
    }
    
    // Check if configured
    public boolean isConfigured() {
        return hasIntegrationToken() && hasDatabaseId();
    }
    
    // Clear config
    public void clear() {
        prefs.edit().clear().apply();
    }
}

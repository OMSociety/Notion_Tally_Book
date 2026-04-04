package com.coderpage.mine.app.tally.sync;

import android.content.Context;
import com.coderpage.mine.app.tally.config.NotionConfig;
import com.coderpage.mine.persistence.entity.TallyRecord;
import java.util.ArrayList;
import java.util.List;

/** Notion 同步管理器 */
public class NotionSyncManager {
    public static final int SYNC_TO_NOTION = 0;
    public static final int SYNC_FROM_NOTION = 1;
    public static final int SYNC_BIDIRECTIONAL = 2;

    private final NotionConfig config;
    private final NotionApiClient apiClient;
    private SyncListener listener;

    public interface SyncListener {
        void onSyncStart();
        void onSyncProgress(int current, int total, String message);
        void onSyncComplete(int synced, int failed);
        void onSyncError(String error);
    }

    public NotionSyncManager(Context context) {
        this.config = new NotionConfig(context);
        this.apiClient = new NotionApiClient();
        if (config.isConfigured()) apiClient.setCredentials(config.getNotionToken(), config.getDatabaseId());
    }

    public void setSyncListener(SyncListener l) { this.listener = l; }
    public boolean isConfigured() { return config.isConfigured(); }
    public NotionConfig getConfig() { return config; }

    public void updateCredentials(String token, String databaseId) {
        config.setNotionToken(token);
        config.setDatabaseId(databaseId);
        apiClient.setCredentials(token, databaseId);
    }

    public void sync() {
        if (!config.isConfigured()) { if (listener != null) listener.onSyncError("请先配置 Notion API"); return; }
        if (listener != null) listener.onSyncStart();
        switch (config.getSyncDirection()) {
            case SYNC_TO_NOTION: syncToNotion(); break;
            case SYNC_FROM_NOTION: syncFromNotion(); break;
            case SYNC_BIDIRECTIONAL: syncBidirectional(); break;
        }
    }

    private void syncToNotion() {
        List<TallyRecord> localRecords = getLocalUnsyncedRecords();
        int total = localRecords.size(), synced = 0, failed = 0;
        for (TallyRecord r : localRecords) {
            if (listener != null) listener.onSyncProgress(synced + failed + 1, total, "同步: " + r.getRemark());
            final int s = synced, f = failed;
            apiClient.createRecord(r, new NotionApiClient.NotionCallback<String>() {
                @Override public void onSuccess(String notionId) { updateLocalRecordSynced(r.getId(), notionId); synced = s + 1; }
                @Override public void onError(String e) { failed = f + 1; }
            });
        }
        config.setLastSyncTime(System.currentTimeMillis());
        if (listener != null) listener.onSyncComplete(synced, failed);
    }

    private void syncFromNotion() {
        apiClient.queryDatabase(new NotionApiClient.NotionCallback<List<TallyRecord>>() {
            @Override public void onSuccess(List<TallyRecord> records) {
                int added = 0, updated = 0;
                for (TallyRecord nr : records) {
                    TallyRecord local = findLocalByNotionId(nr.getNotionId());
                    if (local == null) { addLocalRecord(nr); added++; }
                    else if (nr.getLastModified() > local.getLastModified()) { updateLocalRecord(nr); updated++; }
                    if (listener != null) listener.onSyncProgress(added + updated, records.size(), "处理中...");
                }
                config.setLastSyncTime(System.currentTimeMillis());
                if (listener != null) listener.onSyncComplete(added, updated);
            }
            @Override public void onError(String e) { if (listener != null) listener.onSyncError(e); }
        });
    }

    private void syncBidirectional() {
        apiClient.queryDatabase(new NotionApiClient.NotionCallback<List<TallyRecord>>() {
            @Override public void onSuccess(List<TallyRecord> records) { mergeRecords(records); }
            @Override public void onError(String e) { if (listener != null) listener.onSyncError(e); }
        });
    }

    private void mergeRecords(List<TallyRecord> notionRecords) {
        int localUpdated = 0, notionUpdated = 0;
        List<TallyRecord> toUpload = new ArrayList<>();
        for (TallyRecord nr : notionRecords) {
            TallyRecord local = findLocalByNotionId(nr.getNotionId());
            if (local == null) { addLocalRecord(nr); localUpdated++; }
            else if (nr.getLastModified() > local.getLastModified()) { updateLocalRecord(nr); localUpdated++; }
            else if (local.getLastModified() > nr.getLastModified() && !local.isSynced()) { toUpload.add(local); }
        }
        for (TallyRecord r : toUpload) {
            final int[] st = new int[]{0};
            apiClient.updateRecord(r.getNotionId(), r, new NotionApiClient.NotionCallback<Boolean>() {
                @Override public void onSuccess(Boolean b) { st[0] = 1; } @Override public void onError(String e) { st[0] = 2; }
            });
            if (st[0] == 1) notionUpdated++;
        }
        config.setLastSyncTime(System.currentTimeMillis());
        if (listener != null) listener.onSyncComplete(localUpdated, notionUpdated);
    }

    // 需要根据实际项目实现
    private List<TallyRecord> getLocalUnsyncedRecords() { return new ArrayList<>(); }
    private void updateLocalRecordSynced(long id, String notionId) { }
    private TallyRecord findLocalByNotionId(String notionId) { return null; }
    private void addLocalRecord(TallyRecord record) { }
    private void updateLocalRecord(TallyRecord record) { }
}

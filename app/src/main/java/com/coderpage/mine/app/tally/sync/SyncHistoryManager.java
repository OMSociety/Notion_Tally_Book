package com.coderpage.mine.app.tally.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.coderpage.mine.app.tally.sync.NotionSyncManager.SyncConflict;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SyncHistoryManager {

    private static final String TAG = "SyncHistoryManager";
    private static final String PREF_NAME = "sync_history";
    private static final String KEY_LAST_SYNC = "last_sync_time";
    private static final String KEY_SYNC_HISTORY = "sync_history";
    private static final String KEY_CONFLICT_HISTORY = "conflict_history";
    private static final int MAX_HISTORY_SIZE = 50;

    private final SharedPreferences prefs;

    public SyncHistoryManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void logSync(long timestamp, String direction, int localUpdated, int notionUpdated) {
        try {
            JSONObject entry = new JSONObject();
            entry.put("timestamp", timestamp);
            entry.put("direction", direction);
            entry.put("localUpdated", localUpdated);
            entry.put("notionUpdated", notionUpdated);
            addToHistory(KEY_SYNC_HISTORY, entry);
            prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply();
            Log.i(TAG, "Sync logged: " + direction);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to log sync", e);
        }
    }

    public void logConflict(SyncConflict conflict) {
        try {
            JSONObject entry = new JSONObject();
            entry.put("timestamp", System.currentTimeMillis());
            entry.put("notionId", conflict.notionId);
            entry.put("conflictingFields", new JSONArray(conflict.conflictingFields));
            entry.put("resolution", conflict.resolution.name());
            addToHistory(KEY_CONFLICT_HISTORY, entry);
            Log.w(TAG, "Conflict logged: " + conflict.notionId);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to log conflict", e);
        }
    }

    public void logConflicts(List<SyncConflict> conflicts) {
        for (SyncConflict conflict : conflicts) {
            logConflict(conflict);
        }
    }

    public long getLastSyncTime() {
        return prefs.getLong(KEY_LAST_SYNC, 0);
    }

    public List<JSONObject> getSyncHistory() {
        return getHistory(KEY_SYNC_HISTORY);
    }

    public List<JSONObject> getConflictHistory() {
        return getHistory(KEY_CONFLICT_HISTORY);
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_SYNC_HISTORY).remove(KEY_CONFLICT_HISTORY).apply();
    }

    public int getUnresolvedConflictCount() {
        List<JSONObject> conflicts = getConflictHistory();
        int count = 0;
        for (JSONObject conflict : conflicts) {
            try {
                if ("MANUAL".equals(conflict.optString("resolution", ""))) {
                    count++;
                }
            } catch (JSONException e) {}
        }
        return count;
    }

    private void addToHistory(String key, JSONObject entry) {
        List<JSONObject> history = getHistory(key);
        history.add(0, entry);
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(history.size() - 1);
        }
        JSONArray array = new JSONArray();
        for (JSONObject item : history) {
            array.put(item);
        }
        prefs.edit().putString(key, array.toString()).apply();
    }

    private List<JSONObject> getHistory(String key) {
        List<JSONObject> result = new ArrayList<>();
        String json = prefs.getString(key, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                result.add(array.getJSONObject(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse history: " + json, e);
        }
        return result;
    }
}

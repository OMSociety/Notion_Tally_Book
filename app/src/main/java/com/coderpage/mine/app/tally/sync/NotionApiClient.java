package com.coderpage.mine.app.tally.sync;

import android.os.Handler;
import android.os.Looper;
import com.coderpage.mine.persistence.entity.TallyRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/** Notion API 客户端 */
public class NotionApiClient {
    private static final String NOTION_API_BASE = "https://api.notion.com/v1";
    private static final String NOTION_VERSION = "2022-06-28";
    private String apiToken, databaseId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void setCredentials(String token, String databaseId) { this.apiToken = token; this.databaseId = databaseId; }

    public interface NotionCallback<T> { void onSuccess(T result); void onError(String error); }

    public void queryDatabase(NotionCallback<List<TallyRecord>> callback) {
        new Thread(() -> {
            try {
                List<TallyRecord> records = new ArrayList<>();
                String cursor = null;
                do {
                    JSONObject result = queryPage(cursor);
                    JSONArray results = result.optJSONArray("results");
                    if (results != null) for (int i = 0; i < results.length(); i++) {
                        TallyRecord r = parsePageToRecord(results.getJSONObject(i));
                        if (r != null) records.add(r);
                    }
                    cursor = result.optJSONObject("next_cursor");
                    if (cursor != null) cursor = cursor.optString("cursor");
                } while (cursor != null && !cursor.isEmpty());
                deliverSuccess(callback, records);
            } catch (Exception e) { deliverError(callback, e.getMessage()); }
        }).start();
    }

    public void createRecord(TallyRecord record, NotionCallback<String> callback) {
        new Thread(() -> {
            try { deliverSuccess(callback, createPage(record)); }
            catch (Exception e) { deliverError(callback, e.getMessage()); }
        }).start();
    }

    public void updateRecord(String pageId, TallyRecord record, NotionCallback<Boolean> callback) {
        new Thread(() -> {
            try { updatePage(pageId, record); deliverSuccess(callback, true); }
            catch (Exception e) { deliverError(callback, e.getMessage()); }
        }).start();
    }

    private JSONObject queryPage(String cursor) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(NOTION_API_BASE + "/databases/" + databaseId + "/query").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiToken);
        conn.setRequestProperty("Notion-Version", NOTION_VERSION);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        if (cursor != null) conn.getOutputStream().write(("{\"start_cursor\":\"" + cursor + "\"}").getBytes());
        return readResponse(conn);
    }

    private String createPage(TallyRecord record) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(NOTION_API_BASE + "/pages").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiToken);
        conn.setRequestProperty("Notion-Version", NOTION_VERSION);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(buildPageFromRecord(record).toString().getBytes());
        return new JSONObject(readBody(conn)).getString("id");
    }

    private void updatePage(String pageId, TallyRecord record) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(NOTION_API_BASE + "/pages/" + pageId).openConnection();
        conn.setRequestMethod("PATCH");
        conn.setRequestProperty("Authorization", "Bearer " + apiToken);
        conn.setRequestProperty("Notion-Version", NOTION_VERSION);
        conn.setDoOutput(true);
        conn.getOutputStream().write(buildPageFromRecord(record).toString().getBytes());
        readResponse(conn);
    }

    private JSONObject buildPageFromRecord(TallyRecord record) {
        JSONObject page = new JSONObject();
        page.put("parent", new JSONObject().put("database_id", databaseId));
        JSONObject props = new JSONObject();
        props.put("金额", new JSONObject().put("number", record.getAmount()));
        props.put("分类", new JSONObject().put("select", new JSONObject().put("name", record.getCategory() != null ? record.getCategory() : "未分类")));
        props.put("备注", new JSONObject().put("rich_text", new JSONArray().put(new JSONObject().put("text", new JSONObject().put("content", record.getRemark() != null ? record.getRemark() : "")))));
        props.put("时间", new JSONObject().put("date", new JSONObject().put("start", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date(record.getTime())))));
        props.put("类型", new JSONObject().put("select", new JSONObject().put("name", record.getType() == 0 ? "支出" : "收入")));
        props.put("状态", new JSONObject().put("select", new JSONObject().put("name", "活跃")));
        page.put("properties", props);
        return page;
    }

    private TallyRecord parsePageToRecord(JSONObject page) {
        try {
            TallyRecord r = new TallyRecord();
            r.setNotionId(page.getString("id"));
            JSONObject props = page.getJSONObject("properties");
            r.setAmount(props.optJSONObject("金额") != null ? props.getJSONObject("金额").optDouble("number", 0) : 0);
            r.setCategory(props.optJSONObject("分类") != null ? props.getJSONObject("分类").optJSONObject("select").optString("name", "") : "");
            if (props.optJSONObject("备注") != null) { JSONArray rt = props.getJSONObject("备注").optJSONArray("rich_text"); if (rt != null && rt.length() > 0) r.setRemark(rt.getJSONObject(0).optJSONObject("text").optString("content", "")); }
            if (props.optJSONObject("时间") != null) { String d = props.getJSONObject("时间").optJSONObject("date").optString("start", ""); if (!d.isEmpty()) r.setTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(d).getTime()); }
            String t = props.optJSONObject("类型") != null ? props.getJSONObject("类型").optJSONObject("select").optString("name", "支出") : "支出";
            r.setType(t.equals("收入") ? 1 : 0);
            r.setSynced(true);
            return r;
        } catch (Exception e) { return null; }
    }

    private String readBody(HttpURLConnection conn) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getResponseCode() < 300 ? conn.getInputStream() : conn.getErrorStream()));
        StringBuilder sb = new StringBuilder(); String l;
        while ((l = br.readLine()) != null) sb.append(l);
        br.close();
        if (conn.getResponseCode() >= 300) throw new Exception(sb.toString());
        return sb.toString();
    }

    private JSONObject readResponse(HttpURLConnection conn) throws Exception { return new JSONObject(readBody(conn)); }

    private <T> void deliverSuccess(NotionCallback<T> cb, T r) { mainHandler.post(() -> cb.onSuccess(r)); }
    private <T> void deliverError(NotionCallback<T> cb, String e) { mainHandler.post(() -> cb.onError(e)); }
}

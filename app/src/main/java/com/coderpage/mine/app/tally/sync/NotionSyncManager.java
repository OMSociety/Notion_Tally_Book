package com.coderpage.mine.app.tally.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.coderpage.mine.app.tally.config.NotionConfig;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Notion 同步管理器
 * 
 * @author Flandre Scarlet
 */
public class NotionSyncManager {
    
    private static final String TAG = "NotionSyncManager";
    
    private final Context context;
    private final NotionConfig config;
    private final NotionGateway notionGateway;
    private final ConflictService conflictService;
    private final SyncRepository syncRepository;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Gson gson;
    
    private SyncListener listener;
    private boolean isSyncing = false;
    private String currentSyncType = "manual";
    
    public interface SyncListener {
        void onSyncStart();
        void onSyncProgress(int current, int total, String message);
        void onSyncComplete(SyncResult result);
        void onSyncError(String error);
    }
    
    public static class SyncResult {
        public int uploadedCount;
        public int downloadedCount;
        public int conflictCount;
        public int errorCount;
        public long syncTime;

        public List<String> errors = new ArrayList<>();
    }
    
    public NotionSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.config = NotionConfig.getInstance(context);
        this.notionGateway = new NotionGateway(NotionApiClient.getInstance(config));
        this.conflictService = new ConflictService();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
        
        // 初始化数据库
        TallyDatabase database = TallyDatabase.getInstance();
        this.syncRepository = new SyncRepository(database.recordDao());
    }
    
    public void setSyncListener(SyncListener listener) {
        this.listener = listener;
    }
    
    /**
     * 开始同步
     */
    public void startSync() {
        if (isSyncing) {
            notifyError("同步已在进行中");
            return;
        }
        
        if (!config.isConfigured()) {
            notifyError("请先配置 Notion Integration Token 和 Database ID");
            return;
        }
        
        isSyncing = true;
        notifyStart();
        
        executor.execute(() -> {
            SyncResult result = new SyncResult();
            long startTime = System.currentTimeMillis();
            
            try {
                // 验证 Notion 配置
                if (!notionGateway.validateConfig()) {
                    throw new IOException("Notion 配置无效，请检查 Token 和 Database ID");
                }
                
                int mode = config.getSyncMode();
                
                switch (mode) {
                    case NotionConfig.SYNC_MODE_LOCAL_TO_NOTION:
                        uploadLocalRecords(result);
                        break;
                    case NotionConfig.SYNC_MODE_NOTION_TO_LOCAL:
                        downloadRemoteRecords(result);
                        break;
                    case NotionConfig.SYNC_MODE_BIDIRECTIONAL:
                        bidirectionalSync(result);
                        break;
                }
                
                result.syncTime = System.currentTimeMillis() - startTime;
                config.setLastSyncTime(System.currentTimeMillis());
                
                mainHandler.post(() -> {
                    isSyncing = false;
                    if (listener != null) {
                        listener.onSyncComplete(result);

                    }
                });
                
            } catch (Exception e) {
                result.errors.add(e.getMessage());
                result.errorCount++;
                mainHandler.post(() -> {
                    isSyncing = false;
                    if (listener != null) {
                        listener.onSyncError(e.getMessage());

                    }
                });
            }
        });
    }
    
    /**
     * 双向同步
     */
    private void bidirectionalSync(SyncResult result) throws IOException {
        // 1. 获取本地记录
        List<ConflictResolver.Record> localRecords = getLocalRecords();
        notifyProgress(0, localRecords.size(), "获取本地记录...");
        
        // 2. 获取远程记录
        List<ConflictResolver.Record> remoteRecords = getRemoteRecords();
        notifyProgress(1, 2, "获取远程记录...");
        
        // 3. 创建本地记录映射
        Map<String, ConflictResolver.Record> localMap = new HashMap<>();
        for (ConflictResolver.Record record : localRecords) {
            if (record.id != null) {
                localMap.put(record.id, record);
            }
        }
        
        // 4. 创建远程记录映射（通过 notionPageId）
        Map<String, ConflictResolver.Record> remoteMap = new HashMap<>();
        for (ConflictResolver.Record record : remoteRecords) {
            if (record.notionPageId != null) {
                remoteMap.put(record.notionPageId, record);
            }
        }
        
        int total = localRecords.size() + remoteRecords.size();
        if (total == 0) {
            notifyProgress(1, 1, "没有需要同步的记录");
            return;
        }
        
        int current = 0;
        
        // 5. 处理本地记录
        for (ConflictResolver.Record local : localRecords) {
            current++;
            notifyProgress(current, total, "同步 " + current + "/" + total);
            
            if (local.notionPageId != null && remoteMap.containsKey(local.notionPageId)) {
                // 双方都有 - 冲突处理
                ConflictResolver.Record remote = remoteMap.get(local.notionPageId);
                ConflictResolver.Record winner = conflictService.resolve(local, remote);
                if (winner == local) {
                    uploadRecord(local);
                    result.uploadedCount++;
                } else {
                    updateLocalRecord(winner);
                    result.downloadedCount++;
                }
                result.conflictCount++;
            } else if (local.notionPageId != null) {
                // 本地已有 notionPageId，但当前远端列表中不存在，先清理旧 id，交由 uploadRecord 执行重建并回填
                local.notionPageId = null;
                uploadRecord(local);
                result.uploadedCount++;
            } else {
                // 新记录，需要上传
                uploadRecord(local);
                result.uploadedCount++;
            }
        }
        
        // 6. 处理远程新增记录
        List<ConflictResolver.Record> remoteOnlyRecords =
                conflictService.findRemoteOnlyRecords(localRecords, remoteRecords);
        for (ConflictResolver.Record remote : remoteOnlyRecords) {
            saveRemoteRecord(remote);
            result.downloadedCount++;
        }
    }
    
    /**
     * 上传本地记录到 Notion
     */
    private void uploadLocalRecords(SyncResult result) throws IOException {
        List<ConflictResolver.Record> localRecords = getLocalRecords();
        for (int i = 0; i < localRecords.size(); i++) {
            ConflictResolver.Record record = localRecords.get(i);
            // 只上传还没有 notionPageId 的记录
            if (record.notionPageId == null || record.notionPageId.isEmpty()) {
                uploadRecord(record);
                result.uploadedCount++;
            }
            notifyProgress(i + 1, localRecords.size(), "上传 " + (i + 1) + "/" + localRecords.size());
        }
    }
    
    /**
     * 从 Notion 下载记录
     */
    private void downloadRemoteRecords(SyncResult result) throws IOException {
        List<ConflictResolver.Record> remoteRecords = getRemoteRecords();
        for (int i = 0; i < remoteRecords.size(); i++) {
            saveRemoteRecord(remoteRecords.get(i));
            result.downloadedCount++;
            notifyProgress(i + 1, remoteRecords.size(), "下载 " + (i + 1) + "/" + remoteRecords.size());
        }
    }
    
    /**
     * 获取本地记录
     */
    private List<ConflictResolver.Record> getLocalRecords() {
        return syncRepository.getLocalRecords();
    }
    
    /**
     * 获取远程记录
     */
    private List<ConflictResolver.Record> getRemoteRecords() throws IOException {
        List<ConflictResolver.Record> records = new ArrayList<>();
        String cursor = null;
        
        do {
            String response = notionGateway.queryDatabase(cursor);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            
            JsonArray results = json.getAsJsonArray("results");
            for (int i = 0; i < results.size(); i++) {
                JsonObject page = results.get(i).getAsJsonObject();
                ConflictResolver.Record record = parseNotionPage(page);
                if (record != null) {
                    records.add(record);
                }
            }
            
            if (json.has("has_more") && json.get("has_more").getAsBoolean()) {
                cursor = json.get("next_cursor").getAsString();
            } else {
                cursor = null;
            }
        } while (cursor != null);
        
        return records;
    }
    
    /**
     * 解析 Notion Page 为 Record
     */
    private ConflictResolver.Record parseNotionPage(JsonObject page) {
        try {
            ConflictResolver.Record record = new ConflictResolver.Record();
            record.notionPageId = page.get("id").getAsString();
            
            JsonObject properties = page.getAsJsonObject("properties");
            
            // 解析金额
            if (properties.has("金额")) {
                JsonObject amountProp = properties.getAsJsonObject("金额");
                if (amountProp.has("number") && amountProp.get("number") != null) {
                    record.amount = amountProp.get("number").getAsDouble();
                }
            }
            
            // 解析类型（支出/收入）
            if (properties.has("类型")) {
                JsonObject typeProp = properties.getAsJsonObject("类型");
                if (typeProp.has("select") && typeProp.get("select") != null) {
                    record.type = typeProp.getAsJsonObject("select").get("name").getAsString();
                }
            }
            
            // 解析分类
            if (properties.has("分类")) {
                JsonObject categoryProp = properties.getAsJsonObject("分类");
                if (categoryProp.has("rich_text")) {
                    JsonArray text = categoryProp.getAsJsonArray("rich_text");
                    if (text.size() > 0) {
                        record.category = text.get(0).getAsJsonObject().get("plain_text").getAsString();
                    }
                }
            }
            
            // 解析时间
            if (properties.has("时间")) {
                JsonObject dateProp = properties.getAsJsonObject("时间");
                if (dateProp.has("date") && dateProp.get("date") != null) {
                    JsonObject date = dateProp.getAsJsonObject("date");
                    if (date.has("start") && date.get("start") != null) {
                        String dateStr = date.get("start").getAsString();
                        record.time = parseNotionDateToMillis(dateStr);
                    }
                }
            }
            
            // 解析备注
            if (properties.has("备注")) {
                JsonObject remarkProp = properties.getAsJsonObject("备注");
                if (remarkProp.has("rich_text")) {
                    JsonArray text = remarkProp.getAsJsonArray("rich_text");
                    if (text.size() > 0) {
                        record.remark = text.get(0).getAsJsonObject().get("plain_text").getAsString();
                    }
                }
            }
            
            record.lastModified = record.time;
            
            return record;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private long parseNotionDateToMillis(String dateStr) throws java.text.ParseException {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new java.text.ParseException("date string empty", 0);
        }
        String value = dateStr.trim();
        String normalized = normalizeIsoTimezone(value);
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                sdf.setLenient(false);
                boolean hasZone = pattern.contains("Z") || pattern.contains("'Z'");
                if (!hasZone) {
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                Date parsed = sdf.parse(normalized);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (java.text.ParseException ignore) {
            }
        }
        throw new java.text.ParseException("unsupported date format: " + value, 0);
    }

    private String normalizeIsoTimezone(String value) {
        if (value.endsWith("Z")) {
            return value.substring(0, value.length() - 1) + "+0000";
        }
        int tzSignIndex = Math.max(value.lastIndexOf('+'), value.lastIndexOf('-'));
        int timeSeparatorIndex = value.lastIndexOf('T');
        if (tzSignIndex > timeSeparatorIndex && value.length() - tzSignIndex == 6 && value.charAt(value.length() - 3) == ':') {
            return value.substring(0, value.length() - 3) + value.substring(value.length() - 2);
        }
        return value;
    }
    
    /**
     * 上传记录到 Notion
     */
    private void uploadRecord(ConflictResolver.Record record) throws IOException {
        Map<String, Object> properties = new HashMap<>();
        
        // 金额
        Map<String, Object> amount = new HashMap<>();
        amount.put("number", record.amount);
        properties.put("金额", amount);
        
        // 类型
        Map<String, Object> type = new HashMap<>();
        Map<String, Object> typeSelect = new HashMap<>();
        String typeValue = "支出".equals(record.type) || "expense".equals(record.type) ? "支出" : "收入";
        typeSelect.put("name", typeValue);
        type.put("select", typeSelect);
        properties.put("类型", type);
        
        // 分类
        Map<String, Object> category = new HashMap<>();
        List<Map<String, Object>> categoryText = new ArrayList<>();
        Map<String, Object> text = new HashMap<>();
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("content", record.category != null ? record.category : "");
        text.put("text", textContent);
        categoryText.add(text);
        category.put("rich_text", categoryText);
        properties.put("分类", category);
        
        // 时间
        Map<String, Object> date = new HashMap<>();
        Map<String, Object> dateValue = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        dateValue.put("start", sdf.format(new Date(record.time)));
        date.put("date", dateValue);
        properties.put("时间", date);
        
        // 备注
        if (record.remark != null && !record.remark.isEmpty()) {
            Map<String, Object> remark = new HashMap<>();
            List<Map<String, Object>> remarkText = new ArrayList<>();
            Map<String, Object> remarkContent = new HashMap<>();
            Map<String, Object> remarkTextContent = new HashMap<>();
            remarkTextContent.put("content", record.remark);
            remarkContent.put("text", remarkTextContent);
            remarkText.add(remarkContent);
            remark.put("rich_text", remarkText);
            properties.put("备注", remark);
        }
        
        Map<String, Object> parent = new HashMap<>();
        parent.put("database_id", config.getDatabaseId());
        
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("parent", parent);
        pageData.put("properties", properties);
        
        if (record.notionPageId != null && !record.notionPageId.isEmpty()) {
            notionGateway.updatePage(record.notionPageId, gson.toJson(pageData));
            updateLocalRecord(record);
            return;
        }

        String response = notionGateway.createPage(gson.toJson(pageData));
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (json.has("id")) {
            record.notionPageId = json.get("id").getAsString();
            updateLocalRecord(record);
        }
    }
    
    /**
     * 更新本地记录
     */
    private void updateLocalRecord(ConflictResolver.Record record) {
        syncRepository.updateLocalRecord(record);
    }
    
    /**
     * 保存远程记录到本地
     */
    private void saveRemoteRecord(ConflictResolver.Record record) {
        syncRepository.saveRemoteRecord(record);
    }
    
    /**
     * 验证 Notion 数据库结构
     */
    public boolean validateDatabase() {
        try {
            String schema = notionGateway.getDatabaseSchema();
            JsonObject json = JsonParser.parseString(schema).getAsJsonObject();
            
            // 检查必要的字段
            JsonObject properties = json.getAsJsonObject("properties");
            boolean hasAmount = properties.has("金额");
            boolean hasType = properties.has("类型");
            boolean hasCategory = properties.has("分类");
            boolean hasTime = properties.has("时间");
            
            return hasAmount && hasType && hasCategory && hasTime;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void notifyStart() {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onSyncStart();
            }
        });
    }
    
    private void notifyProgress(int current, int total, String message) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onSyncProgress(current, total, message);
            }
        });
    }
    
    private void notifyError(String error) {
        mainHandler.post(() -> {
            isSyncing = false;
            if (listener != null) {
                listener.onSyncError(error);
            }
        });
    }
    
    public boolean isSyncing() {
        return isSyncing;
    }
}

package com.coderpage.mine.app.tally.sync;

import android.os.Handler;
import android.os.Looper;

import com.coderpage.mine.persistence.entity.TallyRecord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Notion API 客户端
 *
 * 封装与 Notion API v1 的所有 HTTP 通信，使用 OkHttp 作为底层库。
 *
 * <b>支持的字段映射</b>（与数据库模板字段名严格对应）：
 * <table border="1">
 *   <tr><th>Notion 字段名</th><th>对应 TallyRecord 字段</th><th>Notion 类型</th></tr>
 *   <tr><td>金额</td><td>amount</td><td>number</td></tr>
 *   <tr><td>分类</td><td>category</td><td>select</td></tr>
 *   <tr><td>备注</td><td>remark</td><td>rich_text</td></tr>
 *   <tr><td>时间</td><td>time</td><td>date</td></tr>
 *   <tr><td>类型</td><td>type (0=支出,1=收入)</td><td>select</td></tr>
 *   <tr><td>状态</td><td>固定值"活跃"</td><td>select</td></tr>
 * </table>
 *
 * <b>创建 Database 前请先参考</b>：{@code docs/NOTION_DATABASE_TEMPLATE.md}
 *
 * @author abner-l
 * @since 0.7.5
 */
public class NotionApiClient {

    /** Notion API 基础地址 */
    private static final String NOTION_API_BASE = "https://api.notion.com/v1";

    /** Notion API 版本（固定值，勿修改） */
    private static final String NOTION_VERSION = "2022-06-28";

    /** JSON 请求体媒体类型 */
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    /** OkHttp 连接超时（秒） */
    private static final int CONNECT_TIMEOUT_SEC = 15;

    /** OkHttp 读取超时（秒） */
    private static final int READ_TIMEOUT_SEC = 30;

    // ==================== Notion 数据库字段名常量 ====================
    // 这些字段名必须与 Notion 数据库中的列名完全一致，否则同步静默失败
    /** 金额字段名 */
    static final String FIELD_AMOUNT = "金额";
    /** 分类字段名 */
    static final String FIELD_CATEGORY = "分类";
    /** 备注字段名 */
    static final String FIELD_REMARK = "备注";
    /** 时间字段名 */
    static final String FIELD_TIME = "时间";
    /** 类型字段名（支出/收入） */
    static final String FIELD_TYPE = "类型";
    /** 状态字段名（活跃/删除） */
    static final String FIELD_STATUS = "状态";

    // ==================== 内部状态 ====================
    private String apiToken;
    private String databaseId;

    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    /**
     * API 操作回调接口
     *
     * @param <T> 成功结果的类型
     */
    public interface NotionCallback<T> {
        /** 操作成功，结果非空 */
        void onSuccess(T result);
        /** 操作失败，error 为错误描述（已脱敏，不含敏感路径/堆栈） */
        void onError(String error);
    }

    /**
     * 构造 NotionApiClient，同时初始化 OkHttpClient
     */
    public NotionApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                // OkHttp 自动跟随重定向，这里不需要额外处理
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 设置 API 凭证
     *
     * @param token      Notion Integration Token（格式：secret_xxx）
     * @param databaseId Notion Database ID（32位字母数字）
     */
    public void setCredentials(String token, String databaseId) {
        this.apiToken = token;
        this.databaseId = databaseId;
    }

    // ==================== 公开 API 方法 ====================

    /**
     * 查询 Notion Database 中的所有记录（自动处理分页）
     *
     * Notion API 单次返回最多 100 条，使用 next_cursor 翻页。
     * 本方法循环请求直到没有更多页面。
     *
     * @param callback 回调，成功时 result 为全部记录列表
     */
    public void queryDatabase(NotionCallback<List<TallyRecord>> callback) {
        List<TallyRecord> allRecords = new ArrayList<>();
        String[] cursorHolder = new String[1]; // 0: cursor, 1: hasMore

        doQueryPage(null, allRecords, cursorHolder, 0, callback);
    }

    /**
     * 递归分页查询：查询完一页后自动查询下一页
     *
     * @param cursor       当前页游标，null 表示查询第一页
     * @param accumulators 已收集的记录
     * @param cursorHolder 长度2数组：[0]=cursor, [1]=保留
     * @param depth        当前递归深度（防止无限递归）
     * @param callback     最终回调
     */
    private void doQueryPage(String cursor, List<TallyRecord> accumulators,
                             String[] cursorHolder, int depth,
                             NotionCallback<List<TallyRecord>> callback) {
        if (depth > 100) {
            deliverError(callback, "Notion 数据量过大，超过了最大递归深度（100页），请联系开发者");
            return;
        }

        JSONObject body = new JSONObject();
        if (cursor != null && !cursor.isEmpty()) {
            body.put("start_cursor", cursor);
        }

        Request request = buildPostRequest(
                NOTION_API_BASE + "/databases/" + databaseId + "/query",
                body.length() > 0 ? body.toString() : null
        );

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, "网络请求失败，请检查网络连接: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful()) {
                        String errorBody = body != null ? body.string() : "无响应体";
                        String friendly = parseNotionError(errorBody);
                        deliverError(callback, friendly);
                        return;
                    }

                    String json = body != null ? body.string() : "{}";
                    JSONObject result = new JSONObject(json);
                    JSONArray results = result.optJSONArray("results");

                    if (results != null) {
                        for (int i = 0; i < results.length(); i++) {
                            TallyRecord r = parsePageToRecord(results.getJSONObject(i));
                            if (r != null) {
                                accumulators.add(r);
                            }
                        }
                    }

                    // 检查是否有下一页
                    JSONObject nextCursor = result.optJSONObject("next_cursor");
                    boolean hasMore = result.optBoolean("has_more", false);
                    String nextCursorStr = nextCursor != null ? nextCursor.optString("cursor", null) : null;

                    if (hasMore && nextCursorStr != null && !nextCursorStr.isEmpty()) {
                        // 继续查询下一页（同步递归到同一线程，避免回调地狱）
                        doQueryPage(nextCursorStr, accumulators, cursorHolder, depth + 1, callback);
                    } else {
                        // 所有页查完
                        deliverSuccess(callback, new ArrayList<>(accumulators));
                    }
                } catch (Exception e) {
                    deliverError(callback, "解析 Notion 响应失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 在 Notion Database 中创建一条新记录
     *
     * @param record   要创建的记账记录
     * @param callback 回调，success 时 result 为新创建页面的 Page ID
     */
    public void createRecord(TallyRecord record, NotionCallback<String> callback) {
        JSONObject pageBody = buildPageFromRecord(record);

        Request request = buildPostRequest(
                NOTION_API_BASE + "/pages",
                pageBody.toString()
        );

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, "创建 Notion 记录失败，请检查网络: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful()) {
                        String errorBody = body != null ? body.string() : "无响应体";
                        deliverError(callback, parseNotionError(errorBody));
                        return;
                    }
                    String json = body != null ? body.string() : "{}";
                    String pageId = new JSONObject(json).getString("id");
                    deliverSuccess(callback, pageId);
                } catch (Exception e) {
                    deliverError(callback, "解析 Notion 创建响应失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 更新 Notion 中已有记录的属性
     *
     * @param pageId   Notion Page ID（由 createRecord 返回）
     * @param record   更新后的记录数据
     * @param callback 回调，success 时 result 为 true
     */
    public void updateRecord(String pageId, TallyRecord record, NotionCallback<Boolean> callback) {
        JSONObject pageBody = buildPageFromRecord(record);

        String url = NOTION_API_BASE + "/pages/" + pageId;
        Request request = new Request.Builder()
                .url(url)
                .patch(RequestBody.create(pageBody.toString(), MEDIA_TYPE_JSON))
                .addHeader("Authorization", "Bearer " + apiToken)
                .addHeader("Notion-Version", NOTION_VERSION)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverError(callback, "更新 Notion 记录失败，请检查网络: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful()) {
                        String errorBody = body != null ? body.string() : "无响应体";
                        deliverError(callback, parseNotionError(errorBody));
                        return;
                    }
                    deliverSuccess(callback, true);
                } catch (Exception e) {
                    deliverError(callback, "解析 Notion 更新响应失败: " + e.getMessage());
                }
            }
        });
    }

    // ==================== 请求构建 ====================

    /**
     * 构建 POST 请求（带统一 Header）
     *
     * @param url  目标 URL
     * @param body 请求体 JSON 字符串，null 表示无 body
     * @return OkHttp Request 对象
     */
    private Request buildPostRequest(String url, String body) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiToken)
                .addHeader("Notion-Version", NOTION_VERSION)
                .addHeader("Content-Type", "application/json");

        if (body != null && !body.isEmpty()) {
            builder.post(RequestBody.create(body, MEDIA_TYPE_JSON));
        } else {
            builder.post(RequestBody.create("", MEDIA_TYPE_JSON));
        }

        return builder.build();
    }

    // ==================== 数据模型转换 ====================

    /**
     * 将 TallyRecord 转换为 Notion Page 的 JSON 结构
     *
     * @param record 记账记录
     * @return Notion API 格式的 page JSON
     */
    JSONObject buildPageFromRecord(TallyRecord record) {
        JSONObject page = new JSONObject();
        page.put("parent", new JSONObject().put("database_id", databaseId));

        JSONObject props = new JSONObject();
        // 金额
        props.put(FIELD_AMOUNT, new JSONObject().put("number", record.getAmount()));
        // 分类（select 类型，值必须与 Notion 数据库选项完全匹配）
        props.put(FIELD_CATEGORY, new JSONObject()
                .put("select", new JSONObject()
                        .put("name", record.getCategory() != null ? record.getCategory() : "未分类")));
        // 备注（rich_text 类型，为空时传空数组）
        props.put(FIELD_REMARK, new JSONObject()
                .put("rich_text", buildRichText(record.getRemark())));
        // 时间（date 类型，ISO 8601 格式）
        props.put(FIELD_TIME, new JSONObject()
                .put("date", new JSONObject()
                        .put("start", formatIso8601(record.getTime()))));
        // 类型（支出/收入）
        String typeName = record.getType() == 0 ? "支出" : "收入";
        props.put(FIELD_TYPE, new JSONObject()
                .put("select", new JSONObject().put("name", typeName)));
        // 状态（固定"活跃"）
        props.put(FIELD_STATUS, new JSONObject()
                .put("select", new JSONObject().put("name", "活跃")));

        page.put("properties", props);
        return page;
    }

    /**
     * 构建 rich_text 类型的 JSON 数组
     *
     * @param text 文本内容（可为空）
     * @return JSONArray
     */
    private JSONArray buildRichText(String text) {
        JSONArray arr = new JSONArray();
        if (text != null && !text.isEmpty()) {
            arr.put(new JSONObject()
                    .put("text", new JSONObject().put("content", text)));
        }
        return arr;
    }

    /**
     * 将 Notion Page JSON 解析为 TallyRecord
     *
     * 解析失败（如字段类型不匹配、缺少必填字段）时返回 null，
     * 不会抛出异常。
     *
     * @param page Notion Page JSON 对象
     * @return 解析后的 TallyRecord，解析失败返回 null
     */
    TallyRecord parsePageToRecord(JSONObject page) {
        try {
            TallyRecord r = new TallyRecord();
            // id 即 Notion Page ID
            r.setNotionId(page.getString("id"));

            JSONObject props = page.getJSONObject("properties");

            // 金额（number）
            if (props.has(FIELD_AMOUNT) && !props.isNull(FIELD_AMOUNT)) {
                r.setAmount(props.getJSONObject(FIELD_AMOUNT).optDouble("number", 0));
            }

            // 分类（select）
            if (props.has(FIELD_CATEGORY) && !props.isNull(FIELD_CATEGORY)) {
                JSONObject select = props.getJSONObject(FIELD_CATEGORY).optJSONObject("select");
                if (select != null) {
                    r.setCategory(select.optString("name", ""));
                }
            }

            // 备注（rich_text）
            if (props.has(FIELD_REMARK) && !props.isNull(FIELD_REMARK)) {
                JSONArray richText = props.getJSONObject(FIELD_REMARK).optJSONArray("rich_text");
                if (richText != null && richText.length() > 0) {
                    JSONObject textObj = richText.optJSONObject(0);
                    if (textObj != null) {
                        JSONObject text = textObj.optJSONObject("text");
                        if (text != null) {
                            r.setRemark(text.optString("content", ""));
                        }
                    }
                }
            }

            // 时间（date）
            if (props.has(FIELD_TIME) && !props.isNull(FIELD_TIME)) {
                JSONObject date = props.getJSONObject(FIELD_TIME).optJSONObject("date");
                if (date != null) {
                    String start = date.optString("start", "");
                    if (!start.isEmpty()) {
                        r.setTime(parseIso8601(start));
                    }
                }
            }

            // 类型（select：支出/收入）
            if (props.has(FIELD_TYPE) && !props.isNull(FIELD_TYPE)) {
                JSONObject select = props.getJSONObject(FIELD_TYPE).optJSONObject("select");
                String typeName = select != null ? select.optString("name", "支出") : "支出";
                r.setType(typeName.equals("收入") ? 1 : 0);
            }

            // lastModified：使用 Notion 的 last_edited_time
            String lastEdited = page.optString("last_edited_time", "");
            if (!lastEdited.isEmpty()) {
                r.setLastModified(parseIso8601(lastEdited));
            } else {
                r.setLastModified(System.currentTimeMillis());
            }

            r.setSynced(true);
            return r;
        } catch (Exception e) {
            // 解析失败静默返回 null，不影响其他记录
            return null;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 将时间戳格式化为 ISO 8601 字符串（Notion API 要求）
     *
     * @param timestamp 毫秒级时间戳
     * @return ISO 8601 字符串，例：2024-01-15T08:30:00Z
     */
    private String formatIso8601(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(timestamp));
    }

    /**
     * 解析 ISO 8601 字符串为时间戳
     *
     * @param iso8601 ISO 8601 字符串
     * @return 毫秒级时间戳，解析失败返回 0
     */
    private long parseIso8601(String iso8601) {
        try {
            // 尝试标准 ISO 8601 格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return sdf.parse(iso8601).getTime();
        } catch (Exception e1) {
            try {
                // 尝试不带毫秒的格式
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                sdf2.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                return sdf2.parse(iso8601).getTime();
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    /**
     * 解析 Notion API 错误响应，返回对用户友好的错误信息
     *
     * @param errorBody Notion 返回的错误 JSON
     * @return 脱敏后的错误描述
     */
    private String parseNotionError(String errorBody) {
        try {
            JSONObject err = new JSONObject(errorBody);
            String code = err.optString("code", "");
            JSONObject body = err.optJSONObject("body");
            String message = err.optString("message", "");

            // Notion 常见错误码的中文友好提示
            switch (code) {
                case "unauthorized":
                    return "Notion Token 无效，请检查是否正确配置";
                case "object_not_found":
                    return "Notion Database 未找到，请检查 Database ID 是否正确";
                case "validation_error":
                    // 字段校验失败，给出具体字段提示
                    String field = "";
                    if (body != null) {
                        JSONArray errors = body.optJSONArray("errors");
                        if (errors != null && errors.length() > 0) {
                            field = errors.optJSONObject(0).optString("property", "");
                        }
                    }
                    return "Notion 字段验证失败，请检查字段名是否与模板一致" + (field.isEmpty() ? "" : "（字段: " + field + "）");
                case "rate_limited":
                    return "Notion 请求频率超限，请稍后再试";
                case "internal_server_error":
                    return "Notion 服务器内部错误，请稍后重试";
                default:
                    // 截断超长错误消息
                    return message.length() > 100 ? message.substring(0, 100) + "..." : message;
            }
        } catch (Exception e) {
            // JSON 解析失败，隐藏原始内容
            return "Notion API 返回了无效的响应";
        }
    }

    // ==================== 回调分发 ====================

    /** 将成功结果投递到主线程 */
    private <T> void deliverSuccess(NotionCallback<T> cb, T result) {
        mainHandler.post(() -> cb.onSuccess(result));
    }

    /** 将错误信息投递到主线程 */
    private <T> void deliverError(NotionCallback<T> cb, String error) {
        mainHandler.post(() -> cb.onError(error));
    }
}

package com.coderpage.mine.app.tally.sync;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Notion 数据库结构验证器
 *
 * 在首次同步前，检查目标 Database 是否包含记账所需的所有字段，
 * 并验证每个字段的类型是否匹配。
 *
 * <b>必需的字段及类型：</b>
 * <table border="1">
 *   <tr><th>字段名</th><th>必需</th><th>Notion 类型</th></tr>
 *   <tr><td>金额</td><td>是</td><td>number</td></tr>
 *   <tr><td>分类</td><td>是</td><td>select</td></tr>
 *   <tr><td>备注</td><td>是</td><td>rich_text</td></tr>
 *   <tr><td>时间</td><td>是</td><td>date</td></tr>
 *   <tr><td>类型</td><td>是</td><td>select</td></tr>
 *   <tr><td>状态</td><td>否（推荐）</td><td>select</td></tr>
 * </table>
 *
 * @author abner-l
 * @since 0.7.5
 */
public class NotionDatabaseValidator {

    private static final String NOTION_API_BASE = "https://api.notion.com/v1";
    private static final String NOTION_VERSION = "2022-06-28";
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;

    /**
     * 单个字段的校验结果
     */
    public static class FieldCheck {
        /** 字段名 */
        public final String fieldName;
        /** 是否必需 */
        public final boolean required;
        /** 期望的 Notion 类型 */
        public final String expectedType;
        /** 实际 Notion 类型（null 表示字段不存在） */
        public final String actualType;
        /** 错误描述，null 表示通过 */
        public final String error;

        FieldCheck(String fieldName, boolean required, String expectedType,
                   String actualType, String error) {
            this.fieldName = fieldName;
            this.required = required;
            this.expectedType = expectedType;
            this.actualType = actualType;
            this.error = error;
        }

        boolean isPassed() { return error == null; }
    }

    /**
     * 数据库校验结果
     */
    public static class ValidationResult {
        /** 是否完全通过 */
        public final boolean valid;
        /** 所有字段的校验详情 */
        public final List<FieldCheck> fields;
        /** 缺失的必需字段名列表 */
        public final List<String> missingRequiredFields;
        /** 类型不匹配的字段列表 */
        public final List<String> typeMismatchFields;
        /** 总体错误信息（供 UI 显示） */
        public final String summary;

        ValidationResult(List<FieldCheck> fields) {
            this.fields = fields;
            this.missingRequiredFields = new ArrayList<>();
            this.typeMismatchFields = new ArrayList<>();

            for (FieldCheck f : fields) {
                if (f.required && f.actualType == null) {
                    missingRequiredFields.add(f.fieldName);
                } else if (f.actualType != null && !f.expectedType.equalsIgnoreCase(f.actualType)) {
                    typeMismatchFields.add(f.fieldName + " (期望:" + f.expectedType + " 实际:" + f.actualType + ")");
                }
            }

            this.valid = missingRequiredFields.isEmpty() && typeMismatchFields.isEmpty();

            StringBuilder sb = new StringBuilder();
            if (!valid) {
                sb.append("⚠️ 数据库字段校验未通过：\n");
                if (!missingRequiredFields.isEmpty()) {
                    sb.append("缺失必需字段：").append(String.join("、", missingRequiredFields)).append("\n");
                }
                if (!typeMismatchFields.isEmpty()) {
                    sb.append("类型不匹配：").append(String.join("；", typeMismatchFields));
                }
            } else {
                sb.append("✅ 所有字段校验通过");
            }
            this.summary = sb.toString();
        }
    }

    public NotionDatabaseValidator() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    /**
     * 验证 Notion Database 是否符合记账模板要求
     *
     * 调用 Notion GET /databases/{id} 接口获取数据库 Schema，
     * 与预期字段列表逐一比对，返回详细的校验结果。
     *
     * @param token      Notion Integration Token
     * @param databaseId Database ID
     * @param callback   异步回调，result 为校验结果
     */
    public void validate(String token, String databaseId,
                         ValidationCallback callback) {
        String url = NOTION_API_BASE + "/databases/" + databaseId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Notion-Version", NOTION_VERSION)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络请求失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful()) {
                        String errorBody = body != null ? body.string() : "";
                        String friendly = parseNotionError(errorBody, response.code());
                        callback.onError(friendly);
                        return;
                    }

                    String json = body != null ? body.string() : "{}";
                    ValidationResult result = parseSchema(json);
                    callback.onResult(result);
                } catch (Exception e) {
                    callback.onError("解析数据库 Schema 失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 同步版本的验证方法
     *
     * @return 校验结果，失败时返回 invalid=true 的结果对象
     */
    public ValidationResult validateSync(String token, String databaseId) {
        final ValidationResult[] holder = new ValidationResult[1];
        final Exception[] errorHolder = new Exception[1];
        final boolean[] done = new boolean[1];
        Object lock = new Object();

        validate(token, databaseId, new ValidationCallback() {
            @Override
            public void onResult(ValidationResult result) {
                holder[0] = result;
                synchronized (lock) { done[0] = true; lock.notifyAll(); }
            }
            @Override
            public void onError(String error) {
                List<FieldCheck> list = new ArrayList<>();
                list.add(new FieldCheck("网络请求", true, "—", "—", error));
                holder[0] = new ValidationResult(list);
                synchronized (lock) { done[0] = true; lock.notifyAll(); }
            }
        });

        synchronized (lock) {
            while (!done[0]) {
                try { lock.wait(30000); } catch (InterruptedException ignored) {}
            }
        }
        return holder[0];
    }

    // ==================== Schema 解析 ====================

    /**
     * 解析 Notion Database Schema JSON，返回校验结果
     *
     * @param schemaJson GET /databases/{id} 返回的 JSON
     * @return ValidationResult
     */
    private ValidationResult parseSchema(String schemaJson) {
        List<FieldCheck> checks = new ArrayList<>();

        // 定义所有需要检查的字段
        // fieldName, required, expectedNotionType
        String[][] expectedFields = {
                {NotionApiClient.FIELD_AMOUNT,   "true",  "number"},
                {NotionApiClient.FIELD_CATEGORY, "true",  "rich_text"},
                {NotionApiClient.FIELD_REMARK,    "true",  "rich_text"},
                {NotionApiClient.FIELD_TIME,      "true",  "date"},
                {NotionApiClient.FIELD_TYPE,      "true",  "select"},
                {NotionApiClient.FIELD_STATUS,    "false", "select"},
        };

        try {
            JSONObject root = new JSONObject(schemaJson);
            JSONObject properties = root.optJSONObject("properties");
            if (properties == null) {
                // properties 为空，整个校验失败
                for (String[] f : expectedFields) {
                    checks.add(new FieldCheck(f[0], Boolean.parseBoolean(f[1]),
                            f[2], null,
                            "数据库 properties 为空，请确认 Token 是否有权限访问该数据库"));
                }
                return new ValidationResult(checks);
            }

            Set<String> existingFields = new HashSet<>();
            for (String key : properties.keySet()) {
                existingFields.add(key.trim().toLowerCase(Locale.ROOT));
            }

            for (String[] f : expectedFields) {
                String fieldName = f[0];
                boolean required = Boolean.parseBoolean(f[1]);
                String expectedType = f[2];
                String normalized = fieldName.trim().toLowerCase(Locale.ROOT);

                if (!existingFields.contains(normalized)) {
                    checks.add(new FieldCheck(fieldName, required, expectedType,
                            null, required ? "缺少必需字段「" + fieldName + "」" : null));
                } else {
                    // 字段存在，验证类型
                    JSONObject fieldDef = properties.optJSONObject(fieldName);
                    if (fieldDef == null) {
                        // 大小写不精确匹配，尝试遍历找
                        fieldDef = findFieldCaseInsensitive(properties, fieldName);
                    }
                    if (fieldDef != null) {
                        String actualType = fieldDef.optString("type", "");
                        if (!expectedType.equalsIgnoreCase(actualType)) {
                            checks.add(new FieldCheck(fieldName, required, expectedType,
                                    actualType, "类型不匹配：期望「" + expectedType + "」实际「" + actualType + "」"));
                        } else {
                            checks.add(new FieldCheck(fieldName, required, expectedType, actualType, null));
                        }
                    } else {
                        checks.add(new FieldCheck(fieldName, required, expectedType, null, "字段定义解析失败"));
                    }
                }
            }
        } catch (Exception e) {
            // JSON 解析失败，所有检查标记为失败
            for (String[] f : expectedFields) {
                checks.add(new FieldCheck(f[0], true, f[2], null,
                        "解析数据库 Schema 时发生异常: " + e.getMessage()));
            }
        }

        return new ValidationResult(checks);
    }

    /**
     * 大小写不敏感查找字段
     */
    private JSONObject findFieldCaseInsensitive(JSONObject properties, String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        for (String key : properties.keySet()) {
            if (key.trim().toLowerCase(Locale.ROOT).equals(normalized)) {
                return properties.optJSONObject(key);
            }
        }
        return null;
    }

    /**
     * 解析 Notion 错误响应
     */
    private String parseNotionError(String body, int httpCode) {
        if (httpCode == 401 || httpCode == 403) {
            return "权限不足，请确认已将 Integration 连接到该数据库（点击数据库右上角 ... → Add connections）";
        }
        if (httpCode == 404) {
            return "Database 未找到，请检查 Database ID 是否正确";
        }
        try {
            JSONObject err = new JSONObject(body);
            String code = err.optString("code", "");
            if (code.equals("unauthorized")) {
                return "Token 无效，请检查 Integration Token 是否正确";
            }
            return err.optString("message", "Notion 返回了未知错误");
        } catch (Exception e) {
            return "Notion API 错误 (HTTP " + httpCode + ")";
        }
    }

    /**
     * 校验结果回调接口
     */
    public interface ValidationCallback {
        void onResult(ValidationResult result);
        void onError(String error);
    }
}

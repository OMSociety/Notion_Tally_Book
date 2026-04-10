package com.coderpage.mine.app.tally.sync;

import com.coderpage.mine.persistence.entity.TallyRecord;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * NotionApiClient 转换逻辑单元测试
 * 
 * 通过反射测试私有方法：
 * - buildPageFromRecord: TallyRecord → Notion Page JSON
 * - parsePageToRecord:   Notion Page JSON → TallyRecord
 * - formatIso8601:       时间戳 → ISO 8601 字符串
 * - parseIso8601:        ISO 8601 字符串 → 时间戳
 * 
 * @author abner-l
 * @since 0.7.5
 */
public class NotionApiClientTest {

    private NotionApiClient client;

    // 反射工具
    private Object invokePrivate(Object target, String methodName, Object... args) throws Exception {
        Class<?>[] argTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = args[i] == null ? Object.class : args[i].getClass();
        }
        Method m = target.getClass().getDeclaredMethod(methodName, argTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    @Before
    public void setUp() {
        // 使用反射实例化（NotionApiClient 默认构造器，不需要 token/dbId 也能构造）
        client = new NotionApiClient();
    }

    // ==================== formatIso8601 / parseIso8601 ====================

    @Test
    public void formatIso8601_basic() throws Exception {
        // 2024-03-02 12:00:00 UTC
        long timestamp = 1709380800000L;
        String iso = (String) invokePrivate(client, "formatIso8601", timestamp);
        assertNotNull(iso);
        assertTrue("应包含日期", iso.startsWith("2024"));
        assertTrue("应包含T", iso.contains("T"));
    }

    @Test
    public void parseIso8601_basic() throws Exception {
        String iso = "2024-03-02T12:00:00.000Z";
        long ts = (Long) invokePrivate(client, "parseIso8601", iso);
        assertEquals(1709380800000L, ts);
    }

    @Test
    public void roundTrip_timestampConsistency() throws Exception {
        long original = 1712000000000L;
        String iso = (String) invokePrivate(client, "formatIso8601", original);
        long restored = (Long) invokePrivate(client, "parseIso8601", iso);
        assertEquals(original, restored);
    }

    // ==================== buildPageFromRecord ====================

    @Test
    public void buildPageFromRecord_basicFields() throws Exception {
        TallyRecord record = TallyRecord.create();
        record.setAmount(100.5);
        record.setCategory("CanYin");
        record.setRemark("午餐");
        record.setTime(1712000000000L);
        record.setType(0); // 支出

        JSONObject page = (JSONObject) invokePrivate(client, "buildPageFromRecord", record);

        assertNotNull(page);
        assertNotNull(page.getJSONObject("parent"));
        assertNotNull(page.get("properties"));
    }

    @Test
    public void buildPageFromRecord_expenseType() throws Exception {
        TallyRecord record = TallyRecord.create();
        record.setAmount(50.0);
        record.setCategory("交通");
        record.setTime(1712000000000L);
        record.setType(0); // 支出

        JSONObject page = (JSONObject) invokePrivate(client, "buildPageFromRecord", record);
        JSONObject props = page.getJSONObject("properties");
        JSONObject typeField = props.getJSONObject("类型");
        assertEquals("支出", typeField.getJSONObject("select").getString("name"));
    }

    @Test
    public void buildPageFromRecord_incomeType() throws Exception {
        TallyRecord record = TallyRecord.create();
        record.setAmount(5000.0);
        record.setCategory("薪资");
        record.setTime(1712000000000L);
        record.setType(1); // 收入

        JSONObject page = (JSONObject) invokePrivate(client, "buildPageFromRecord", record);
        JSONObject props = page.getJSONObject("properties");
        JSONObject typeField = props.getJSONObject("类型");
        assertEquals("收入", typeField.getJSONObject("select").getString("name"));
    }

    @Test
    public void buildPageFromRecord_nullCategory_becomes未分类() throws Exception {
        TallyRecord record = TallyRecord.create();
        record.setAmount(10.0);
        record.setCategory(null);
        record.setTime(1712000000000L);
        record.setType(0);

        JSONObject page = (JSONObject) invokePrivate(client, "buildPageFromRecord", record);
        JSONObject props = page.getJSONObject("properties");
        String categoryName = props.getJSONObject("分类")
                .getJSONObject("select").getString("name");
        assertEquals("未分类", categoryName);
    }

    @Test
    public void buildPageFromRecord_emptyRemark_hasEmptyRichText() throws Exception {
        TallyRecord record = TallyRecord.create();
        record.setAmount(10.0);
        record.setCategory("餐饮");
        record.setRemark("");
        record.setTime(1712000000000L);
        record.setType(0);

        JSONObject page = (JSONObject) invokePrivate(client, "buildPageFromRecord", record);
        JSONObject props = page.getJSONObject("properties");
        JSONArray richText = props.getJSONObject("备注").getJSONArray("rich_text");
        assertEquals(0, richText.length());
    }

    @Test
    public void buildPageFromRecord_withRemark_hasRichText() throws Exception {
        TallyRecord record = TallyRecord.create();
        record.setAmount(10.0);
        record.setCategory("餐饮");
        record.setRemark("好吃");
        record.setTime(1712000000000L);
        record.setType(0);

        JSONObject page = (JSONObject) invokePrivate(client, "buildPageFromRecord", record);
        JSONObject props = page.getJSONObject("properties");
        JSONArray richText = props.getJSONObject("备注").getJSONArray("rich_text");
        assertEquals(1, richText.length());
        assertEquals("好吃", richText.getJSONObject(0).getJSONObject("text").getString("content"));
    }

    // ==================== parsePageToRecord ====================

    @Test
    public void parsePageToRecord_basicFields() throws Exception {
        JSONObject page = buildMockNotionPage(
                "page-123",
                88.88,
                "CanYin",
                "午饭",
                "2024-04-02T08:00:00.000Z",
                "支出"
        );

        TallyRecord r = (TallyRecord) invokePrivate(client, "parsePageToRecord", page);

        assertNotNull(r);
        assertEquals("page-123", r.getNotionId());
        assertEquals(88.88, r.getAmount(), 0.001);
        assertEquals("CanYin", r.getCategory());
        assertEquals("午饭", r.getRemark());
        assertEquals(0, r.getType()); // 支出
        assertTrue(r.isSynced());
    }

    @Test
    public void parsePageToRecord_incomeType() throws Exception {
        JSONObject page = buildMockNotionPage(
                "page-456",
                5000.0,
                "XinZi",
                "月薪",
                "2024-04-01T00:00:00.000Z",
                "收入"
        );

        TallyRecord r = (TallyRecord) invokePrivate(client, "parsePageToRecord", page);
        assertEquals(1, r.getType());
    }

    @Test
    public void parsePageToRecord_missingAmount_defaultsToZero() throws Exception {
        JSONObject page = buildMockNotionPageMissingField("金额");
        TallyRecord r = (TallyRecord) invokePrivate(client, "parsePageToRecord", page);
        assertEquals(0.0, r.getAmount(), 0.001);
    }

    @Test
    public void parsePageToRecord_nullAmountField_defaultsToZero() throws Exception {
        JSONObject page = buildMockNotionPage(
                "page-789",
                null,
                "CanYin",
                "",
                "2024-04-01T00:00:00.000Z",
                "支出"
        );

        TallyRecord r = (TallyRecord) invokePrivate(client, "parsePageToRecord", page);
        assertEquals(0.0, r.getAmount(), 0.001);
    }

    @Test
    public void parsePageToRecord_missingTime_defaultsToCurrentTime() throws Exception {
        JSONObject page = buildMockNotionPage(
                "page-time-missing",
                10.0,
                "Other",
                "",
                null,
                "支出"
        );

        long before = System.currentTimeMillis();
        TallyRecord r = (TallyRecord) invokePrivate(client, "parsePageToRecord", page);
        long after = System.currentTimeMillis();
        // lastModified 会 fallback 到当前时间
        assertTrue(r.getLastModified() >= before && r.getLastModified() <= after);
    }

    @Test
    public void parsePageToRecord_missingType_defaultsTo支出() throws Exception {
        JSONObject page = buildMockNotionPage(
                "page-type-missing",
                10.0,
                "Other",
                "",
                "2024-04-01T00:00:00.000Z",
                null
        );

        TallyRecord r = (TallyRecord) invokePrivate(client, "parsePageToRecord", page);
        assertEquals(0, r.getType()); // 默认支出
    }

    @Test
    public void parsePageToRecord_lastEditedTime_usedForLastModified() throws Exception {
        String editedTime = "2024-03-20T15:30:00.000Z";
        JSONObject page = buildMockNotionPage(
                "page-edited",
                10.0,
                "Other",
                "",
                "2024-04-01T00:00:00.000Z",
                "支出"
        );
        page.put("last_edited_time", editedTime);

        TallyRecord r = (TallyRecord) invokePrivate(client, "parsePageToRecord", page);
        long expected = (Long) invokePrivate(client, "parseIso8601", editedTime);
        assertEquals(expected, r.getLastModified());
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建模拟的 Notion Page JSON（包含完整必填字段）
     */
    private JSONObject buildMockNotionPage(String id, Double amount, String category,
                                           String remark, String dateStart, String typeName) {
        JSONObject page = new JSONObject();
        page.put("id", id);
        page.put("last_edited_time", "2024-04-01T00:00:00.000Z");

        JSONObject props = new JSONObject();

        // 金额
        if (amount != null) {
            props.put("金额", new JSONObject().put("number", amount));
        } else {
            props.put("金额", JSONObject.NULL);
        }

        // 分类（rich_text/text）
        JSONObject catField = new JSONObject();
        if (category != null && !category.isEmpty()) {
            catField.put("rich_text", new JSONArray()
                    .put(new JSONObject().put("text", new JSONObject().put("content", category))));
        } else {
            catField.put("rich_text", new JSONArray());
        }
        props.put("分类", catField);

        // 备注
        JSONObject remField = new JSONObject();
        if (remark != null && !remark.isEmpty()) {
            remField.put("rich_text", new JSONArray()
                    .put(new JSONObject().put("text", new JSONObject().put("content", remark))));
        } else {
            remField.put("rich_text", new JSONArray());
        }
        props.put("备注", remField);

        // 时间
        if (dateStart != null) {
            props.put("时间", new JSONObject()
                    .put("date", new JSONObject().put("start", dateStart)));
        } else {
            props.put("时间", JSONObject.NULL);
        }

        // 类型
        if (typeName != null) {
            props.put("类型", new JSONObject()
                    .put("select", new JSONObject().put("name", typeName)));
        } else {
            props.put("类型", JSONObject.NULL);
        }

        page.put("properties", props);
        return page;
    }

    /**
     * 构建缺少指定字段的 Notion Page JSON
     */
    private JSONObject buildMockNotionPageMissingField(String missingField) {
        JSONObject page = buildMockNotionPage(
                "page-missing",
                10.0,
                "CanYin",
                "测试",
                "2024-04-01T00:00:00.000Z",
                "支出"
        );
        page.getJSONObject("properties").remove(missingField);
        return page;
    }
}

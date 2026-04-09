package com.coderpage.mine.app.tally.persistence.model;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;

/**
 * Record 模型单元测试
 *
 * @author test
 * @since 0.8.0
 */
public class RecordTest {

    private Record record;

    @Before
    public void setUp() {
        record = new Record();
    }

    @Test
    public void testDefaultConstructor() {
        Record newRecord = new Record();
        assertNotNull(newRecord);
    }

    @Test
    public void testTypeConstants() {
        assertEquals(0, Record.TYPE_EXPENSE);
        assertEquals(1, Record.TYPE_INCOME);
    }

    @Test
    public void testSetAndGetId() {
        record.setId(12345L);
        assertEquals(12345L, record.getId());
    }

    @Test
    public void testSetAndGetAccountId() {
        record.setAccountId(100L);
        assertEquals(100L, record.getAccountId());
    }

    @Test
    public void testSetAndGetTime() {
        long timestamp = System.currentTimeMillis();
        record.setTime(timestamp);
        assertEquals(timestamp, record.getTime());
    }

    @Test
    public void testSetAndGetAmount() {
        // 测试金额精度处理
        record.setAmount(123.456);
        // setAmount会对金额四舍五入到两位小数
        assertEquals(123.46, record.getAmount(), 0.001);
    }

    @Test
    public void testAmountPrecision() {
        // 测试各种金额精度
        record.setAmount(100.999);
        assertEquals(101.0, record.getAmount(), 0.001);

        record.setAmount(50.004);
        assertEquals(50.0, record.getAmount(), 0.001);

        record.setAmount(0.0);
        assertEquals(0.0, record.getAmount(), 0.001);
    }

    @Test
    public void testSetAndGetCategoryUniqueName() {
        String uniqueName = "food_dinner";
        record.setCategoryUniqueName(uniqueName);
        assertEquals(uniqueName, record.getCategoryUniqueName());
    }

    @Test
    public void testSetAndGetCategoryName() {
        String categoryName = "晚餐";
        record.setCategoryName(categoryName);
        assertEquals(categoryName, record.getCategoryName());
    }

    @Test
    public void testSetAndGetCategoryIcon() {
        String icon = "ic_food";
        record.setCategoryIcon(icon);
        assertEquals(icon, record.getCategoryIcon());
    }

    @Test
    public void testSetAndGetDesc() {
        String desc = "测试备注";
        record.setDesc(desc);
        assertEquals(desc, record.getDesc());
    }

    @Test
    public void testSetAndGetSyncId() {
        String syncId = "uuid-12345";
        record.setSyncId(syncId);
        assertEquals(syncId, record.getSyncId());
    }

    @Test
    public void testSetAndGetSyncStatus() {
        record.setSyncStatus(RecordEntity.SYNC_STATUS_SYNCED);
        assertEquals(RecordEntity.SYNC_STATUS_SYNCED, record.getSyncStatus());
    }

    @Test
    public void testSetAndGetType() {
        // 测试支出类型
        record.setType(Record.TYPE_EXPENSE);
        assertEquals(Record.TYPE_EXPENSE, record.getType());

        // 测试收入类型
        record.setType(Record.TYPE_INCOME);
        assertEquals(Record.TYPE_INCOME, record.getType());
    }

    @Test
    public void testCreateEntity() {
        // 设置Record的所有字段
        record.setId(1L);
        record.setAccountId(2L);
        record.setTime(1609459200000L);
        record.setAmount(100.50);
        record.setCategoryUniqueName("food_lunch");
        record.setCategoryName("午餐");
        record.setCategoryIcon("ic_food");
        record.setDesc("测试午餐");
        record.setSyncId("sync-uuid-123");
        record.setSyncStatus(RecordEntity.SYNC_STATUS_PENDING);
        record.setType(Record.TYPE_EXPENSE);

        // 创建Entity
        RecordEntity entity = record.createEntity();

        // 验证字段映射正确
        assertEquals(record.getId(), entity.getId());
        assertEquals(record.getAccountId(), entity.getAccountId());
        assertEquals(record.getTime(), entity.getTime());
        assertEquals(record.getAmount(), entity.getAmount(), 0.001);
        assertEquals(record.getCategoryUniqueName(), entity.getCategoryUniqueName());
        assertEquals(record.getDesc(), entity.getDesc());
        assertEquals(record.getSyncId(), entity.getSyncId());
        assertEquals(record.getSyncStatus(), entity.getSyncStatus());
        assertEquals(record.getType(), entity.getType());
    }

    @Test
    public void testNegativeAmount() {
        // 测试负数金额（理论上不应该出现，但测试边界情况）
        record.setAmount(-50.0);
        assertEquals(-50.0, record.getAmount(), 0.001);
    }

    @Test
    public void testLargeAmount() {
        // 测试大金额
        record.setAmount(999999999.99);
        assertEquals(999999999.99, record.getAmount(), 0.001);
    }
}

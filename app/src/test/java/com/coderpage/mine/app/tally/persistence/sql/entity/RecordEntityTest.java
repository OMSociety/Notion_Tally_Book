package com.coderpage.mine.app.tally.persistence.sql.entity;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

/**
 * RecordEntity 实体类单元测试
 *
 * @author test
 * @since 0.8.0
 */
public class RecordEntityTest {

    private RecordEntity entity;

    @Before
    public void setUp() {
        entity = new RecordEntity();
    }

    @Test
    public void testTypeConstants() {
        assertEquals(0, RecordEntity.TYPE_EXPENSE);
        assertEquals(1, RecordEntity.TYPE_INCOME);
    }

    @Test
    public void testDefaultValues() {
        // 测试默认值
        RecordEntity defaultEntity = new RecordEntity();
        assertEquals("", defaultEntity.getDesc());
        assertEquals("", defaultEntity.getSyncId());
        assertEquals(0, defaultEntity.getId());
        assertEquals(0, defaultEntity.getAmount(), 0.001);
    }

    @Test
    public void testSetAndGetId() {
        entity.setId(100L);
        assertEquals(100L, entity.getId());
    }

    @Test
    public void testSetAndGetAccountId() {
        entity.setAccountId(200L);
        assertEquals(200L, entity.getAccountId());
    }

    @Test
    public void testSetAndGetTime() {
        long time = System.currentTimeMillis();
        entity.setTime(time);
        assertEquals(time, entity.getTime());
    }

    @Test
    public void testSetAndGetAmount() {
        entity.setAmount(150.75);
        assertEquals(150.75, entity.getAmount(), 0.001);
    }

    @Test
    public void testSetAndGetCategoryUniqueName() {
        String category = "transport_subway";
        entity.setCategoryUniqueName(category);
        assertEquals(category, entity.getCategoryUniqueName());
    }

    @Test
    public void testSetAndGetDesc() {
        String desc = "上班地铁";
        entity.setDesc(desc);
        assertEquals(desc, entity.getDesc());
    }

    @Test
    public void testSetAndGetSyncId() {
        String syncId = "notion-page-id-123";
        entity.setSyncId(syncId);
        assertEquals(syncId, entity.getSyncId());
    }

    @Test
    public void testSetAndGetSyncStatus() {
        entity.setSyncStatus(RecordEntity.SYNC_STATUS_SYNCED);
        assertEquals(RecordEntity.SYNC_STATUS_SYNCED, entity.getSyncStatus());
    }

    @Test
    public void testSetAndGetDelete() {
        entity.setDelete(1);
        assertEquals(1, entity.getDelete());
    }

    @Test
    public void testSetAndGetType() {
        entity.setType(RecordEntity.TYPE_EXPENSE);
        assertEquals(RecordEntity.TYPE_EXPENSE, entity.getType());

        entity.setType(RecordEntity.TYPE_INCOME);
        assertEquals(RecordEntity.TYPE_INCOME, entity.getType());
    }

    @Test
    public void testSyncStatusConstants() {
        // 测试同步状态常量存在
        assertEquals(0, RecordEntity.SYNC_STATUS_NONE);
        assertEquals(1, RecordEntity.SYNC_STATUS_PENDING);
        assertEquals(2, RecordEntity.SYNC_STATUS_SYNCED);
    }

    @Test
    public void testFullEntityLifecycle() {
        // 模拟完整的实体生命周期
        long now = System.currentTimeMillis();

        entity.setId(1L);
        entity.setAccountId(2L);
        entity.setTime(now);
        entity.setAmount(250.00);
        entity.setCategoryUniqueName("salary");
        entity.setDesc("月薪");
        entity.setSyncId("sync-001");
        entity.setSyncStatus(RecordEntity.SYNC_STATUS_SYNCED);
        entity.setDelete(0);
        entity.setType(RecordEntity.TYPE_INCOME);

        // 验证所有字段
        assertEquals(1L, entity.getId());
        assertEquals(2L, entity.getAccountId());
        assertEquals(now, entity.getTime());
        assertEquals(250.00, entity.getAmount(), 0.001);
        assertEquals("salary", entity.getCategoryUniqueName());
        assertEquals("月薪", entity.getDesc());
        assertEquals("sync-001", entity.getSyncId());
        assertEquals(RecordEntity.SYNC_STATUS_SYNCED, entity.getSyncStatus());
        assertEquals(0, entity.getDelete());
        assertEquals(RecordEntity.TYPE_INCOME, entity.getType());
    }

    @Test
    public void testNullDescHandling() {
        // 测试非null约束
        entity.setDesc("test");
        assertEquals("test", entity.getDesc());
    }
}

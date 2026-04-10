package com.coderpage.mine.persistence.entity;

import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * TallyRecord 组合模式单元测试
 * 
 * 覆盖：工厂方法、字段代理、Notion字段映射、边界条件
 * 
 * @author abner-l
 * @since 0.7.5
 */
public class TallyRecordTest {

    private RecordEntity baseEntity;

    @Before
    public void setUp() {
        baseEntity = new RecordEntity();
        baseEntity.setId(42L);
        baseEntity.setAccountId(1L);
        baseEntity.setTime(1712000000000L);
        baseEntity.setAmount(88.88);
        baseEntity.setCategoryUniqueName("CanYin");
        baseEntity.setDesc("午饭");
        baseEntity.setSyncId("page-abc123");
        baseEntity.setSyncStatus(1);
        baseEntity.setType(0);
    }

    // ==================== 工厂方法 ====================

    @Test
    public void fromEntity_basicMapping() {
        TallyRecord r = TallyRecord.fromEntity(baseEntity);
        assertNotNull(r);
        assertEquals(42L, r.getId());
        assertEquals(88.88, r.getAmount(), 0.001);
        assertEquals("CanYin", r.getCategory());
        assertEquals("午饭", r.getRemark());
        assertEquals("page-abc123", r.getNotionId());
        assertEquals(0, r.getType());
    }

    @Test
    public void fromEntity_null_returnsNull() {
        assertNull(TallyRecord.fromEntity(null));
    }

    @Test
    public void create_emptyRecord() {
        TallyRecord r = TallyRecord.create();
        assertNotNull(r);
        assertEquals("", r.getNotionId());
        assertEquals(0, r.getId());
        assertEquals(0.0, r.getAmount(), 0.001);
        assertFalse(r.isSynced());
    }

    // ==================== 组合模式代理 ====================

    @Test
    public void getCategory_proxiesToEntity() {
        TallyRecord r = TallyRecord.fromEntity(baseEntity);
        assertEquals("CanYin", r.getCategory());
        assertEquals("CanYin", r.getCategoryUniqueName());
    }

    @Test
    public void getRemark_proxiesToDesc() {
        TallyRecord r = TallyRecord.fromEntity(baseEntity);
        assertEquals("午饭", r.getRemark());
        assertEquals("午饭", r.getDesc());
    }

    @Test
    public void isSynced_checksSyncStatus() {
        TallyRecord r = TallyRecord.fromEntity(baseEntity);
        assertTrue(r.isSynced());
        assertEquals(1, r.getSyncStatus());
        
        baseEntity.setSyncStatus(0);
        assertFalse(r.isSynced());
    }

    // ==================== Notion 字段映射 ====================

    @Test
    public void notionId_syncIdMapping() {
        TallyRecord r = TallyRecord.fromEntity(baseEntity);
        // notionId 在构造时从 entity.getSyncId() 初始化
        assertEquals("page-abc123", r.getNotionId());
        assertEquals("page-abc123", r.getSyncId());
    }

    @Test
    public void setNotionId_updatesLocalField() {
        TallyRecord r = TallyRecord.create();
        r.setNotionId("new-page-id");
        assertEquals("new-page-id", r.getNotionId());
    }

    @Test
    public void lastModified_independentFromEntityTime() {
        TallyRecord r = TallyRecord.fromEntity(baseEntity);
        // 构造时 lastModified = entity.getTime()
        assertEquals(1712000000000L, r.getLastModified());
        
        // 但修改 lastModified 不影响 entity.time
        r.setLastModified(9999L);
        assertEquals(9999L, r.getLastModified());
        assertEquals(1712000000000L, r.getTime());
    }

    // ==================== setter 边界 ====================

    @Test
    public void setCategory_null_becomesOther() {
        TallyRecord r = TallyRecord.create();
        r.setCategory(null);
        assertEquals("other", r.getCategory());
    }

    @Test
    public void setRemark_null_becomesEmpty() {
        TallyRecord r = TallyRecord.create();
        r.setRemark(null);
        assertEquals("", r.getRemark());
    }

    @Test
    public void setSynced_trueFalse() {
        TallyRecord r = TallyRecord.create();
        r.setSynced(true);
        assertEquals(1, r.getSyncStatus());
        assertTrue(r.isSynced());
        
        r.setSynced(false);
        assertEquals(0, r.getSyncStatus());
        assertFalse(r.isSynced());
    }

    // ==================== toEntity 转换 ====================

    @Test
    public void toEntity_syncsBackNotionId() {
        TallyRecord r = TallyRecord.create();
        r.setNotionId("page-xyz");
        r.setLastModified(12345L);
        
        RecordEntity e = r.toEntity();
        assertEquals("page-xyz", e.getSyncId());
        assertEquals(12345L, e.getTime());
    }

    @Test
    public void toEntity_nullNotionId_becomesEmpty() {
        TallyRecord r = TallyRecord.create();
        // notionId 初始为 ""
        RecordEntity e = r.toEntity();
        assertEquals("", e.getSyncId());
    }

    // ==================== 便捷代理方法 ====================

    @Test
    public void proxy_getSetId() {
        TallyRecord r = TallyRecord.create();
        r.setId(99L);
        assertEquals(99L, r.getId());
    }

    @Test
    public void proxy_getSetAmount() {
        TallyRecord r = TallyRecord.create();
        r.setAmount(123.45);
        assertEquals(123.45, r.getAmount(), 0.001);
    }

    @Test
    public void proxy_getSetType() {
        TallyRecord r = TallyRecord.create();
        r.setType(1);
        assertEquals(1, r.getType());
    }

    @Test
    public void proxy_getSetTime() {
        TallyRecord r = TallyRecord.create();
        r.setTime(1700000000000L);
        assertEquals(1700000000000L, r.getTime());
    }

    @Test
    public void proxy_getSetAccountId() {
        TallyRecord r = TallyRecord.create();
        r.setAccountId(5L);
        assertEquals(5L, r.getAccountId());
    }

    @Test
    public void getEntity_returnsUnderlyingEntity() {
        TallyRecord r = TallyRecord.fromEntity(baseEntity);
        assertSame(baseEntity, r.getEntity());
    }

    @Test
    public void constructor_nullEntity_throwsException() {
        try {
            new TallyRecord(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("entity cannot be null", e.getMessage());
        }
    }

    @Test
    public void getSyncIdLegacy_deprecatedButWorks() {
        TallyRecord r = TallyRecord.fromEntity(baseEntity);
        @SuppressWarnings("deprecation")
        String legacy = r.getSyncIdLegacy();
        assertEquals("page-abc123", legacy);
    }
}

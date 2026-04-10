package com.coderpage.mine.app.tally.sync;

import com.coderpage.mine.app.tally.persistence.sql.dao.RecordDao;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;
import com.coderpage.mine.persistence.entity.TallyRecord;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * SyncRepository 单元测试
 * 
 * 通过 Mockito 模拟 RecordDao，测试同步流程逻辑：
 * - queryUnsyncedRecords: 过滤未同步记录
 * - findByNotionId:        根据 Notion ID 查找
 * - markAsSynced:           标记同步状态
 * - insertFromNotion:       从 Notion 插入
 * - updateFromNotion:       从 Notion 更新
 * 
 * @author abner-l
 * @since 0.7.5
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncRepositoryTest {

    @Mock
    private RecordDao mockRecordDao;

    private SyncRepository syncRepository;

    @Before
    public void setUp() {
        // 使用反射注入 mock DAO（绕过 TallyDatabase 单例）
        syncRepository = new SyncRepository();
        try {
            java.lang.reflect.Field daoField = SyncRepository.class.getDeclaredField("recordDao");
            daoField.setAccessible(true);
            daoField.set(syncRepository, mockRecordDao);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== queryUnsyncedRecords ====================

    @Test
    public void queryUnsyncedRecords_filtersUnsyncedOnly() {
        RecordEntity synced = createEntity(1, 0, "page-synced");
        RecordEntity unsynced1 = createEntity(2, 1, null);
        RecordEntity unsynced2 = createEntity(3, 0, null);

        when(mockRecordDao.queryAll())
                .thenReturn(Arrays.asList(synced, unsynced1, unsynced2));

        List<TallyRecord> result = syncRepository.queryUnsyncedRecords();

        assertEquals(2, result.size());
        assertFalse(result.get(0).isSynced());
        assertFalse(result.get(1).isSynced());
    }

    @Test
    public void queryUnsyncedRecords_empty_returnsEmptyList() {
        when(mockRecordDao.queryAll()).thenReturn(Collections.emptyList());

        List<TallyRecord> result = syncRepository.queryUnsyncedRecords();

        assertTrue(result.isEmpty());
    }

    @Test
    public void queryUnsyncedRecords_nullResult_returnsEmptyList() {
        when(mockRecordDao.queryAll()).thenReturn(null);

        List<TallyRecord> result = syncRepository.queryUnsyncedRecords();

        assertTrue(result.isEmpty());
    }

    @Test
    public void queryUnsyncedRecords_allSynced_returnsEmptyList() {
        RecordEntity e1 = createEntity(1, 1, "page-1");
        RecordEntity e2 = createEntity(2, 1, "page-2");
        when(mockRecordDao.queryAll()).thenReturn(Arrays.asList(e1, e2));

        List<TallyRecord> result = syncRepository.queryUnsyncedRecords();

        assertTrue(result.isEmpty());
    }

    // ==================== findByNotionId ====================

    @Test
    public void findByNotionId_found() {
        RecordEntity e = createEntity(42, 1, "page-found");
        when(mockRecordDao.queryAll()).thenReturn(Collections.singletonList(e));

        TallyRecord result = syncRepository.findByNotionId("page-found");

        assertNotNull(result);
        assertEquals(42L, result.getId());
        assertEquals("page-found", result.getNotionId());
    }

    @Test
    public void findByNotionId_notFound_returnsNull() {
        when(mockRecordDao.queryAll()).thenReturn(Collections.emptyList());

        TallyRecord result = syncRepository.findByNotionId("non-existent");

        assertNull(result);
    }

    @Test
    public void findByNotionId_nullId_returnsNull() {
        TallyRecord result = syncRepository.findByNotionId(null);
        assertNull(result);
        verify(mockRecordDao, never()).queryAll();
    }

    @Test
    public void findByNotionId_emptyId_returnsNull() {
        TallyRecord result = syncRepository.findByNotionId("");
        assertNull(result);
        verify(mockRecordDao, never()).queryAll();
    }

    // ==================== markAsSynced ====================

    @Test
    public void markAsSynced_updatesEntity() {
        RecordEntity entity = createEntity(99, 0, null);
        when(mockRecordDao.queryById(99L)).thenReturn(entity);

        syncRepository.markAsSynced(99L, "new-notion-id");

        ArgumentCaptor<RecordEntity> captor = ArgumentCaptor.forClass(RecordEntity.class);
        verify(mockRecordDao).update(captor.capture());

        RecordEntity updated = captor.getValue();
        assertEquals("new-notion-id", updated.getSyncId());
        assertEquals(1, updated.getSyncStatus());
    }

    @Test
    public void markAsSynced_notFound_doesNotCrash() {
        when(mockRecordDao.queryById(999L)).thenReturn(null);

        // 不应抛异常
        syncRepository.markAsSynced(999L, "some-id");
        verify(mockRecordDao, never()).update(any());
    }

    // ==================== insertFromNotion ====================

    @Test
    public void insertFromNotion_setsSyncStatusAndInserts() {
        TallyRecord record = TallyRecord.create();
        record.setAmount(100.0);
        record.setCategory("餐饮");
        record.setTime(1712000000000L);
        record.setType(0);
        record.setNotionId("notion-insert-123");

        when(mockRecordDao.insert(any(RecordEntity.class))).thenReturn(55L);

        long insertedId = syncRepository.insertFromNotion(record);

        assertEquals(55L, insertedId);

        ArgumentCaptor<RecordEntity> captor = ArgumentCaptor.forClass(RecordEntity.class);
        verify(mockRecordDao).insert(captor.capture());

        RecordEntity saved = captor.getValue();
        assertEquals(1, saved.getSyncStatus()); // 插入后自动标记已同步
    }

    // ==================== updateFromNotion ====================

    @Test
    public void updateFromNotion_updatesMatchingRecord() {
        // 找到现有记录
        RecordEntity existing = createEntity(7, 0, "page-update");
        when(mockRecordDao.queryAll()).thenReturn(Collections.singletonList(existing));
        when(mockRecordDao.queryById(7L)).thenReturn(existing);

        // Notion 侧的新数据
        TallyRecord notionRecord = TallyRecord.create();
        notionRecord.setNotionId("page-update");
        notionRecord.setAmount(200.0);
        notionRecord.setCategory("交通");
        notionRecord.setRemark("打车");
        notionRecord.setTime(1720000000000L);
        notionRecord.setType(0);

        syncRepository.updateFromNotion(notionRecord);

        ArgumentCaptor<RecordEntity> captor = ArgumentCaptor.forClass(RecordEntity.class);
        verify(mockRecordDao).update(captor.capture());

        RecordEntity updated = captor.getValue();
        assertEquals(200.0, updated.getAmount(), 0.001);
        assertEquals("交通", updated.getCategoryUniqueName());
        assertEquals("打车", updated.getDesc());
        assertEquals(1, updated.getSyncStatus());
    }

    @Test
    public void updateFromNotion_notFound_doesNotCrash() {
        when(mockRecordDao.queryAll()).thenReturn(Collections.emptyList());

        TallyRecord r = TallyRecord.create();
        r.setNotionId("non-existent");
        r.setAmount(1.0);

        // 不应抛异常
        syncRepository.updateFromNotion(r);
        verify(mockRecordDao, never()).update(any());
    }

    // ==================== queryAll ====================

    @Test
    public void queryAll_returnsAllRecordsAsTallyRecord() {
        RecordEntity e1 = createEntity(1, 1, "page-1");
        RecordEntity e2 = createEntity(2, 0, "page-2");
        RecordEntity e3 = createEntity(3, 1, "page-3");
        when(mockRecordDao.queryAll()).thenReturn(Arrays.asList(e1, e2, e3));

        List<TallyRecord> result = syncRepository.queryAll();

        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals(3L, result.get(2).getId());
    }

    @Test
    public void queryAll_nullResult_returnsEmptyList() {
        when(mockRecordDao.queryAll()).thenReturn(null);

        List<TallyRecord> result = syncRepository.queryAll();

        assertTrue(result.isEmpty());
    }

    // ==================== 辅助方法 ====================

    private RecordEntity createEntity(long id, int syncStatus, String syncId) {
        RecordEntity e = new RecordEntity();
        e.setId(id);
        e.setSyncStatus(syncStatus);
        e.setSyncId(syncId != null ? syncId : "");
        e.setAmount(10.0);
        e.setTime(System.currentTimeMillis());
        e.setCategoryUniqueName("Other");
        e.setDesc("");
        e.setType(0);
        e.setAccountId(1L);
        return e;
    }
}

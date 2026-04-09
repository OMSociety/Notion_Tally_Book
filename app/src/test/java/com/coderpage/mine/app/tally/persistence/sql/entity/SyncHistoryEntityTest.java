package com.coderpage.mine.app.tally.persistence.sql.entity;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

/**
 * SyncHistoryEntity 同步历史记录实体测试
 *
 * @author test
 * @since 0.8.0
 */
public class SyncHistoryEntityTest {

    private SyncHistoryEntity entity;

    @Before
    public void setUp() {
        entity = new SyncHistoryEntity();
    }

    @Test
    public void testDefaultConstructor() {
        SyncHistoryEntity newEntity = new SyncHistoryEntity();
        assertNotNull(newEntity);
    }

    @Test
    public void testSetAndGetId() {
        entity.setId(1L);
        assertEquals(1L, entity.getId());
    }

    @Test
    public void testSetAndGetStartTime() {
        long startTime = System.currentTimeMillis();
        entity.setStartTime(startTime);
        assertEquals(startTime, entity.getStartTime());
    }

    @Test
    public void testSetAndGetEndTime() {
        long endTime = System.currentTimeMillis();
        entity.setEndTime(endTime);
        assertEquals(endTime, entity.getEndTime());
    }

    @Test
    public void testSetAndGetResult() {
        entity.setResult(SyncHistoryEntity.RESULT_SUCCESS);
        assertEquals(SyncHistoryEntity.RESULT_SUCCESS, entity.getResult());
    }

    @Test
    public void testSetAndGetSuccessCount() {
        entity.setSuccessCount(10);
        assertEquals(10, entity.getSuccessCount());
    }

    @Test
    public void testSetAndGetFailCount() {
        entity.setFailCount(2);
        assertEquals(2, entity.getFailCount());
    }

    @Test
    public void testSetAndGetFailReason() {
        String reason = "Network timeout";
        entity.setFailReason(reason);
        assertEquals(reason, entity.getFailReason());
    }

    @Test
    public void testResultConstants() {
        assertEquals(0, SyncHistoryEntity.RESULT_SUCCESS);
        assertEquals(1, SyncHistoryEntity.RESULT_FAIL);
        assertEquals(2, SyncHistoryEntity.RESULT_PARTIAL);
    }

    @Test
    public void testFullSyncHistoryLifecycle() {
        long startTime = System.currentTimeMillis() - 10000; // 10秒前开始
        long endTime = System.currentTimeMillis();

        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        entity.setResult(SyncHistoryEntity.RESULT_SUCCESS);
        entity.setSuccessCount(15);
        entity.setFailCount(0);
        entity.setFailReason("");

        assertEquals(startTime, entity.getStartTime());
        assertEquals(endTime, entity.getEndTime());
        assertEquals(SyncHistoryEntity.RESULT_SUCCESS, entity.getResult());
        assertEquals(15, entity.getSuccessCount());
        assertEquals(0, entity.getFailCount());
    }

    @Test
    public void testPartialFailure() {
        entity.setResult(SyncHistoryEntity.RESULT_PARTIAL);
        entity.setSuccessCount(8);
        entity.setFailCount(3);
        entity.setFailReason("部分记录同步失败: 网络超时");

        assertEquals(SyncHistoryEntity.RESULT_PARTIAL, entity.getResult());
        assertEquals(8, entity.getSuccessCount());
        assertEquals(3, entity.getFailCount());
        assertEquals("部分记录同步失败: 网络超时", entity.getFailReason());
    }

    @Test
    public void testSyncDuration() {
        long startTime = System.currentTimeMillis() - 5000;
        long endTime = System.currentTimeMillis();

        entity.setStartTime(startTime);
        entity.setEndTime(endTime);

        long duration = entity.getEndTime() - entity.getStartTime();
        assertTrue(duration >= 5000);
        assertTrue(duration <= 6000);
    }
}

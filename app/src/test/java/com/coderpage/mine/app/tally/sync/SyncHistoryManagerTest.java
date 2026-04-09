package com.coderpage.mine.app.tally.sync;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.dao.SyncHistoryDao;
import com.coderpage.mine.app.tally.persistence.sql.entity.SyncHistoryEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * SyncHistoryManager 单元测试
 *
 * @author test
 * @since 0.8.0
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncHistoryManagerTest {

    @Mock
    private SyncHistoryDao mockDao;

    @Mock
    private TallyDatabase mockDatabase;

    private SyncHistoryManager manager;

    @Before
    public void setUp() {
        // 由于 SyncHistoryManager 直接依赖 TallyDatabase.getInstance()
        // 这里我们测试静态方法 formatHistory
        // 其他方法需要依赖注入改造才能测试
    }

    @Test
    public void testFormatHistorySuccess() {
        SyncHistoryEntity history = new SyncHistoryEntity();
        history.setSyncDirection(SyncHistoryEntity.DIRECTION_UPLOAD);
        history.setStatus(1);
        history.setUploaded(5);
        history.setPulled(3);

        String result = SyncHistoryManager.formatHistory(history);
        assertTrue(result.contains("上传"));
        assertTrue(result.contains("5"));
        assertTrue(result.contains("3"));
    }

    @Test
    public void testFormatHistoryFailed() {
        SyncHistoryEntity history = new SyncHistoryEntity();
        history.setSyncDirection(SyncHistoryEntity.DIRECTION_DOWNLOAD);
        history.setStatus(2);
        history.setErrorMessage("网络连接失败");

        String result = SyncHistoryManager.formatHistory(history);
        assertTrue(result.contains("下载"));
        assertTrue(result.contains("网络连接失败"));
    }

    @Test
    public void testFormatHistoryNoChange() {
        SyncHistoryEntity history = new SyncHistoryEntity();
        history.setSyncDirection(SyncHistoryEntity.DIRECTION_UPLOAD);
        history.setStatus(1);
        history.setUploaded(0);
        history.setPulled(0);

        String result = SyncHistoryManager.formatHistory(history);
        assertTrue(result.contains("无数据变动"));
    }

    @Test
    public void testDirectionConstants() {
        assertEquals(0, SyncHistoryEntity.DIRECTION_UPLOAD);
        assertEquals(1, SyncHistoryEntity.DIRECTION_DOWNLOAD);
        assertEquals(2, SyncHistoryEntity.DIRECTION_BIDIRECTIONAL);
    }

    @Test
    public void testStatusConstants() {
        assertEquals(0, SyncHistoryEntity.STATUS_IDLE);
        assertEquals(1, SyncHistoryEntity.STATUS_SUCCESS);
        assertEquals(2, SyncHistoryEntity.STATUS_FAILED);
    }

    @Test
    public void testDirectionText() {
        SyncHistoryEntity entity = new SyncHistoryEntity();
        
        entity.setSyncDirection(SyncHistoryEntity.DIRECTION_UPLOAD);
        assertEquals("上传", entity.getDirectionText());

        entity.setSyncDirection(SyncHistoryEntity.DIRECTION_DOWNLOAD);
        assertEquals("下载", entity.getDirectionText());

        entity.setSyncDirection(SyncHistoryEntity.DIRECTION_BIDIRECTIONAL);
        assertEquals("双向", entity.getDirectionText());
    }

    @Test
    public void testStatusText() {
        SyncHistoryEntity entity = new SyncHistoryEntity();
        
        entity.setStatus(SyncHistoryEntity.STATUS_IDLE);
        assertEquals("空闲", entity.getStatusText());

        entity.setStatus(SyncHistoryEntity.STATUS_SUCCESS);
        assertEquals("成功", entity.getStatusText());

        entity.setStatus(SyncHistoryEntity.STATUS_FAILED);
        assertEquals("失败", entity.getStatusText());
    }
}

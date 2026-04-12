package com.coderpage.mine.app.tally.sync;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ConflictServiceTest {

    @Test
    public void findRemoteOnlyRecords_shouldReturnOnlyRemoteExclusiveRecords() {
        ConflictService service = new ConflictService();

        ConflictResolver.Record local = new ConflictResolver.Record();
        local.notionPageId = "p1";

        ConflictResolver.Record remoteSame = new ConflictResolver.Record();
        remoteSame.notionPageId = "p1";
        ConflictResolver.Record remoteOnly = new ConflictResolver.Record();
        remoteOnly.notionPageId = "p2";

        List<ConflictResolver.Record> remoteOnlyList =
                service.findRemoteOnlyRecords(Arrays.asList(local), Arrays.asList(remoteSame, remoteOnly));

        Assert.assertEquals(1, remoteOnlyList.size());
        Assert.assertEquals("p2", remoteOnlyList.get(0).notionPageId);
    }
}

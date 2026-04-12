package com.coderpage.mine.app.tally.sync;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SyncDiffHelperTest {

    @Test
    public void findRemoteOnlyRecords_shouldReturnRemoteRecordWhenNotInLocal() {
        ConflictResolver.Record local = new ConflictResolver.Record();
        local.notionPageId = "page_local_1";

        ConflictResolver.Record remoteKeep = new ConflictResolver.Record();
        remoteKeep.notionPageId = "page_local_1";

        ConflictResolver.Record remoteOnly = new ConflictResolver.Record();
        remoteOnly.notionPageId = "page_remote_only";

        List<ConflictResolver.Record> result = SyncDiffHelper.findRemoteOnlyRecords(
                Arrays.asList(local),
                Arrays.asList(remoteKeep, remoteOnly));

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("page_remote_only", result.get(0).notionPageId);
    }

    @Test
    public void findRemoteOnlyRecords_shouldIgnoreNullOrEmptyPageId() {
        ConflictResolver.Record remoteNull = new ConflictResolver.Record();
        remoteNull.notionPageId = null;
        ConflictResolver.Record remoteEmpty = new ConflictResolver.Record();
        remoteEmpty.notionPageId = "";

        List<ConflictResolver.Record> result = SyncDiffHelper.findRemoteOnlyRecords(
                new ArrayList<>(),
                Arrays.asList(remoteNull, remoteEmpty));

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void findRemoteOnlyRecords_shouldBeIdempotentWhenRemoteHasDuplicatePageId() {
        ConflictResolver.Record remote1 = new ConflictResolver.Record();
        remote1.notionPageId = "dup_page";
        ConflictResolver.Record remote2 = new ConflictResolver.Record();
        remote2.notionPageId = "dup_page";

        List<ConflictResolver.Record> result = SyncDiffHelper.findRemoteOnlyRecords(
                null,
                Arrays.asList(remote1, remote2));

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("dup_page", result.get(0).notionPageId);
    }

    @Test
    public void findRemoteOnlyRecords_shouldReturnEmptyWhenRemoteRecordsMissing() {
        List<ConflictResolver.Record> result = SyncDiffHelper.findRemoteOnlyRecords(
                Arrays.asList(new ConflictResolver.Record()),
                null);
        Assert.assertTrue(result.isEmpty());
    }
}

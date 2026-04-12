package com.coderpage.mine.app.tally.sync;

import org.junit.Assert;
import org.junit.Test;

public class ConflictResolverTest {

    @Test
    public void resolve_shouldReturnNonNullRecordWhenOneSideIsNull() {
        ConflictResolver resolver = new ConflictResolver();
        ConflictResolver.Record local = new ConflictResolver.Record();
        local.id = "1";

        Assert.assertSame(local, resolver.resolve(local, null));
        Assert.assertNull(resolver.resolve(null, null));
    }

    @Test
    public void resolve_keepNewest_shouldPreferLocalWhenSameLastModified() {
        ConflictResolver resolver = new ConflictResolver(ConflictResolver.STRATEGY_KEEP_NEWEST);
        ConflictResolver.Record local = new ConflictResolver.Record();
        local.lastModified = 100L;
        ConflictResolver.Record remote = new ConflictResolver.Record();
        remote.lastModified = 100L;

        Assert.assertSame(local, resolver.resolve(local, remote));
    }

    @Test
    public void resolve_merge_shouldNotDuplicateSameRemark() {
        ConflictResolver resolver = new ConflictResolver(ConflictResolver.STRATEGY_MERGE);
        ConflictResolver.Record local = new ConflictResolver.Record();
        local.id = "1";
        local.remark = "same";
        local.amount = 10;
        local.type = "expense";
        local.category = "food";
        local.lastModified = 200L;

        ConflictResolver.Record remote = new ConflictResolver.Record();
        remote.notionPageId = "p1";
        remote.remark = "same";
        remote.time = 100L;
        remote.lastModified = 100L;

        ConflictResolver.Record result = resolver.resolve(local, remote);
        Assert.assertEquals("same", result.remark);
        Assert.assertEquals("p1", result.notionPageId);
    }
}

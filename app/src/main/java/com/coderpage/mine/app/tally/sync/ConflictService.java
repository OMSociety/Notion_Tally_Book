package com.coderpage.mine.app.tally.sync;

import java.util.List;

/**
 * 冲突与差异判定服务
 */
public class ConflictService {

    private final ConflictResolver conflictResolver = new ConflictResolver();

    public ConflictResolver.Record resolve(ConflictResolver.Record local, ConflictResolver.Record remote) {
        return conflictResolver.resolve(local, remote);
    }

    public List<ConflictResolver.Record> findRemoteOnlyRecords(
            List<ConflictResolver.Record> localRecords,
            List<ConflictResolver.Record> remoteRecords) {
        return SyncDiffHelper.findRemoteOnlyRecords(localRecords, remoteRecords);
    }
}

package com.coderpage.mine.app.tally.sync;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 同步差异计算帮助类
 */
public class SyncDiffHelper {

    private SyncDiffHelper() {
    }

    /**
     * 查找仅存在于远程的记录（通过 notionPageId 判定）
     */
    public static List<ConflictResolver.Record> findRemoteOnlyRecords(
            List<ConflictResolver.Record> localRecords,
            List<ConflictResolver.Record> remoteRecords) {
        List<ConflictResolver.Record> remoteOnly = new ArrayList<>();
        if (remoteRecords == null || remoteRecords.isEmpty()) {
            return remoteOnly;
        }

        Set<String> localPageIds = new HashSet<>();
        if (localRecords != null) {
            for (ConflictResolver.Record local : localRecords) {
                if (local != null && local.notionPageId != null && !local.notionPageId.isEmpty()) {
                    localPageIds.add(local.notionPageId);
                }
            }
        }

        for (ConflictResolver.Record remote : remoteRecords) {
            if (remote == null || remote.notionPageId == null || remote.notionPageId.isEmpty()) {
                continue;
            }
            if (!localPageIds.contains(remote.notionPageId)) {
                remoteOnly.add(remote);
            }
        }

        return remoteOnly;
    }
}

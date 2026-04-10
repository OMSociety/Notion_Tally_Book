package com.coderpage.mine.app.tally.module.backup;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.coderpage.base.common.IError;
import com.coderpage.mine.persistence.database.MineDatabase;
import com.coderpage.mine.persistence.entity.KeyValue;

import java.io.File;

/**
 * 自动备份 Worker
 *
 * 通过 WorkManager 在后台执行自动备份任务。
 * 配置项从 MineDatabase (KeyValueDao) 读取，与 SettingViewModel 统一管理。
 *
 * @author abner-l
 * @since 0.7.5
 */
public class AutoBackupWorker extends Worker {

    private static final String TAG = "AutoBackupWorker";

    /** 自动备份开关 Key（与 SettingWorkerConst / SettingViewModel 对齐） */
    private static final String KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled";

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        if (!isAutoBackupEnabled()) {
            Log.i(TAG, "Auto backup is disabled, skipping.");
            return Result.success();
        }

        Log.i(TAG, "Starting auto backup...");
        try {
            File file = Backup.backupToJsonFileSync(getApplicationContext(),
                    new Backup.BackupProgressListener() {
                        @Override
                        public void onProgressUpdate(Backup.BackupProgress backupProgress) {
                            Log.d(TAG, "Backup progress: " + backupProgress.name());
                        }

                        @Override
                        public void success(Void aVoid) {
                            Log.i(TAG, "Auto backup completed successfully.");
                        }

                        @Override
                        public void failure(IError iError) {
                            Log.e(TAG, "Auto backup failed: " + iError.msg());
                        }
                    });

            if (file != null) {
                Log.i(TAG, "Backup file saved: " + file.getAbsolutePath());
                return Result.success();
            } else {
                Log.e(TAG, "Backup file is null, backup may have failed.");
                return Result.failure();
            }
        } catch (Exception e) {
            Log.e(TAG, "Backup exception", e);
            return Result.failure();
        }
    }

    /**
     * 从 MineDatabase (KeyValueDao) 读取自动备份开关状态
     * 与 SettingViewModel 使用相同的配置源
     */
    private boolean isAutoBackupEnabled() {
        try {
            KeyValue setting = MineDatabase.getInstance().keyValueDao().query(KEY_AUTO_BACKUP_ENABLED);
            return setting != null && "true".equalsIgnoreCase(setting.getValue());
        } catch (Exception e) {
            Log.e(TAG, "Failed to read auto backup setting", e);
            return false;
        }
    }
}

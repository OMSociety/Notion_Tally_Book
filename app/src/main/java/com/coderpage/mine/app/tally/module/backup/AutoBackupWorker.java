package com.coderpage.mine.app.tally.module.backup;// AutoBackupWorker.java

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.coderpage.base.common.IError;

import java.io.File;

public class AutoBackupWorker extends Worker {
    
    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        if (!isAutoBackupEnabled()) {
            return Result.success();
        }

        try {
            File file = Backup.backupToJsonFileSync(getApplicationContext(),
                    new Backup.BackupProgressListener() {
                        @Override
                        public void onProgressUpdate(Backup.BackupProgress backupProgress) {
                            // 可以添加日志记录
                        }

                        @Override
                        public void success(Void aVoid) {
                            Log.i("AutoBackupWorker", "Backup completed successfully");
                        }

                        @Override
                        public void failure(IError iError) {
                            Log.e("AutoBackupWorker", "Backup failed: " + iError.msg());
                        }
                    });

            return file != null ? Result.success() : Result.failure();
        } catch (Exception e) {
            Log.e("AutoBackupWorker", "Backup exception", e);
            return Result.failure();
        }
    }

    private boolean isAutoBackupEnabled() {
        android.content.SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences("backup_settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("auto_backup_enabled", false);
    }

}

package com.coderpage.mine.app.tally.module.backup;// AutoBackupWorker.java

import android.content.Context;
import android.support.annotation.NonNull;
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
                            // 备份成功
                            ((android.app.Activity) getApplicationContext()).runOnUiThread(() -> {
                                Toast.makeText(getApplicationContext(), "备份成功", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void failure(IError iError) {
                            // 记录错误日志
                            Log.e("AutoBackupWorker", "Backup failed: " + iError.msg());
                            ((android.app.Activity) getApplicationContext()).runOnUiThread(() -> {
                                Toast.makeText(getApplicationContext(), "备份失败: " + iError.msg(), Toast.LENGTH_SHORT).show();
                            });
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

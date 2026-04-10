package com.coderpage.mine.app.tally.module.backup;





import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.coderpage.base.cache.Cache;
import com.coderpage.base.common.Callback;
import com.coderpage.base.common.IError;
import com.coderpage.base.common.NonThrowError;
import com.coderpage.concurrency.AsyncTaskExecutor;
import com.coderpage.mine.app.tally.common.error.ErrorCode;
import com.coderpage.mine.app.tally.persistence.model.Record;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * @author abner-l. 2017-06-01
 * @since 0.4.0
 */

class BackupCache {

    private static final String TAG = "BackupCache";
    private static final String BACKUP_FOLDER_NAME = "backup";
    private static final String BACKUP_FILE_NAME = "backup";
    private static final String AUTOMATIC_BACKUP_FILE_NAME = "automatic_backup";

    private static String DATA_ROOT_PATH = null;

    private Context mContext;
    private File backupFolder; // 备份文件夹
    private String backupFolderPath = DATA_ROOT_PATH + File.separator + BACKUP_FOLDER_NAME;
    private SimpleDateFormat backupFileDateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    BackupCache(Context context) {
        mContext = context;
        DATA_ROOT_PATH = context.getCacheDir().getPath();
        initBackupFolder();
    }

    private synchronized boolean initBackupFolder() {
        if (backupFolder != null && backupFolder.exists()) {
            return true;
        }

        File cacheRoot = Cache.getCacheFolder(mContext);
        if (!cacheRoot.exists()) {
            return false;
        }

        backupFolderPath = cacheRoot.getPath() + File.separator + BACKUP_FOLDER_NAME;
        backupFolder = new File(backupFolderPath);
        if (backupFolder.exists()) {
            return true;
        }
        boolean mkDirOk = backupFolder.mkdir();
        if (!mkDirOk) {
            LOGE(TAG, "create backup cache folder failed");
        }
        return mkDirOk;
    }

    /**
     * 备份消费记录到 JSON 文件中
     *
     * @param backupModel 备份数据对象
     * @param callback    备份结果回调
     *
     * @return 备份文件，如果备份成功返回文件对象，否则返回 null
     */
    File backup2JsonFile(BackupModel backupModel, Callback<Void, IError> callback) {
        String fileName = formatBackupJsonFileName();
        File file = new File(backupFolderPath, fileName);
        if (!createFileIfNotExists(file)) {
            callback.failure(new NonThrowError(
                    ErrorCode.ILLEGAL_ARGS, "create backup file failed"));
            return null;
        }

        try {
            FileWriter fileWriter = new FileWriter(file);
            JSON.writeJSONStringTo(
                    backupModel,
                    fileWriter,
                    SerializerFeature.WriteNullListAsEmpty,
                    SerializerFeature.WriteNullStringAsEmpty,
                    SerializerFeature.WriteNullNumberAsZero);
            LOGI(TAG, "备份 JSON 文件成功 " + file.getAbsolutePath());
            callback.success(null);
        } catch (IOException e) {
            LOGE(TAG, "文件写入错误:", e);
            callback.failure(new NonThrowError(ErrorCode.UNKNOWN, "文件写入错误:" + e.getMessage()));
            return null;
        }

        return file;
    }

    private String formatBackupJsonFileName() {
        Calendar calendar = Calendar.getInstance();
        String dateFormatted = backupFileDateFormat.format(calendar.getTime());
        return BACKUP_FILE_NAME + "_" + dateFormatted + ".json";
    }

    private boolean createFileIfNotExists(File file) {
        if (file == null) {
            return false;
        }
        boolean createFileOk = true;

        // 确保父目录存在
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                LOGE(TAG, "创建父目录失败: " + parentDir.getAbsolutePath());
                return false;
            }
        }

        if (!file.exists()) {
            try {
                createFileOk = file.createNewFile();
            } catch (IOException e) {
                createFileOk = false;
                LOGE(TAG, "创建文件失败:" + e.getMessage());
            }

        }
        return createFileOk;
    }

    /**
     * 删除所有备份文件
     */
    public boolean deleteAllBackupFile() {
        File backupFileFolder = new File(backupFolderPath);
        if (!backupFileFolder.exists()) {
            return true;
        }
        if (backupFileFolder.isDirectory()) {
            String[] backupFileNames = backupFileFolder.list();
            for (String fileName : backupFileNames) {
                new File(backupFileFolder, fileName).delete();
            }
        }
        return true;
    }

    List<File> listBackupFiles() {
        File imageFolder = new File(backupFolderPath);
        if (!imageFolder.exists()) {
            return new ArrayList<>();
        }

        File[] files = imageFolder.listFiles();
        return files == null ? new ArrayList<>(0) : Arrays.asList(files);
    }

    /**
     * 同步备份到 JSON 文件
     *
     * @param backupModel 备份数据模型
     * @param listener    回调监听器
     * @return 备份文件
     */
    public File backup2JsonFileSync(BackupModel backupModel, Backup.BackupProgressListener listener) {
        try {
            File file = new File(backupFolderPath, AUTOMATIC_BACKUP_FILE_NAME +".json");
            if (!createFileIfNotExists(file)) {
                listener.failure(new NonThrowError(
                        ErrorCode.ILLEGAL_ARGS, "create backup file failed"));
                return null;
            }

            FileWriter fileWriter = new FileWriter(file);
            JSON.writeJSONStringTo(
                    backupModel,
                    fileWriter,
                    SerializerFeature.WriteNullListAsEmpty,
                    SerializerFeature.WriteNullStringAsEmpty,
                    SerializerFeature.WriteNullNumberAsZero);
            LOGI(TAG, "备份 JSON 文件成功 " + file.getAbsolutePath());
            listener.success(null);
            return file;
        } catch (Exception e) {
            LOGE(TAG, "文件写入错误:", e);
            listener.failure(new NonThrowError(ErrorCode.UNKNOWN, "文件写入错误:" + e.getMessage()));
            return null;
        }
    }
}

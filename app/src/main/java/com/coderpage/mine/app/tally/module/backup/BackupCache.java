package com.coderpage.mine.app.tally.module.backup;

import static com.coderpage.base.utils.LogUtils.LOGE;
import static com.coderpage.base.utils.LogUtils.LOGI;
import static com.coderpage.base.utils.LogUtils.makeLogTag;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.coderpage.base.cache.Cache;
import com.coderpage.base.common.Callback;
import com.coderpage.base.common.IError;
import com.coderpage.base.common.NonThrowError;
import com.coderpage.base.common.SimpleCallback;
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

    private static final String TAG = makeLogTag(BackupCache.class);
    private static final String BACKUP_FOLDER_NAME = "backup";
    private static final String BACKUP_FILE_NAME = "backup";
    private static final String AUTOMATIC_BACKUP_FILE_NAME = "automatic_backup";

    private static final ThreadLocal<SimpleDateFormat> BACKUP_DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()));

    private static String DATA_ROOT_PATH = null;

    private Context mContext;
    private volatile File backupFolder; // 备份文件夹
    private volatile String backupFolderPath;

    BackupCache(Context context) {
        mContext = context;
        DATA_ROOT_PATH = context.getCacheDir().getPath();
        File cacheRoot = Cache.getCacheFolder(context);
        backupFolderPath = cacheRoot.getPath() + File.separator + BACKUP_FOLDER_NAME;
        backupFolder = new File(backupFolderPath);
        initBackupFolder();
    }

    private synchronized boolean initBackupFolder() {
        if (backupFolder.exists()) {
            return true;
        }

        File cacheRoot = Cache.getCacheFolder(mContext);
        if (!cacheRoot.exists()) {
            return false;
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
    synchronized File backup2JsonFile(BackupModel backupModel, Callback<Void, IError> callback) {
        String fileName = formatBackupJsonFileName();
        File file = new File(backupFolderPath, fileName);
        if (!createFileIfNotExists(file)) {
            ((SimpleCallback<Void>) callback).failure(new NonThrowError(
                    ErrorCode.ILLEGAL_ARGS, "create backup file failed"));
            return null;
        }

        try (FileWriter fileWriter = new FileWriter(file)) {
            JSON.writeJSONStringTo(
                    backupModel,
                    fileWriter,
                    SerializerFeature.WriteNullListAsEmpty,
                    SerializerFeature.WriteNullStringAsEmpty,
                    SerializerFeature.WriteNullNumberAsZero);
            fileWriter.flush();
            LOGI(TAG, "备份 JSON 文件成功 " + file.getAbsolutePath());
            callback.success(null);
        } catch (IOException e) {
            LOGE(TAG, "文件写入错误:", e);
            ((SimpleCallback<Void>) callback).failure(new NonThrowError(ErrorCode.UNKNOWN, "文件写入错误:" + e.getMessage()));
            return null;
        }

        return file;
    }

    private String formatBackupJsonFileName() {
        Calendar calendar = Calendar.getInstance();
        String dateFormatted = BACKUP_DATE_FORMAT.get().format(calendar.getTime());
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
    public synchronized boolean deleteAllBackupFile() {
        File backupFileFolder = new File(backupFolderPath);
        if (!backupFileFolder.exists()) {
            return true;
        }
        if (backupFileFolder.isDirectory()) {
            String[] backupFileNames = backupFileFolder.list();
            if (backupFileNames == null) return true;
            for (String fileName : backupFileNames) {
                new File(backupFileFolder, fileName).delete();
            }
        }
        return true;
    }

    synchronized List<File> listBackupFiles() {
        initBackupFolder();
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
    public synchronized File backup2JsonFileSync(BackupModel backupModel, Backup.BackupProgressListener listener) {
        try {
            File file = new File(backupFolderPath, AUTOMATIC_BACKUP_FILE_NAME +".json");
            if (!createFileIfNotExists(file)) {
                ((SimpleCallback<Void>) listener).failure(new NonThrowError(
                        ErrorCode.ILLEGAL_ARGS, "create backup file failed"));
                return null;
            }

            try (FileWriter fileWriter = new FileWriter(file)) {
                JSON.writeJSONStringTo(
                        backupModel,
                        fileWriter,
                        SerializerFeature.WriteNullListAsEmpty,
                        SerializerFeature.WriteNullStringAsEmpty,
                        SerializerFeature.WriteNullNumberAsZero);
                fileWriter.flush();
            }
            LOGI(TAG, "备份 JSON 文件成功 " + file.getAbsolutePath());
            listener.success(null);
            return file;
        } catch (Exception e) {
            LOGE(TAG, "文件写入错误:", e);
            ((SimpleCallback<Void>) listener).failure(new NonThrowError(ErrorCode.UNKNOWN, "文件写入错误:" + e.getMessage()));
            return null;
        }
    }
}

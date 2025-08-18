package com.coderpage.mine.app.tally.module.backup;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.coderpage.base.common.IError;
import com.coderpage.base.common.SimpleCallback;
import com.coderpage.base.utils.FileUtils;
import com.coderpage.base.utils.ResUtils;
import com.coderpage.framework.BaseViewModel;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.permission.PermissionReqHandler;
import com.tendcloud.tenddata.TCAgent;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author lc. 2019-05-19 23:51
 * @since 0.6.2
 */


public class BackupFileViewModel extends BaseViewModel {

    /** 处理加载信息 */
    private MutableLiveData<String> mProcessMessage = new MutableLiveData<>();

    private PermissionReqHandler mPermissionReqHandler;

    // 自动备份状态
    private MutableLiveData<Boolean> mAutoBackupEnabled = new MutableLiveData<>();

    // SharedPreferences文件名
    private static final String PREF_NAME = "backup_settings";
    private static final String KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled";


    public BackupFileViewModel(Application application) {
        super(application);
        // 初始化自动备份状态
        initAutoBackupStatus();
    }

    /**
     * 获取自动备份状态
     */
    public LiveData<Boolean> getAutoBackupEnabled() {
        return mAutoBackupEnabled;
    }

    /**
     * 初始化自动备份状态
     */
    private void initAutoBackupStatus() {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false); // 默认关闭

        // 检查是否真正存在任务
        if (enabled) {
            try {
                List<WorkInfo> workInfos = WorkManager.getInstance()
                        .getWorkInfosByTag(WorkerConst.UNIQUE_NAME_AUTO_BACKUP_WORKER)
                        .get();
                // 如果没有找到任务，则更新状态为关闭
                if (workInfos.isEmpty()) {
                    enabled = false;
                    // 同时更新 SharedPreferences 中的状态
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(KEY_AUTO_BACKUP_ENABLED, false);
                    editor.apply();
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        mAutoBackupEnabled.setValue(enabled);
    }
    /**
     * 切换自动备份状态
     */
    public void onAutoBackupStatusClick() {
        Boolean currentStatus = mAutoBackupEnabled.getValue();
        if (currentStatus == null) {
            currentStatus = false;
        }

        boolean newStatus = !currentStatus;
        mAutoBackupEnabled.setValue(newStatus);
        saveAutoBackupStatus(newStatus);

        if (newStatus) {
            // 启用自动备份 - 安排每日备份
            schedulePeriodicBackup();
        } else {
            // 禁用自动备份 - 取消所有相关任务
            WorkManager.getInstance()
                    .cancelAllWorkByTag(WorkerConst.UNIQUE_NAME_AUTO_BACKUP_WORKER);
        }
    }
    /**
     * 安排周期性备份任务
     */
    private void schedulePeriodicBackup() {
        PeriodicWorkRequest periodicRequest = new PeriodicWorkRequest.Builder(
                AutoBackupWorker.class, 24, TimeUnit.HOURS)
                .addTag(WorkerConst.UNIQUE_NAME_AUTO_BACKUP_WORKER)
                .build();

        WorkManager.getInstance().enqueueUniquePeriodicWork(
                WorkerConst.UNIQUE_NAME_AUTO_BACKUP_WORKER,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest);
    }
    /**
     * 保存自动备份状态到SharedPreferences
     *
     * @param enabled 是否启用自动备份
     */
    private void saveAutoBackupStatus(boolean enabled) {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled);
        editor.apply();
    }

    LiveData<String> getProcessMessage() {
        return mProcessMessage;
    }

    void onMenuManagerClick(Activity activity) {
        BackupFileManagerActivity.open(activity);
    }

    /** 导出数据点击 */
    public void onExportDataClick(Activity activity) {
        String[] permissionArray = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if (mPermissionReqHandler == null) {
            mPermissionReqHandler = new PermissionReqHandler(activity);
        }
        mPermissionReqHandler.requestPermission(false, permissionArray, new PermissionReqHandler.Listener() {
            @Override
            public void onGranted(boolean grantedAll, String[] permissionArray) {
                backup2JsonFile();
            }

            @Override
            public void onDenied(String[] permissionArray) {
                showToastShort(R.string.permission_request_failed_write_external_storage);
            }
        });
    }

    /** 导入数据点击 */
    public void onImportDataClick(Activity activity) {
        String[] permissionArray = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if (mPermissionReqHandler == null) {
            mPermissionReqHandler = new PermissionReqHandler(activity);
        }
        mPermissionReqHandler.requestPermission(false, permissionArray, new PermissionReqHandler.Listener() {
            @Override
            public void onGranted(boolean grantedAll, String[] permissionArray) {
                showBackupFileSelectDialog(activity);
            }

            @Override
            public void onDenied(String[] permissionArray) {
                showToastShort(R.string.permission_request_failed_read_external_storage);
            }
        });
    }

    /**
     * 处理从文件管理器选择的备份文件。
     *
     * @param activity activity
     * @param filePath 文件路径。
     */
    private void onBackupFileSelectedFromFileSystem(Activity activity, String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            showToastShort(R.string.tally_toast_illegal_path);
            return;
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            showToastShort(R.string.tally_toast_illegal_path);
            return;
        }
        readDataFromBackupJsonFile(filePath, backupModel -> showRestoreDataConfirmDialog(activity, backupModel));
    }

    /** 显示备份文件列表 */
    private void showBackupFileSelectDialog(Activity activity) {
        List<File> fileList = Backup.listBackupFiles(getApplication());

        String[] fileItems = new String[fileList.size()];
        for (int i = 0; i < fileItems.length; i++) {
            fileItems[i] = fileList.get(i).getName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setItems(fileItems, (dialog, which) -> {
            dialog.dismiss();
            String filePath = fileList.get(which).getAbsolutePath();
            // 弹框确认弹框
            readDataFromBackupJsonFile(filePath, backupModel ->
                    showRestoreDataConfirmDialog(activity, backupModel));
        });
        builder.setPositiveButton(
                R.string.dialog_btn_choose_local_file, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    activity.startActivityForResult(intent, 1);
                });
        builder.setNegativeButton(R.string.dialog_btn_cancel, (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create().show();
    }

    /**
     * 导入数据确认弹框
     *
     * @param activity    activity
     * @param backupModel 待导入的数据
     */
    private void showRestoreDataConfirmDialog(Activity activity, BackupModel backupModel) {
        BackupModelMetadata metadata = backupModel.getMetadata();
        String backupDate = new Date(metadata.getBackupDate()).toLocaleString();
        String backupDeviceName = metadata.getDeviceName();
        String backupExpenseCount = String.valueOf(metadata.getExpenseNumber());
        String backupVersion = metadata.getClientVersion() + "(" + metadata.getClientVersionCode() + ")";

        View view = activity.getLayoutInflater().inflate(R.layout.dialog_tally_restore_data_confirm, null);
        ((TextView) view.findViewById(R.id.tvBackupDate)).setText(backupDate);
        ((TextView) view.findViewById(R.id.tvBackupDeviceName)).setText(backupDeviceName);
        ((TextView) view.findViewById(R.id.tvBackupVersion)).setText(backupVersion);
        ((TextView) view.findViewById(R.id.tvBackupExpenseCount)).setText(backupExpenseCount);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.tally_alert_restore_data)
                .setView(view)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    dialog.dismiss();
                    restoreToDbFromBackupModel(backupModel);
                });
        AlertDialog confirmDialog = builder.create();
        confirmDialog.setCanceledOnTouchOutside(false);
        confirmDialog.show();
    }


    /** 备份到 JSON 文件中 */
    private void backup2JsonFile() {
        Backup.backupToJsonFile(getApplication(), new Backup.BackupProgressListener() {
            @Override
            public void onProgressUpdate(Backup.BackupProgress backupProgress) {
                switch (backupProgress) {
                    // 正在读取文件
                    case READ_DATA:
                        mProcessMessage.postValue(ResUtils.getString(
                                getApplication(), R.string.tally_alert_reading_db_data));
                        break;
                    // 正在写入文件
                    case WRITE_FILE:
                        mProcessMessage.postValue(ResUtils.getString(
                                getApplication(), R.string.tally_alert_write_data_2_file));
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void success(Void aVoid) {
                // 备份完成，隐藏加载框
                mProcessMessage.postValue(null);
                showToastShort(R.string.tally_alert_backup_success);
            }

            @Override
            public void failure(IError iError) {
                mProcessMessage.postValue(null);
                showToastLong(ResUtils.getString(getApplication(), R.string.tally_alert_backup_failure)
                        + " ERR:" + iError.msg());
            }
        });
    }

    /**
     * 读取备份文件信息
     *
     * @param filePath 备份文件所在目录
     * @param callback 回调
     */
    private void readDataFromBackupJsonFile(String filePath, SimpleCallback<BackupModel> callback) {
        File file = new File(filePath);
        Backup.readBackupJsonFile(file, new Backup.RestoreProgressListener() {
            @Override
            public void onProgressUpdate(Backup.RestoreProgress restoreProgress) {
                switch (restoreProgress) {
                    case READ_FILE:
                        mProcessMessage.postValue(ResUtils.getString(
                                getApplication(), R.string.tally_alert_reading_db_data));
                        break;
                    case CHECK_FILE_FORMAT:
                        mProcessMessage.postValue(ResUtils.getString(
                                getApplication(), R.string.tally_alert_check_data_format));
                        break;
                    case RESTORE_TO_DB:
                        mProcessMessage.postValue(ResUtils.getString(
                                getApplication(), R.string.tally_alert_restore_data));
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void success(BackupModel backupModel) {
                mProcessMessage.postValue(null);
                if (backupModel == null) {
                    TCAgent.onError(getApplication(), new IllegalStateException("备份文件读取失败"));
                    return;
                }
                runOnUiThread(() -> callback.success(backupModel));
            }

            @Override
            public void failure(IError iError) {
                mProcessMessage.postValue(null);
                showToastLong(iError.msg());
            }
        });
    }

    /**
     * 恢复到数据库
     *
     * @param backupModel 备份的数据
     */
    private void restoreToDbFromBackupModel(BackupModel backupModel) {
        Backup.restoreDataFromBackupData(getApplication(), backupModel, new Backup.RestoreProgressListener() {
            @Override
            public void onProgressUpdate(Backup.RestoreProgress restoreProgress) {
                switch (restoreProgress) {
                    case READ_FILE:
                        mProcessMessage.postValue(ResUtils.getString(
                                getApplication(), R.string.tally_alert_reading_db_data));
                        break;
                    case CHECK_FILE_FORMAT:
                        mProcessMessage.postValue(ResUtils.getString(
                                getApplication(), R.string.tally_alert_check_data_format));
                        break;
                    case RESTORE_TO_DB:
                        mProcessMessage.postValue(ResUtils.getString(
                                getApplication(), R.string.tally_alert_restore_data));
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void success(BackupModel backupModel) {
                mProcessMessage.postValue(null);
                showToastShort(R.string.tally_alert_restore_data_success);
            }

            @Override
            public void failure(IError iError) {
                showToastLong(ResUtils.getString(getApplication(), R.string.tally_alert_restore_data_failure)
                        + " ERR:" + iError.msg());
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // 生命周期
    ///////////////////////////////////////////////////////////////////////////

    protected void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            // Get the Uri of the selected file
            Uri uri = data.getData();
            String path = FileUtils.getPath(activity, uri);
            onBackupFileSelectedFromFileSystem(activity, path);
        }
    }

    public void onRequestPermissionsResult(Activity activity,
                                           int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (mPermissionReqHandler != null) {
            mPermissionReqHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}

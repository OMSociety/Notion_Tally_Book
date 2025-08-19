package com.coderpage.mine.app.tally.module.backup;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.DatePickerDialog;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.coderpage.base.common.IError;
import com.coderpage.base.common.SimpleCallback;
import com.coderpage.base.utils.FileUtils;
import com.coderpage.base.utils.ResUtils;
import com.coderpage.concurrency.AsyncTaskExecutor;
import com.coderpage.framework.BaseViewModel;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.permission.PermissionReqHandler;
import com.coderpage.mine.app.tally.utils.DatePickUtils;
import com.tendcloud.tenddata.TCAgent;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    private TextView mCurrentFolderTextView;

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

    /** 备份数据点击 */
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

    //导出文件
    /** 导出为CSV或Excel点击 */
    public void onExportToCsvOrExcelClick(Activity activity) {
        showExportDataDialog(activity);
    }

    /**
     * 显示导出数据对话框
     * @param activity Activity上下文
     */
    private void showExportDataDialog(Activity activity) {
        // 加载自定义布局
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_tally_export_data, null);

        TextView tvStartDate = view.findViewById(R.id.tvStartDate);
        TextView tvEndDate = view.findViewById(R.id.tvEndDate);
        TextView tvFolder = view.findViewById(R.id.tvFolder);
        RadioGroup rgFormat = view.findViewById(R.id.rgFormat);
        ImageView ivClearStartDate = view.findViewById(R.id.ivClearStartDate);
        ImageView ivClearEndDate = view.findViewById(R.id.ivClearEndDate);

        // 设置默认日期（最近30天）
        Calendar calendar = Calendar.getInstance();
        long endDate = calendar.getTimeInMillis();
        calendar.add(Calendar.MONTH, -1);
        long startDate = calendar.getTimeInMillis();

        // 格式化日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvStartDate.setText(sdf.format(new Date(startDate)));
        tvEndDate.setText(sdf.format(new Date(endDate)));
        tvStartDate.setTag(startDate);
        tvEndDate.setTag(endDate);

        // 更新清除按钮的可见性
        updateClearButtonVisibility(tvStartDate, ivClearStartDate, startDate);
        updateClearButtonVisibility(tvEndDate, ivClearEndDate, endDate);

        // 设置日期选择监听器
        tvStartDate.setOnClickListener(v -> {
            // 获取当前选择的日期作为默认值
            Calendar c = Calendar.getInstance();
            Long currentDate = (Long) v.getTag();
            if (currentDate != null) {
                c.setTimeInMillis(currentDate);
            }
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // 创建并显示 DatePickerDialog
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    activity,
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                        // 用户选择日期后的回调处理
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);
                        long timeInMillis = selectedDate.getTimeInMillis();
                        tvStartDate.setTag(timeInMillis);
                        tvStartDate.setText(sdf.format(selectedDate.getTime()));
                        updateClearButtonVisibility(tvStartDate, ivClearStartDate, timeInMillis);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        tvEndDate.setOnClickListener(v -> {
            // 获取当前选择的日期作为默认值
            Calendar c = Calendar.getInstance();
            Long currentDate = (Long) v.getTag();
            if (currentDate != null) {
                c.setTimeInMillis(currentDate);
            }
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // 创建并显示 DatePickerDialog
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    activity,
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                        // 用户选择日期后的回调处理
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);
                        long timeInMillis = selectedDate.getTimeInMillis();
                        tvEndDate.setTag(timeInMillis);
                        tvEndDate.setText(sdf.format(selectedDate.getTime()));
                        updateClearButtonVisibility(tvEndDate, ivClearEndDate, timeInMillis);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        // 设置文件夹选择监听器
        tvFolder.setOnClickListener(v -> {
            // 这里应该打开文件夹选择器
            // 目前我们显示一个简单的输入对话框作为示例
            showFolderSelectionDialog(activity, tvFolder);
        });

        // 设置清除按钮监听器
        ivClearStartDate.setOnClickListener(v -> {
            // 清除开始日期
            tvStartDate.setTag(null);
            tvStartDate.setText("");
            updateClearButtonVisibility(tvStartDate, ivClearStartDate, 0);
        });

        ivClearEndDate.setOnClickListener(v -> {
            // 清除结束日期
            tvEndDate.setTag(null);
            tvEndDate.setText("");
            updateClearButtonVisibility(tvEndDate, ivClearEndDate, 0);
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(" ")
                .setView(view)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    // 获取用户选择的参数
                    Long selectedStartDate = (Long) tvStartDate.getTag();
                    Long selectedEndDate = (Long) tvEndDate.getTag();
                    String selectedFolder = (String) tvFolder.getTag();
                    int selectedFormat = rgFormat.getCheckedRadioButtonId();

                    // 执行导出操作
                    exportData(activity, selectedStartDate, selectedEndDate, selectedFolder, selectedFormat);
                    dialog.dismiss();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 显示文件夹选择对话框
     *
     * @param activity Activity上下文
     * @param folderTextView 显示文件夹路径的TextView
     */
    private void showFolderSelectionDialog(Activity activity, TextView folderTextView) {
        // 保存当前对话框中的tvFolder引用
        mCurrentFolderTextView = folderTextView;
        // 使用系统文件管理器选择文件夹
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        activity.startActivityForResult(intent, 2);
    }
    /**
     * 更新清除按钮的可见性
     *
     * @param textView TextView
     * @param clearButton 清除按钮
     * @param date 日期时间戳
     */
    private void updateClearButtonVisibility(TextView textView, ImageView clearButton, long date) {
        // 如果有日期，则显示清除图标（X图标）
        if (date != 0 && !textView.getText().toString().isEmpty()) {
            clearButton.setImageResource(R.drawable.abc_ic_clear_material);
            clearButton.setVisibility(View.VISIBLE);
        } else {
            clearButton.setVisibility(View.GONE);
        }
    }

    /**
     * 导出数据
     * @param activity Activity上下文
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param folder 导出文件夹路径
     * @param formatId 格式ID（CSV或Excel）
     */
    private void exportData(Activity activity, Long startDate, Long endDate, String folder, int formatId) {
        // 检查存储权限
        String[] permissionArray = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if (mPermissionReqHandler == null) {
            mPermissionReqHandler = new PermissionReqHandler(activity);
        }
        mPermissionReqHandler.requestPermission(false, permissionArray, new PermissionReqHandler.Listener() {
            @Override
            public void onGranted(boolean grantedAll, String[] permissionArray) {
                // 执行实际的导出操作
                performExport(startDate, endDate, folder, formatId);
            }

            @Override
            public void onDenied(String[] permissionArray) {
                showToastShort(R.string.permission_request_failed_write_external_storage);
            }
        });
    }

    /**
     * 执行实际的数据导出操作
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param folder 导出文件夹路径
     * @param formatId 格式ID
     */
    private void performExport(Long startDate, Long endDate, String folder, int formatId) {
        // 这里是实际的导出逻辑
        // 目前只是显示一个提示信息
        mProcessMessage.postValue("正在导出数据到 " + folder + "...");

        // 模拟导出过程
        AsyncTaskExecutor.execute(() -> {
            try {
                File testFile = new File(folder, "test.txt");
                if (!testFile.exists()) {
                    testFile.createNewFile();
                }

                // 完成后更新UI
                runOnUiThread(() -> {
                    mProcessMessage.postValue(null);
                    String format = formatId == R.id.rbCsv ? "CSV" : "Excel";
                    showToastShort("数据已导出为" + format + "格式到 " + folder);
                });
            } catch (Exception e) {
                // 完成后更新UI
                runOnUiThread(() -> {
                    mProcessMessage.postValue(null);
                    showToastShort("数据失败");
                });
            }
        });
    }

    /**
     * 从URI获取文件夹路径
     *
     * @param context 上下文
     * @param uri 文件夹URI
     * @return 文件夹路径
     */
    private String getFolderPathFromUri(Context context, Uri uri) {
        // 使用DocumentFile处理目录URI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile documentFile = DocumentFile.fromTreeUri(context, uri);
            if (documentFile != null && documentFile.exists()) {
                // 对于外部存储文档
                if (FileUtils.isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getTreeDocumentId(uri);
                    final String[] split = docId.split(":");
                    if (split.length == 2) {
                        if ("primary".equalsIgnoreCase(split[0])) {
                            return Environment.getExternalStorageDirectory() + "/" + split[1];
                        }
                    }
                    // 如果无法解析，至少返回一个合理的默认值
                    return Environment.getExternalStorageDirectory().toString();
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // 外部存储文档
                if (FileUtils.isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    if (split.length == 2) {
                        if ("primary".equalsIgnoreCase(split[0])) {
                            return Environment.getExternalStorageDirectory() + "/" + split[1];
                        }
                    }
                    return Environment.getExternalStorageDirectory().toString();
                }
                // 下载文档
                else if (FileUtils.isDownloadsDocument(uri)) {
                    // 对于下载目录，我们返回下载目录路径
                    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                }
                // 其他文档类型
                else {
                    // 尝试使用FileUtils.getPath获取路径
                    String path = FileUtils.getPath(context, uri);
                    if (!TextUtils.isEmpty(path)) {
                        return path;
                    }
                }
            }
        }

        // 对于非Document URI，尝试直接获取路径
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        // 尝试使用FileUtils.getPath获取路径
        String path = FileUtils.getPath(context, uri);
        if (!TextUtils.isEmpty(path)) {
            return path;
        }

        // 如果无法解析路径，则使用默认下载目录
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    }



    ///////////////////////////////////////////////////////////////////////////
    // 生命周期
    ///////////////////////////////////////////////////////////////////////////
    protected void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                // Get the Uri of the selected file
                Uri uri = data.getData();
                String path = FileUtils.getPath(activity, uri);
                onBackupFileSelectedFromFileSystem(activity, path);
            } else if (requestCode == 2) {
                // 处理文件夹选择结果
                Uri uri = data.getData();
                if (uri != null) {
                    // 使用DocumentFile处理文件夹URI
                    String path = getFolderPathFromUri(activity, uri);
                    if (path != null) {
                        // 更新UI
                        if (mCurrentFolderTextView != null) {
                            mCurrentFolderTextView.setText(path);
                            mCurrentFolderTextView.setTag(path);
                        }
                    }
                }
            }
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

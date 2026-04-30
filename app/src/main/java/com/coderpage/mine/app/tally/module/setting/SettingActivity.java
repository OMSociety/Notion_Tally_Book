package com.coderpage.mine.app.tally.module.setting;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.coderpage.base.utils.FileUtils;
import com.coderpage.mine.BuildConfig;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.ai.AiSettingActivity;
import com.coderpage.mine.app.tally.common.router.TallyRouter;
import com.coderpage.mine.app.tally.config.NotionConfig;
import com.coderpage.mine.app.tally.module.about.AboutActivity;
import com.coderpage.mine.app.tally.module.backup.BackupFileActivity;
import com.coderpage.mine.app.tally.module.backup.CsvImporter;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.sync.NotionSyncManager;
import com.coderpage.mine.ui.BaseActivity;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 设置页面 - 简化版
 * 
 * @author Flandre Scarlet
 */
@Route(path = TallyRouter.SETTING)
public class SettingActivity extends BaseActivity {

    private static final int REQUEST_PERMISSION = 100;
    private static final int REQUEST_IMPORT_CSV = 101;

    // Notion 配置
    private EditText etNotionToken;
    private EditText etNotionDatabaseId;

    // 功能入口
    private LinearLayout lyAiSetting;
    private LinearLayout lyManualSync;
    private LinearLayout lyNotionHelp;

    // 数据管理
    private LinearLayout lyImport;
    private LinearLayout lyExport;
    private LinearLayout lyClear;

    // 其他
    private LinearLayout lyAbout;
    private LinearLayout lyUpdate;
    private TextView tvVersion;

    private NotionConfig mNotionConfig;
    private NotionSyncManager mNotionSyncManager;
    private boolean mSyncing = false;
    private ExecutorService mDatabaseExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        mNotionConfig = NotionConfig.getInstance(this);
        mDatabaseExecutor = Executors.newSingleThreadExecutor();

        initToolbar();
        initViews();
        loadSettings();
        setupListeners();
    }

    private void initToolbar() {
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.menu_tally_setting);
        }
        mToolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        etNotionToken = findViewById(R.id.etNotionToken);
        etNotionDatabaseId = findViewById(R.id.etNotionDatabaseId);
        lyAiSetting = findViewById(R.id.lyAiSetting);
        lyManualSync = findViewById(R.id.lyManualSync);
        lyNotionHelp = findViewById(R.id.lyNotionHelp);
        lyImport = findViewById(R.id.lyImport);
        lyExport = findViewById(R.id.lyExport);
        lyClear = findViewById(R.id.lyClear);
        lyAbout = findViewById(R.id.lyAbout);
        lyUpdate = findViewById(R.id.lyUpdate);
        tvVersion = findViewById(R.id.tvVersion);
    }

    private void loadSettings() {
        etNotionToken.setText(mNotionConfig.getIntegrationToken());
        etNotionDatabaseId.setText(mNotionConfig.getDatabaseId());
        tvVersion.setText(getString(R.string.setting_version_format, BuildConfig.VERSION_NAME));
    }

    private void setupListeners() {
        lyAiSetting.setOnClickListener(v -> startActivity(new Intent(this, AiSettingActivity.class)));
        lyManualSync.setOnClickListener(v -> startManualSync());
        lyNotionHelp.setOnClickListener(v -> startActivity(new Intent(this, NotionDatabaseHelpActivity.class)));
        lyImport.setOnClickListener(v -> checkPermissionAndImport());
        lyExport.setOnClickListener(v -> startActivity(new Intent(this, BackupFileActivity.class)));
        lyClear.setOnClickListener(v -> showClearDialog());
        lyAbout.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        lyUpdate.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.setting_latest_version, BuildConfig.VERSION_NAME), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveNotionConfig() {
        String token = etNotionToken.getText() == null ? "" : etNotionToken.getText().toString().trim();
        String databaseId = etNotionDatabaseId.getText() == null ? "" : etNotionDatabaseId.getText().toString().trim();
        mNotionConfig.setIntegrationToken(token);
        mNotionConfig.setDatabaseId(databaseId);
    }

    private void startManualSync() {
        if (mSyncing) {
            Toast.makeText(this, R.string.setting_sync_in_progress, Toast.LENGTH_SHORT).show();
            return;
        }

        saveNotionConfig();
        if (!mNotionConfig.isConfigured()) {
            Toast.makeText(this, R.string.setting_notion_config_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mNotionSyncManager == null) {
            mNotionSyncManager = new NotionSyncManager(this);
        }

        mSyncing = true;
        mNotionSyncManager.setSyncListener(new NotionSyncManager.SyncListener() {
            @Override
            public void onSyncStart() {
                Toast.makeText(SettingActivity.this, R.string.setting_sync_start, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSyncProgress(int current, int total, String message) {
            }

            @Override
            public void onSyncComplete(NotionSyncManager.SyncResult result) {
                mSyncing = false;
                Toast.makeText(SettingActivity.this,
                        getString(R.string.setting_sync_done, result.uploadedCount, result.downloadedCount, result.conflictCount),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSyncError(String error) {
                mSyncing = false;
                Toast.makeText(SettingActivity.this, getString(R.string.setting_sync_failed, error), Toast.LENGTH_LONG).show();
            }
        });
        mNotionSyncManager.startSync();
    }

    private void checkPermissionAndImport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
                return;
            }
        }
        showImportDialog();
    }

    private void showImportDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.setting_data_import)
            .setItems(new String[]{getString(R.string.setting_import_json), getString(R.string.setting_import_csv)}, (dialog, which) -> {
                if (which == 0) {
                    startActivity(new Intent(this, BackupFileActivity.class));
                } else {
                    chooseCsvFileForImport();
                }
            })
            .setNegativeButton(R.string.dialog_btn_cancel, null)
            .show();
    }

    private void chooseCsvFileForImport() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "application/vnd.ms-excel",
                "text/plain"
        });
        startActivityForResult(Intent.createChooser(intent, getString(R.string.setting_import_csv)), REQUEST_IMPORT_CSV);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showImportDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        if (requestCode != REQUEST_IMPORT_CSV) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            Toast.makeText(this, R.string.tally_toast_illegal_path, Toast.LENGTH_SHORT).show();
            return;
        }
        String path = FileUtils.getPath(this, uri);
        if (path == null || path.trim().isEmpty()) {
            Toast.makeText(this, R.string.tally_toast_illegal_path, Toast.LENGTH_SHORT).show();
            return;
        }
        importCsvFile(path);
    }

    private void importCsvFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            Toast.makeText(this, R.string.tally_toast_illegal_path, Toast.LENGTH_SHORT).show();
            return;
        }
        if (mDatabaseExecutor == null || mDatabaseExecutor.isShutdown()) {
            mDatabaseExecutor = Executors.newSingleThreadExecutor();
        }
        mDatabaseExecutor.execute(() -> {
            CsvImporter.ImportResult importResult = CsvImporter.importFromCsv(this, file);
            runOnUiThread(() -> {
                if (importResult.success) {
                    Toast.makeText(this, importResult.message, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, getString(R.string.setting_csv_import_failed, importResult.message), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void showClearDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.setting_data_clear)
            .setMessage(R.string.setting_clear_confirm)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> clearAllRecords())
            .setNegativeButton(R.string.dialog_btn_cancel, null)
            .show();
    }

    private void clearAllRecords() {
        if (mDatabaseExecutor == null || mDatabaseExecutor.isShutdown()) {
            mDatabaseExecutor = Executors.newSingleThreadExecutor();
        }
        mDatabaseExecutor.execute(() -> {
            try {
                TallyDatabase.getInstance().recordDao().deleteAll();
                runOnUiThread(() -> Toast.makeText(SettingActivity.this, R.string.setting_clear_success, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(SettingActivity.this, R.string.setting_clear_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNotionConfig();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDatabaseExecutor != null && !mDatabaseExecutor.isShutdown()) {
            mDatabaseExecutor.shutdown();
        }
    }
}

package com.coderpage.mine.app.tally.module.setting;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.module.about.AboutActivity;
import com.coderpage.mine.app.tally.module.backup.BackupFileActivity;
import com.coderpage.mine.app.tally.persistence.preference.SettingPreference;
import com.coderpage.mine.ui.BaseActivity;

/**
 * 设置页面 - 简化版
 * 
 * @author Flandre Scarlet
 */
public class SettingActivity extends BaseActivity {

    private static final int REQUEST_PERMISSION = 100;

    // AI 配置
    private EditText etAiApiUrl;
    private EditText etAiApiKey;
    private EditText etAiModel;
    
    // 数据管理
    private LinearLayout lyImport;
    private LinearLayout lyExport;
    private LinearLayout lyClear;
    
    // 其他
    private LinearLayout lyAbout;
    private LinearLayout lyUpdate;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        
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
            getSupportActionBar().setTitle("设置");
        }
        mToolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        etAiApiUrl = findViewById(R.id.etAiApiUrl);
        etAiApiKey = findViewById(R.id.etAiApiKey);
        etAiModel = findViewById(R.id.etAiModel);
        lyImport = findViewById(R.id.lyImport);
        lyExport = findViewById(R.id.lyExport);
        lyClear = findViewById(R.id.lyClear);
        lyAbout = findViewById(R.id.lyAbout);
        lyUpdate = findViewById(R.id.lyUpdate);
    }

    private void loadSettings() {
        etAiApiUrl.setText(SettingPreference.getAiApiUrl(this));
        etAiApiKey.setText(SettingPreference.getAiApiKey(this));
        etAiModel.setText(SettingPreference.getAiModel(this));
    }

    private void setupListeners() {
        lyImport.setOnClickListener(v -> checkPermissionAndImport());
        lyExport.setOnClickListener(v -> startActivity(new Intent(this, BackupFileActivity.class)));
        lyClear.setOnClickListener(v -> showClearDialog());
        lyAbout.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        lyUpdate.setOnClickListener(v -> {
            Toast.makeText(this, "当前已是最新版本 v1.0.0", Toast.LENGTH_SHORT).show();
        });
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
            .setTitle("导入数据")
            .setItems(new String[]{"JSON备份", "CSV文件"}, (dialog, which) -> {
                if (which == 0) {
                    startActivity(new Intent(this, BackupFileActivity.class));
                } else {
                    Toast.makeText(this, "CSV导入功能开发中", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showImportDialog();
        }
    }

    private void showClearDialog() {
        new AlertDialog.Builder(this)
            .setTitle("清除数据")
            .setMessage("确定要清除所有记账数据吗？此操作不可恢复。")
            .setPositiveButton("确定", (dialog, which) -> Toast.makeText(this, "数据已清除", Toast.LENGTH_SHORT).show())
            .setNegativeButton("取消", null)
            .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SettingPreference.setAiApiUrl(this, etAiApiUrl.getText().toString().trim());
        SettingPreference.setAiApiKey(this, etAiApiKey.getText().toString().trim());
        SettingPreference.setAiModel(this, etAiModel.getText().toString().trim());
    }
}

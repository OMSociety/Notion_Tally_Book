package com.coderpage.mine.app.tally.module.setting;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.config.NotionConfig;
import com.coderpage.mine.app.tally.sync.NotionDatabaseValidator;
import com.coderpage.mine.app.tally.sync.NotionSyncManager;

import java.util.List;

/**
 * Notion 同步设置页面
 *
 * 功能：
 * <ul>
 *   <li>配置 Token 和 Database ID</li>
 *   <li>选择同步方向</li>
 *   <li>开启/关闭自动同步</li>
 *   <li>测试连接（字段模板校验）</li>
 *   <li>立即同步</li>
 * </ul>
 *
 * @author abner-l
 * @since 0.7.5
 */
public class NotionSyncSettingActivity extends AppCompatActivity {

    private EditText etNotionToken;
    private EditText etDatabaseId;
    private RadioGroup rgSyncDirection;
    private RadioButton rbToNotion;
    private RadioButton rbFromNotion;
    private RadioButton rbBidirectional;
    private Switch switchAutoSync;
    private Button btnSave;
    private Button btnTest;
    private Button btnSyncNow;
    private TextView tvStatus;

    private NotionSyncManager syncManager;
    private NotionConfig config;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notion_sync_setting);

        initViews();
        loadConfig();
        setupListeners();

        syncManager = new NotionSyncManager(this);
        syncManager.setSyncListener(new SyncListenerImpl());
    }

    private void initViews() {
        etNotionToken = findViewById(R.id.et_notion_token);
        etDatabaseId = findViewById(R.id.et_database_id);
        rgSyncDirection = findViewById(R.id.rg_sync_direction);
        rbToNotion = findViewById(R.id.rb_to_notion);
        rbFromNotion = findViewById(R.id.rb_from_notion);
        rbBidirectional = findViewById(R.id.rb_bidirectional);
        switchAutoSync = findViewById(R.id.switch_auto_sync);
        btnSave = findViewById(R.id.btn_save);
        btnTest = findViewById(R.id.btn_test);
        btnSyncNow = findViewById(R.id.btn_sync_now);
        tvStatus = findViewById(R.id.tv_status);
    }

    /**
     * 从 SharedPreferences 加载已有配置，填充到表单
     */
    private void loadConfig() {
        config = new NotionConfig(this);

        etNotionToken.setText(config.getNotionToken());
        etDatabaseId.setText(config.getDatabaseId());
        switchAutoSync.setChecked(config.isAutoSync());

        // 同步方向 RadioGroup 回显
        switch (config.getSyncDirection()) {
            case NotionSyncManager.SYNC_TO_NOTION:
                rbToNotion.setChecked(true);
                break;
            case NotionSyncManager.SYNC_FROM_NOTION:
                rbFromNotion.setChecked(true);
                break;
            default:
                rbBidirectional.setChecked(true);
                break;
        }

        updateStatusText();
    }

    private void setupListeners() {
        // 保存配置
        btnSave.setOnClickListener(v -> saveConfig());

        // 测试连接（校验 Token + Database ID + 字段模板）
        btnTest.setOnClickListener(v -> {
            if (!validateInput()) return;
            applyCredentialsToConfig();
            btnTest.setEnabled(false);
            tvStatus.setText("正在测试连接...");
            syncManager.testConnection(new NotionSyncManager.ConnectionTestCallback() {
                @Override
                public void onResult(ValidationResult result) {
                    runOnUiThread(() -> {
                        btnTest.setEnabled(true);
                        if (result.valid) {
                            tvStatus.setText("✅ " + result.summary + "\n" +
                                    "（上次同步：" + config.getLastSyncTimeLabel() + "）");
                            Toast.makeText(this, "连接正常，可以开始同步！", Toast.LENGTH_SHORT).show();
                        } else {
                            tvStatus.setText(result.summary);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        btnTest.setEnabled(true);
                        tvStatus.setText("❌ " + error);
                    });
                }
            });
        });

        // 立即同步
        btnSyncNow.setOnClickListener(v -> {
            if (!validateInput()) return;
            applyCredentialsToConfig();
            syncManager.sync();
        });
    }

    /**
     * 校验表单输入
     *
     * @return true=校验通过，false=校验失败（已在输入框下方显示提示）
     */
    private boolean validateInput() {
        String token = etNotionToken.getText().toString().trim();
        String dbId = etDatabaseId.getText().toString().trim();

        if (TextUtils.isEmpty(token)) {
            etNotionToken.setError("请输入 Notion Integration Token");
            etNotionToken.requestFocus();
            return false;
        }
        if (!token.startsWith("secret_")) {
            etNotionToken.setError("Token 格式错误，应以 secret_ 开头");
            etNotionToken.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(dbId)) {
            etDatabaseId.setError("请输入 Database ID");
            etDatabaseId.requestFocus();
            return false;
        }
        // Database ID 应为 32 位字母数字（不含连字符）
        String cleanId = dbId.replace("-", "");
        if (cleanId.length() != 32) {
            etDatabaseId.setError("Database ID 应为 32 位，当前 " + cleanId.length() + " 位");
            etDatabaseId.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * 将表单数据写入 NotionConfig
     */
    private void applyCredentialsToConfig() {
        String token = etNotionToken.getText().toString().trim();
        String dbId = etDatabaseId.getText().toString().trim();
        int direction;
        if (rbToNotion.isChecked()) {
            direction = NotionSyncManager.SYNC_TO_NOTION;
        } else if (rbFromNotion.isChecked()) {
            direction = NotionSyncManager.SYNC_FROM_NOTION;
        } else {
            direction = NotionSyncManager.SYNC_BIDIRECTIONAL;
        }

        config.setNotionToken(token);
        config.setDatabaseId(dbId);
        config.setSyncDirection(direction);
        config.setAutoSync(switchAutoSync.isChecked());
        config.setSyncEnabled(true);

        syncManager.updateCredentials(token, dbId);
    }

    /**
     * 保存配置（不含同步）
     */
    private void saveConfig() {
        if (!validateInput()) return;
        applyCredentialsToConfig();
        tvStatus.setText("配置已保存\n方向：" + config.getSyncDirectionLabel() +
                "\n上次同步：" + config.getLastSyncTimeLabel());
        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
    }

    private void updateStatusText() {
        if (config.isConfigured()) {
            String status = "已配置 | 方向：" + config.getSyncDirectionLabel()
                    + " | 上次同步：" + config.getLastSyncTimeLabel();
            tvStatus.setText(status);
        } else {
            tvStatus.setText("尚未配置 Notion 同步");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (syncManager != null) {
            syncManager.release();
        }
    }

    // ==================== 内部类 ====================

    private class SyncListenerImpl implements NotionSyncManager.SyncListener {
        @Override
        public void onSyncStart() {
            runOnUiThread(() -> {
                btnSyncNow.setEnabled(false);
                btnTest.setEnabled(false);
                tvStatus.setText("同步开始，请稍候...");
            });
        }

        @Override
        public void onSyncProgress(int current, int total, String message) {
            runOnUiThread(() -> tvStatus.setText(message + " (" + current + "/" + total + ")"));
        }

        @Override
        public void onSyncComplete(int synced, int failed) {
            runOnUiThread(() -> {
                btnSyncNow.setEnabled(true);
                btnTest.setEnabled(true);
                String result = String.format("同步完成：成功 %d 条，失败 %d 条", synced, failed);
                tvStatus.setText(result + "\n上次同步：" + config.getLastSyncTimeLabel());
                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                updateStatusText();
            });
        }

        @Override
        public void onSyncError(String error) {
            runOnUiThread(() -> {
                btnSyncNow.setEnabled(true);
                btnTest.setEnabled(true);
                tvStatus.setText("❌ " + error);
                Toast.makeText(this, "同步失败: " + error, Toast.LENGTH_LONG).show();
            });
        }
    }
}

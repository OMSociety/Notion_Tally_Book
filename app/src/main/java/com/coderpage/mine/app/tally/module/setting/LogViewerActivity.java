package com.coderpage.mine.app.tally.module.setting;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.coderpage.mine.R;
import com.coderpage.mine.ui.BaseActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 日志查看页面
 */
public class LogViewerActivity extends BaseActivity {

    private TextView tvLogContent;
    private ExecutorService mExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);
        mExecutor = Executors.newSingleThreadExecutor();

        initToolbar();
        initViews();
        loadLogs();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("日志查看");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        tvLogContent = findViewById(R.id.tvLogContent);
    }

    private void loadLogs() {
        tvLogContent.setText("正在加载日志...");
        mExecutor.execute(() -> {
            List<String> logLines = new ArrayList<>();
            BufferedReader reader = null;
            try {
                // 读取 logcat 最近 500 行日志
                Process process = Runtime.getRuntime().exec(new String[]{
                        "logcat", "-d", "-v", "time", "-t", "500"
                });
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    logLines.add(line);
                }
                process.waitFor();
            } catch (Exception e) {
                logLines.add("读取日志失败: " + e.getMessage());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception ignored) {
                    }
                }
            }

            // 反转使最新日志在最前面
            List<String> reversed = new ArrayList<>();
            for (int i = logLines.size() - 1; i >= 0; i--) {
                reversed.add(logLines.get(i));
            }

            StringBuilder sb = new StringBuilder();
            for (String s : reversed) {
                sb.append(s).append("\n");
            }

            String logText = sb.toString().isEmpty() ? "暂无日志" : sb.toString();
            runOnUiThread(() -> tvLogContent.setText(logText));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mExecutor != null && !mExecutor.isShutdown()) {
            mExecutor.shutdown();
        }
    }
}

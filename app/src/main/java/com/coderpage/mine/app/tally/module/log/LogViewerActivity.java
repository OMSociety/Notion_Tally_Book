package com.coderpage.mine.app.tally.module.log;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.router.TallyRouter;
import com.coderpage.mine.ui.BaseActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 日志查看器
 */
@Route(path = TallyRouter.LOG_VIEWER)
public class LogViewerActivity extends BaseActivity {

    private TextView mLogContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        mLogContent = findViewById(R.id.tvLogContent);

        loadLogs();
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setToolbarAsBack((View v) -> finish());
    }

    private void loadLogs() {
        mLogContent.setText("正在加载日志...");
        new Thread(() -> {
            StringBuilder logBuilder = new StringBuilder();
            try {
                // 获取应用日志目录
                File logDir = new File(getFilesDir(), "logs");
                if (!logDir.exists()) {
                    logDir.mkdirs();
                }

                // 查找最新的日志文件
                File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
                if (logFiles != null && logFiles.length > 0) {
                    // 找到最新的日志文件
                    File latestLog = logFiles[0];
                    for (File file : logFiles) {
                        if (file.lastModified() > latestLog.lastModified()) {
                            latestLog = file;
                        }
                    }

                    // 读取日志内容
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    try (BufferedReader reader = new BufferedReader(new FileReader(latestLog))) {
                        String line;
                        int lineCount = 0;
                        while ((line = reader.readLine()) != null && lineCount < 1000) {
                            logBuilder.append(line).append("\n");
                            lineCount++;
                        }
                    }
                } else {
                    // 读取系统日志 - 只过滤本应用的
                    loadAppSystemLogs(logBuilder);
                }
            } catch (IOException e) {
                logBuilder.append("读取日志失败: ").append(e.getMessage());
            }

            String logText = logBuilder.toString();
            runOnUiThread(() -> {
                if (logText.isEmpty()) {
                    mLogContent.setText("暂无日志记录\n\n应用运行日志将显示在这里。\n\n提示：使用应用过程中产生的日志会自动记录。");
                } else {
                    mLogContent.setText(logText);
                }
            });
        }).start();
    }

    private void loadAppSystemLogs(StringBuilder logBuilder) {
        BufferedReader reader = null;
        try {
            // 只获取本应用的日志 - 过滤包名
            String packageName = getPackageName();
            Process process = Runtime.getRuntime().exec(new String[]{
                    "logcat", "-d", "-v", "time", "-t", "200",
                    "--pid=" + android.os.Process.myPid()
            });
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            List<String> logLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                logLines.add(line);
            }

            // 反转使最新日志在最前面
            for (int i = logLines.size() - 1; i >= 0; i--) {
                logBuilder.append(logLines.get(i)).append("\n");
            }
        } catch (IOException e) {
            // ignore
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}

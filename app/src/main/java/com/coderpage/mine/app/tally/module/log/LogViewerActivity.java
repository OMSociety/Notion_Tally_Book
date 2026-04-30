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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 日志查看器
 */
@Route(path = TallyRouter.LOG_VIEWER)
public class LogViewerActivity extends BaseActivity {

    private TextView mLogContent;
    private TextView mLogInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        mLogContent = findViewById(R.id.tvLogContent);
        mLogInfo = findViewById(R.id.tvLogInfo);

        loadLogs();
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setToolbarAsBack((View v) -> finish());
    }

    private void loadLogs() {
        try {
            // 获取应用日志目录
            File logDir = new File(getFilesDir(), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // 查找最新的日志文件
            File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
            if (logFiles == null || logFiles.length == 0) {
                // 尝试读取系统日志
                loadSystemLogs();
                return;
            }

            // 找到最新的日志文件
            File latestLog = logFiles[0];
            for (File file : logFiles) {
                if (file.lastModified() > latestLog.lastModified()) {
                    latestLog = file;
                }
            }

            // 读取日志内容
            StringBuilder logBuilder = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            try (BufferedReader reader = new BufferedReader(new FileReader(latestLog))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < 1000) {
                    logBuilder.append(line).append("\n");
                    lineCount++;
                }
            }

            mLogContent.setText(logBuilder.toString());
            mLogInfo.setText(String.format("日志文件: %s\n更新时间: %s\n日志行数: %d",
                    latestLog.getName(),
                    sdf.format(new Date(latestLog.lastModified())),
                    logBuilder.toString().split("\n").length));

        } catch (IOException e) {
            mLogContent.setText("读取日志失败: " + e.getMessage());
            mLogInfo.setText("错误信息");
            Toast.makeText(this, "读取日志失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSystemLogs() {
        try {
            // 使用 logcat 获取最近的日志
            Process process = Runtime.getRuntime().exec("logcat -d -t 100");
            java.io.InputStream inputStream = process.getInputStream();
            java.io.InputStreamReader inputStreamReader = new java.io.InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);

            StringBuilder logBuilder = new StringBuilder();
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 100) {
                logBuilder.append(line).append("\n");
                lineCount++;
            }

            reader.close();
            inputStreamReader.close();
            inputStream.close();

            if (logBuilder.length() > 0) {
                mLogContent.setText(logBuilder.toString());
                mLogInfo.setText("系统日志 (最近100条)\n" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            } else {
                mLogContent.setText("暂无日志记录\n\n应用运行日志将显示在这里。\n\n提示：使用应用过程中产生的日志会自动记录。");
                mLogInfo.setText("暂无日志");
            }

        } catch (IOException e) {
            mLogContent.setText("暂无日志记录\n\n应用运行日志将显示在这里。\n\n提示：使用应用过程中产生的日志会自动记录。");
            mLogInfo.setText("暂无日志");
        }
    }
}

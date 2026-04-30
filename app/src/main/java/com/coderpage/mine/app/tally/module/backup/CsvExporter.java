package com.coderpage.mine.app.tally.module.backup;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.coderpage.mine.app.tally.persistence.model.Record;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CSV 导出工具类
 * 导出格式：时间,分类,金额,类型,备注
 *
 * @author Flandre Scarlet
 */
public class CsvExporter {

    private static final String TAG = "CsvExporter";

    /**
     * 导出结果
     */
    public static class ExportResult {
        public boolean success;
        public String message;
        public String filePath;
    }

    /**
     * 导出所有记录为 CSV 文件
     */
    public static ExportResult exportToCsv(Context context) {
        ExportResult result = new ExportResult();
        try {
            List<Record> records = TallyDatabase.getInstance().recordDao().queryAll();
            if (records == null || records.isEmpty()) {
                result.success = false;
                result.message = "没有可导出的记录";
                return result;
            }

            // 创建导出目录
            File exportDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "export");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // 生成文件名
            SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "tally_export_" + fileNameFormat.format(new Date()) + ".csv";
            File csvFile = new File(exportDir, fileName);

            // 写入 CSV
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(csvFile), StandardCharsets.UTF_8)) {
                // 写入 BOM（Excel 打开需要）
                writer.write('\uFEFF');
                // 写入表头
                writer.write("时间,分类,金额,类型,备注\n");

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                for (Record record : records) {
                    String time = dateFormat.format(new Date(record.getTime()));
                    String category = escapeCsv(record.getCategoryName());
                    String amount = String.valueOf(record.getAmount());
                    String type = record.getType() == 0 ? "支出" : "收入";
                    String remark = escapeCsv(record.getDesc());

                    writer.write(time + "," + category + "," + amount + "," + type + "," + remark + "\n");
                }
            }

            result.success = true;
            result.message = "导出成功，共 " + records.size() + " 条记录";
            result.filePath = csvFile.getAbsolutePath();
            Log.i(TAG, "Exported " + records.size() + " records to " + csvFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            result.success = false;
            result.message = "导出失败：" + e.getMessage();
        }
        return result;
    }

    /**
     * 转义 CSV 字段（处理逗号、引号、换行）
     */
    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

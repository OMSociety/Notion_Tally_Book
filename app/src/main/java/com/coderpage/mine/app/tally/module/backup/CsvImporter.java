package com.coderpage.mine.app.tally.module.backup;

import android.content.Context;
import android.util.Log;

import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CSV 导入工具类
 * 支持从老 MINE 记账本导出的 CSV 文件导入
 * 
 * @author Flandre Scarlet
 */
public class CsvImporter {

    private static final String TAG = "CsvImporter";
    private static final SimpleDateFormat[] DATE_FORMATS = {
        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()),
        new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
        new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()),
        new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    };

    /**
     * 从 CSV 文件导入记录
     */
    public static ImportResult importFromCsv(Context context, File file) {
        ImportResult result = new ImportResult();
        List<RecordEntity> records = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;
            int successCount = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // 跳过第一行（表头）
                if (isFirstLine) {
                    isFirstLine = false;
                    if (line.contains("时间") || line.contains("日期") || 
                        line.toLowerCase().contains("time") || line.toLowerCase().contains("date")) {
                        continue;
                    }
                }
                
                // 跳过 BOM
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                RecordEntity record = parseLine(line);
                if (record != null) {
                    records.add(record);
                    successCount++;
                }
            }
            
            // 保存到数据库
            if (!records.isEmpty()) {
                TallyDatabase database = TallyDatabase.getInstance();
                for (RecordEntity record : records) {
                    database.recordDao().insert(record);
                }
            }
            
            result.success = true;
            result.totalCount = records.size();
            result.successCount = successCount;
            result.message = "成功导入 " + successCount + " 条记录";
            
            Log.d(TAG, "CSV 导入完成: 成功=" + successCount);
            
        } catch (IOException e) {
            Log.e(TAG, "CSV 导入失败", e);
            result.success = false;
            result.message = "读取文件失败: " + e.getMessage();
        }
        
        return result;
    }

    private static RecordEntity parseLine(String line) {
        List<String> fields = parseCsvFields(line);
        
        if (fields.isEmpty()) {
            return null;
        }
        
        RecordEntity record = new RecordEntity();
        record.setTime(System.currentTimeMillis());
        record.setDesc("");
        
        // 尝试解析金额
        for (String field : fields) {
            try {
                double amount = parseAmount(field.trim());
                if (amount != 0) {
                    record.setAmount(Math.abs(amount));
                    String lineLower = line.toLowerCase();
                    if (lineLower.contains("收入") || lineLower.contains("income")) {
                        record.setType(RecordEntity.TYPE_INCOME);
                    } else {
                        record.setType(RecordEntity.TYPE_EXPENSE);
                    }
                    break;
                }
            } catch (NumberFormatException e) {
                // 忽略
            }
        }
        
        if (record.getAmount() <= 0) {
            return null;
        }
        
        // 提取备注
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i).trim();
            if (!isNumber(field) && !isDateTime(field) && !field.contains("收入") && 
                !field.contains("支出") && !field.contains("expense") && !field.contains("income")) {
                if (record.getDesc().isEmpty()) {
                    record.setDesc(field);
                }
            }
        }
        
        return record;
    }

    private static List<String> parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }

    private static double parseAmount(String str) {
        if (str == null || str.isEmpty()) return 0;
        str = str.replaceAll("[^\\d.-]", "");
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean isNumber(String str) {
        return parseAmount(str) != 0;
    }

    private static boolean isDateTime(String str) {
        for (SimpleDateFormat format : DATE_FORMATS) {
            try {
                format.parse(str.trim());
                return true;
            } catch (ParseException e) {
                // 忽略
            }
        }
        return false;
    }

    public static class ImportResult {
        public boolean success;
        public int totalCount;
        public int successCount;
        public int failCount;
        public String message;
    }
}

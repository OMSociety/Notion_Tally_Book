package com.coderpage.mine.app.tally.module.backup;

import android.content.Context;
import android.util.Log;

import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV 导入工具类
 * 支持通用 CSV 格式：时间,分类,金额,类型,备注
 * 
 * @author Flandre Scarlet
 */
public class CsvImporter {

    private static final String TAG = "CsvImporter";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

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
                
                // 跳过 BOM
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                
                // 跳过空行
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // 跳过第一行（表头）
                if (isFirstLine) {
                    isFirstLine = false;
                    // 检测是否是表头行
                    String lowerLine = line.toLowerCase();
                    if (lowerLine.contains("时间") || lowerLine.contains("分类") || 
                        lowerLine.contains("金额") || lowerLine.contains("date") ||
                        lowerLine.contains("category") || lowerLine.contains("amount")) {
                        continue;
                    }
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
        
        if (fields.size() < 3) {
            return null;
        }
        
        RecordEntity record = new RecordEntity();
        record.setTime(System.currentTimeMillis());
        record.setDesc("");
        record.setType(RecordEntity.TYPE_EXPENSE); // 默认支出
        
        // 尝试解析每个字段
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i).trim();
            
            // 跳过空字段
            if (field.isEmpty()) {
                continue;
            }
            
            // 尝试解析时间
            if (record.getTime() == 0 || record.getTime() == System.currentTimeMillis()) {
                long timestamp = parseDateTime(field);
                if (timestamp > 0) {
                    record.setTime(timestamp);
                    continue;
                }
            }
            
            // 尝试解析金额
            double amount = parseAmount(field);
            if (amount > 0) {
                record.setAmount(amount);
                continue;
            }
            
            // 判断是收入还是支出
            String lowerField = field.toLowerCase();
            if (lowerField.contains("收入") || lowerField.contains("支出") ||
                lowerField.contains("income") || lowerField.contains("expense") ||
                lowerField.contains("支出") || lowerField.contains("收入")) {
                if (lowerField.contains("收入")) {
                    record.setType(RecordEntity.TYPE_INCOME);
                } else {
                    record.setType(RecordEntity.TYPE_EXPENSE);
                }
                continue;
            }
            
            // 如果还没有设置分类，设置它
            if (record.getCategoryUniqueName() == null || record.getCategoryUniqueName().isEmpty()) {
                record.setCategoryUniqueName(field);
            }
            
            // 如果还没有设置备注，设置它
            if (record.getDesc().isEmpty()) {
                record.setDesc(field);
            }
        }
        
        // 如果金额为0，跳过这条记录
        if (record.getAmount() <= 0) {
            return null;
        }
        
        return record;
    }

    /**
     * 解析 CSV 字段（处理带引号和转义的字段）
     */
    private static List<String> parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // 转义的引号
                    field.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
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

    /**
     * 解析日期时间
     * 支持格式：2026-04-11 20:45, 2026-04-11, 2026/04/11 20:45
     */
    private static long parseDateTime(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        
        str = str.trim();
        
        // 尝试多种日期格式
        String[] formats = {
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm",
            "yyyy/MM/dd",
            "yyyy.MM.dd HH:mm",
            "yyyy.MM.dd"
        };
        
        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                return sdf.parse(str).getTime();
            } catch (ParseException e) {
                // 尝试下一个格式
            }
        }
        
        return 0;
    }

    /**
     * 解析金额
     */
    private static double parseAmount(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        
        // 移除货币符号、千分位分隔符和空格
        str = str.trim()
                .replaceAll("[¥$€£]", "")
                .replaceAll(",", "")
                .replaceAll("\\s", "");
        
        // 保留数字、小数点和负号
        str = str.replaceAll("[^\\d.-]", "");
        
        try {
            return Math.abs(Double.parseDouble(str));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static class ImportResult {
        public boolean success;
        public int totalCount;
        public int successCount;
        public int failCount;
        public String message;
    }
}

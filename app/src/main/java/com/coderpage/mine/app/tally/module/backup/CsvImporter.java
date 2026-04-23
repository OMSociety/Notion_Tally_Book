package com.coderpage.mine.app.tally.module.backup;

import android.content.Context;
import android.util.Log;

import com.coderpage.mine.app.tally.persistence.model.CategoryModel;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.entity.CategoryEntity;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * CSV 导入工具类
 * 兼容导出格式：
 * 1. 当前版本导出：时间,分类,金额,类型,备注
 * 2. 旧版本/原项目常见导出：时间,分类,金额,备注（无类型列）
 * 
 * @author Flandre Scarlet
 */
public class CsvImporter {

    private static final String TAG = "CsvImporter";
    private static final int TYPE_UNKNOWN = -1;

    /**
     * 从 CSV 文件导入记录
     */
    public static ImportResult importFromCsv(Context context, File file) {
        ImportResult result = new ImportResult();
        if (file == null || !file.exists() || !file.isFile()) {
            result.success = false;
            result.message = "CSV 文件不存在";
            return result;
        }

        List<RecordEntity> records = new ArrayList<>();
        TallyDatabase database = TallyDatabase.getInstance();
        CategoryResolver categoryResolver = new CategoryResolver(database.categoryDao().allCategory());
        
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            
            String line;
            boolean firstDataLineChecked = false;
            Map<String, Integer> headerIndexMap = null;
            int lineNumber = 0;
            int successCount = 0;
            int failCount = 0;
            int totalDataCount = 0;
            
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

                List<String> fields = parseCsvFields(line);
                if (isAllEmpty(fields)) {
                    continue;
                }

                if (!firstDataLineChecked) {
                    firstDataLineChecked = true;
                    headerIndexMap = tryParseHeader(fields);
                    if (headerIndexMap != null) {
                        continue;
                    }
                }

                totalDataCount++;
                RecordEntity record = parseLine(fields, headerIndexMap, categoryResolver);
                if (record != null) {
                    records.add(record);
                    successCount++;
                } else {
                    failCount++;
                    Log.w(TAG, "CSV 第 " + lineNumber + " 行解析失败");
                }
            }
            
            // 保存到数据库
            if (!records.isEmpty()) {
                RecordEntity[] insertArray = records.toArray(new RecordEntity[0]);
                database.recordDao().insert(insertArray);
            }
            
            result.success = true;
            result.totalCount = totalDataCount;
            result.successCount = successCount;
            result.failCount = failCount;
            result.message = "成功导入 " + successCount + " 条记录，失败 " + failCount + " 条";
            
            Log.d(TAG, "CSV 导入完成: 总计=" + totalDataCount + ", 成功=" + successCount + ", 失败=" + failCount);
            
        } catch (Exception e) {
            Log.e(TAG, "CSV 导入失败", e);
            result.success = false;
            result.message = "导入失败: " + e.getMessage();
        }
        
        return result;
    }

    private static RecordEntity parseLine(List<String> fields,
                                          Map<String, Integer> headerIndexMap,
                                          CategoryResolver categoryResolver) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }

        String timeField = pickField(fields, headerIndexMap, "time");
        String categoryField = pickField(fields, headerIndexMap, "category");
        String amountField = pickField(fields, headerIndexMap, "amount");
        String typeField = pickField(fields, headerIndexMap, "type");
        String descField = pickField(fields, headerIndexMap, "desc");

        // 无表头时按常见顺序兼容：时间,分类,金额,类型,备注 或 时间,分类,金额,备注
        if (headerIndexMap == null) {
            timeField = getField(fields, 0);
            categoryField = getField(fields, 1);
            amountField = getField(fields, 2);
            if (fields.size() >= 5) {
                typeField = getField(fields, 3);
                descField = getField(fields, 4);
            } else if (fields.size() >= 4) {
                descField = getField(fields, 3);
            }
        }

        if (isBlank(amountField) && fields.size() >= 3) {
            amountField = findPossibleAmountField(fields);
        }

        double amountWithSign = parseAmountWithSign(amountField);
        if (amountWithSign == 0) {
            return null;
        }

        String categoryName = safeTrim(categoryField);
        int type = parseType(typeField);
        if (type == TYPE_UNKNOWN) {
            if (amountWithSign < 0) {
                type = RecordEntity.TYPE_EXPENSE;
            } else {
                int inferredType = categoryResolver.inferTypeByCategoryName(categoryName);
                type = inferredType == TYPE_UNKNOWN ? RecordEntity.TYPE_EXPENSE : inferredType;
            }
        }

        String categoryUniqueName = categoryResolver.resolve(categoryName, type);
        if (isBlank(categoryUniqueName)) {
            categoryUniqueName = categoryResolver.defaultByType(type);
        }

        long time = parseDateTime(timeField);
        if (time <= 0) {
            time = System.currentTimeMillis();
        }

        RecordEntity record = new RecordEntity();
        record.setTime(time);
        record.setAmount(Math.abs(amountWithSign));
        record.setType(type);
        record.setCategoryUniqueName(isBlank(categoryUniqueName) ? "" : categoryUniqueName);
        record.setDesc(isBlank(descField) ? "" : descField.trim());
        record.setSyncId("csv:" + UUID.randomUUID().toString());
        return record;
    }

    private static Map<String, Integer> tryParseHeader(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            String key = normalizeHeader(fields.get(i));
            if (isBlank(key)) {
                continue;
            }
            if (isTimeHeader(key)) {
                indexMap.put("time", i);
            } else if (isCategoryHeader(key)) {
                indexMap.put("category", i);
            } else if (isAmountHeader(key)) {
                indexMap.put("amount", i);
            } else if (isTypeHeader(key)) {
                indexMap.put("type", i);
            } else if (isDescHeader(key)) {
                indexMap.put("desc", i);
            }
        }
        return indexMap.isEmpty() ? null : indexMap;
    }

    private static boolean isTimeHeader(String key) {
        return "时间".equals(key) || "日期".equals(key) || "date".equals(key)
                || "datetime".equals(key) || "time".equals(key);
    }

    private static boolean isCategoryHeader(String key) {
        return "分类".equals(key) || "category".equals(key) || "cate".equals(key);
    }

    private static boolean isAmountHeader(String key) {
        return "金额".equals(key) || "money".equals(key) || "amount".equals(key)
                || "price".equals(key) || "sum".equals(key);
    }

    private static boolean isTypeHeader(String key) {
        return "类型".equals(key) || "type".equals(key) || "收支".equals(key);
    }

    private static boolean isDescHeader(String key) {
        return "备注".equals(key) || "remark".equals(key) || "desc".equals(key)
                || "description".equals(key) || "note".equals(key) || "说明".equals(key);
    }

    private static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace("\uFEFF", "")
                .replace("（", "(")
                .replace("）", ")")
                .replaceAll("\\s", "")
                .toLowerCase(Locale.ROOT);
    }

    private static boolean isAllEmpty(List<String> fields) {
        for (String field : fields) {
            if (!isBlank(field)) {
                return false;
            }
        }
        return true;
    }

    private static String pickField(List<String> fields, Map<String, Integer> indexMap, String key) {
        if (indexMap == null) {
            return null;
        }
        Integer index = indexMap.get(key);
        if (index == null) {
            return null;
        }
        return getField(fields, index);
    }

    private static String getField(List<String> fields, int index) {
        if (index < 0 || index >= fields.size()) {
            return null;
        }
        return fields.get(index);
    }

    private static String findPossibleAmountField(List<String> fields) {
        for (String field : fields) {
            if (parseAmountWithSign(field) != 0) {
                return field;
            }
        }
        return null;
    }

    private static int parseType(String str) {
        if (isBlank(str)) {
            return TYPE_UNKNOWN;
        }
        String lower = str.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("收入") || "income".equals(lower)) {
            return RecordEntity.TYPE_INCOME;
        }
        if (lower.contains("支出") || "expense".equals(lower)) {
            return RecordEntity.TYPE_EXPENSE;
        }
        return TYPE_UNKNOWN;
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
            "yyyy.MM.dd",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy.MM.dd HH:mm:ss",
            "yyyy年MM月dd日 HH:mm",
            "yyyy年MM月dd日"
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
    private static double parseAmountWithSign(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        
        // 移除货币符号、千分位分隔符和空格
        str = str.trim()
                .replaceAll("[¥$€£]", "")
                .replaceAll(",", "")
                .replaceAll("\\s", "");
        
        // 保留数字、小数点和负号（用于判断正负）
        str = str.replaceAll("[^\\d.-]", "");
        
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static class CategoryResolver {
        private final Map<String, String> expenseByName = new HashMap<>();
        private final Map<String, String> incomeByName = new HashMap<>();
        private String defaultExpenseUniqueName = "";
        private String defaultIncomeUniqueName = "";

        CategoryResolver(List<CategoryModel> categories) {
            if (categories == null) {
                return;
            }
            for (CategoryModel category : categories) {
                String key = normalizeCategoryName(category.getName());
                if (CategoryEntity.TYPE_INCOME == category.getType()) {
                    if (!isBlank(key) && !incomeByName.containsKey(key)) {
                        incomeByName.put(key, category.getUniqueName());
                    }
                    if (isBlank(defaultIncomeUniqueName)) {
                        defaultIncomeUniqueName = category.getUniqueName();
                    }
                } else {
                    if (!isBlank(key) && !expenseByName.containsKey(key)) {
                        expenseByName.put(key, category.getUniqueName());
                    }
                    if (isBlank(defaultExpenseUniqueName)) {
                        defaultExpenseUniqueName = category.getUniqueName();
                    }
                }
            }
        }

        String resolve(String categoryName, int type) {
            String key = normalizeCategoryName(categoryName);
            if (isBlank(key)) {
                return defaultByType(type);
            }
            if (type == CategoryEntity.TYPE_INCOME && incomeByName.containsKey(key)) {
                return incomeByName.get(key);
            }
            if (type == CategoryEntity.TYPE_EXPENSE && expenseByName.containsKey(key)) {
                return expenseByName.get(key);
            }
            // 类型匹配不到时跨类型兜底，兼容旧数据
            if (incomeByName.containsKey(key)) {
                return incomeByName.get(key);
            }
            if (expenseByName.containsKey(key)) {
                return expenseByName.get(key);
            }
            return defaultByType(type);
        }

        String defaultByType(int type) {
            if (type == CategoryEntity.TYPE_INCOME && !isBlank(defaultIncomeUniqueName)) {
                return defaultIncomeUniqueName;
            }
            if (type == CategoryEntity.TYPE_EXPENSE && !isBlank(defaultExpenseUniqueName)) {
                return defaultExpenseUniqueName;
            }
            return !isBlank(defaultExpenseUniqueName) ? defaultExpenseUniqueName : defaultIncomeUniqueName;
        }

        int inferTypeByCategoryName(String categoryName) {
            String key = normalizeCategoryName(categoryName);
            if (isBlank(key)) {
                return TYPE_UNKNOWN;
            }
            if (incomeByName.containsKey(key) && !expenseByName.containsKey(key)) {
                return CategoryEntity.TYPE_INCOME;
            }
            if (expenseByName.containsKey(key) && !incomeByName.containsKey(key)) {
                return CategoryEntity.TYPE_EXPENSE;
            }
            return TYPE_UNKNOWN;
        }

        private String normalizeCategoryName(String value) {
            if (value == null) {
                return "";
            }
            return value.trim().toLowerCase(Locale.ROOT);
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

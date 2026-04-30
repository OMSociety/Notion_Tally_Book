package com.coderpage.mine.app.tally.module.backup;

import static com.coderpage.base.utils.LogUtils.LOGE;
import static com.coderpage.base.utils.LogUtils.makeLogTag;
import static com.coderpage.base.utils.UIUtils.showToastShort;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.coderpage.base.common.Callback;
import com.coderpage.base.common.IError;
import com.coderpage.base.common.NonThrowError;
import com.coderpage.base.common.SimpleCallback;
import com.coderpage.base.utils.ArrayUtils;
import com.coderpage.base.utils.CommonUtils;
import com.coderpage.base.utils.LogUtils;
import com.coderpage.concurrency.AsyncTaskExecutor;
import com.coderpage.mine.BuildConfig;
import com.coderpage.mine.MineApp;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.error.ErrorCode;
import com.coderpage.mine.app.tally.persistence.model.CategoryModel;
import com.coderpage.mine.app.tally.persistence.model.Record;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.persistence.sql.dao.CategoryDao;
import com.coderpage.mine.app.tally.persistence.sql.entity.CategoryEntity;
import com.coderpage.mine.app.tally.persistence.sql.entity.RecordEntity;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * @author abner-l. 2017-06-01
 * @since 0.4.0
 */

public class Backup {

    private static final String TAG = makeLogTag(Backup.class);

    /**
     * 备份过程回调；
     * 回调包括 {@link BackupProgress#READ_DATA} {@link BackupProgress#WRITE_FILE}
     */
    public interface BackupProgressListener extends Callback<Void, IError> {
        /**
         * 回到
         *
         * @param backupProgress progress
         */
        void onProgressUpdate(BackupProgress backupProgress);
    }

    public enum BackupProgress {
        // 读取数据
        READ_DATA,
        // 写入备份文件
        WRITE_FILE,
    }




    /**
     * 恢复文件过程回调；
     */
    public interface RestoreProgressListener extends Callback<BackupModel, IError> {
        /**
         * 更新回调
         *
         * @param restoreProgress progress
         */
        void onProgressUpdate(RestoreProgress restoreProgress);
    }

    public enum RestoreProgress {
        // 读取文件
        READ_FILE,
        // 检查文件格式
        CHECK_FILE_FORMAT,
        // 恢复文件到数据库
        RESTORE_TO_DB
    }

    /**
     * 备份消费记录到 JSON 文件中；
     *
     * @param context  {@link Context}
     * @param listener 备份回调
     */
    public static void backupToJsonFile(Context context, BackupProgressListener listener) {
        AsyncTaskExecutor.execute(() -> {
            listener.onProgressUpdate(BackupProgress.READ_DATA);
            BackupModel backupModel = readData();

            listener.onProgressUpdate(BackupProgress.WRITE_FILE);
            new BackupCache(context).backup2JsonFile(backupModel, listener);
        });
    }


    /**
     * 同步备份数据到 JSON 文件
     *
     * @param context  {@link Context}
     * * @param listener 备份回调
     * @return 备份文件
     */
    public static File backupToJsonFileSync(Context context, BackupProgressListener listener) {
        listener.onProgressUpdate(BackupProgress.READ_DATA);
        BackupModel backupModel = readData();

        listener.onProgressUpdate(BackupProgress.WRITE_FILE);
        return new BackupCache(context).backup2JsonFileSync(backupModel, listener);
    }

    /**
     * 读取备份的 JSON 文件。
     *
     * @param file     {@link File}备份文件
     * @param listener 回调
     */
    public static void readBackupJsonFile(File file, RestoreProgressListener listener) {
        AsyncTaskExecutor.execute(() -> {
            listener.onProgressUpdate(RestoreProgress.READ_FILE);
            if (file == null) {
                ((SimpleCallback<BackupModel>) listener).failure(new NonThrowError(ErrorCode.INTERNAL_ERR, "File is null"));
                return;
            }

            LogUtils.LOGD(TAG,"Read backup json file: " + file.getAbsolutePath());

            if (!file.exists()) {
                ((SimpleCallback<BackupModel>) listener).failure(new NonThrowError(ErrorCode.ILLEGAL_ARGS, "File not exist"));
                return;
            }
            if (file.isDirectory()) {
                ((SimpleCallback<BackupModel>) listener).failure(new NonThrowError(ErrorCode.ILLEGAL_ARGS, "Illegal file type"));
                return;
            }

            String sourceString = null;
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader inputStreamReader = new InputStreamReader(fis);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                String line;
                StringBuilder sourceBuilder = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null) {
                    sourceBuilder.append(line);
                }
                sourceString = sourceBuilder.toString();
            } catch (FileNotFoundException e) {
                LOGE(TAG, "File not found", e);
                ((SimpleCallback<BackupModel>) listener).failure(new NonThrowError(ErrorCode.ILLEGAL_ARGS, "File not found"));
                return;
            } catch (IOException e) {
                LOGE(TAG, "IO Err", e);
                ((SimpleCallback<BackupModel>) listener).failure(new NonThrowError(ErrorCode.INTERNAL_ERR, "File io err"));
                return;
            }

            listener.onProgressUpdate(RestoreProgress.CHECK_FILE_FORMAT);
            try {
                BackupModel backupModel = JSON.parseObject(sourceString, BackupModel.class);
                listener.success(backupModel);
            } catch (Exception e) {
                LOGE(TAG, "Parse json err", e);
                ((SimpleCallback<BackupModel>) listener).failure(new NonThrowError(ErrorCode.INTERNAL_ERR, "not a json file"));
            }
        });
    }

    /**
     * 将备份的数据恢复到数据库
     *
     * @param context     {@link Context}
     * @param backupModel {@link BackupModel} 备份的数据
     * @param listener    恢复到数据库的回调
     */
    public static void restoreDataFromBackupData(Context context,
                                                 BackupModel backupModel,
                                                 RestoreProgressListener listener) {
        AsyncTaskExecutor.execute(() -> {
            listener.onProgressUpdate(RestoreProgress.RESTORE_TO_DB);

            BackupModelMetadata metadata = backupModel.getMetadata();
            // 恢复分类表数据
            List<BackupModelCategory> categoryList = backupModel.getCategoryList();
            if (categoryList != null && !categoryList.isEmpty()) {
                boolean restoreCategoryOk = restoreCategoryTable(metadata, categoryList);
                if (!restoreCategoryOk) {
                    ((SimpleCallback<BackupModel>) listener).failure(new NonThrowError(ErrorCode.SQL_ERR, "恢复分类数据失败"));
                    return;
                }
            }

            // 恢复消费表数据
            List<BackupModelRecord> expenseList = backupModel.getExpenseList();
            if (expenseList != null && !expenseList.isEmpty()) {
                boolean restoreExpenseOk = restoreExpenseTable(metadata, expenseList);
                if (!restoreExpenseOk) {
                    ((SimpleCallback<BackupModel>) listener).failure(new NonThrowError(ErrorCode.SQL_ERR, "恢复消费数据失败"));
                    return;
                }
            }

            listener.success(backupModel);
        });
    }

    /**
     * 读取默认备份文件目录中所有的备份文件。
     *
     * @param context {@link Context}
     * @return 默认备份文件存放目录中的所有备份文件
     */
    public static List<File> listBackupFiles(Context context) {
        return new BackupCache(context).listBackupFiles();
    }

    private static boolean restoreCategoryTable(BackupModelMetadata metadata,
                                                List<BackupModelCategory> categoryList) {
        CategoryDao categoryDao = TallyDatabase.getInstance().categoryDao();
        List<CategoryModel> currentExistCategoryList = categoryDao.allCategory();

        List<CategoryEntity> entityList = new ArrayList<>();
        for (int i = 0; i < categoryList.size(); i++) {
            BackupModelCategory backupCategory = categoryList.get(i);
            CategoryEntity entity = new CategoryEntity();
            entity.setName(backupCategory.getName());
            entity.setIcon(backupCategory.getIcon());
            entity.setAccountId(backupCategory.getAccountId());
            entity.setSyncStatus(backupCategory.getSyncStatus());
            // 0.6.0 版本之前没有 type 之分，全部为支出分类类型
            entity.setType(metadata.getClientVersionCode() < 60 ?
                    CategoryEntity.TYPE_EXPENSE : backupCategory.getType());
            // 0.6.0 版本之前没有 uniqueCategoryName，全部统一使用 category icon
            entity.setUniqueName(TextUtils.isEmpty(backupCategory.getUniqueName()) ?
                    backupCategory.getIcon() : backupCategory.getUniqueName());

            boolean alreadyContains = ArrayUtils.contains(currentExistCategoryList, item -> {
                return CommonUtils.isEqual(entity.getUniqueName(), item.getUniqueName())
                        && entity.getAccountId() == item.getAccountId();
            });
            if (alreadyContains) {
                continue;
            }
            entityList.add(entity);
        }

        if (entityList.isEmpty()) {
            return true;
        }

        CategoryEntity[] insertArray = new CategoryEntity[entityList.size()];
        ArrayUtils.forEach(entityList, (count, index, item) -> {
            insertArray[index] = item;
        });
        try {
            categoryDao.insert(insertArray);
            return true;
        } catch (Exception e) {
            LOGE(TAG, "恢复数据失败-分类表", e);
        }

        return false;
    }

    private static boolean restoreExpenseTable(BackupModelMetadata metadata,
                                               List<BackupModelRecord> expenseList) {
        // 0.6.0 版本之前备份的数据。单独处理
        if (metadata.getClientVersionCode() < 60) {
            return restoreExpenseTableBefore060(expenseList);
        }

        TallyDatabase database = TallyDatabase.getInstance();
        RecordEntity[] insertArray = new RecordEntity[expenseList.size()];
        for (int i = 0; i < expenseList.size(); i++) {
            BackupModelRecord backupExpense = expenseList.get(i);

            RecordEntity entity = new RecordEntity();
            entity.setAccountId(backupExpense.getAccountId());
            entity.setAmount(BigDecimal.valueOf(backupExpense.getAmount()));
            entity.setTime(backupExpense.getTime());
            entity.setCategoryUniqueName(backupExpense.getCategoryUniqueName());
            entity.setDesc(backupExpense.getDesc());
            entity.setSyncId(backupExpense.getSyncId());
            entity.setSyncStatus(backupExpense.getSyncStatus());
            entity.setType(backupExpense.getType());

            insertArray[i] = entity;
        }

        try {
            database.recordDao().insert(insertArray);
            return true;
        } catch (Exception e) {
            LOGE(TAG, "恢复数据失败-消费记录表", e);
        }

        return false;
    }

    /**
     * 恢复 0.6.0 版本之前的备份数据
     *
     * @param recordList 记录列表
     * @return 恢复结果
     */
    private static boolean restoreExpenseTableBefore060(List<BackupModelRecord> recordList) {
        TallyDatabase database = TallyDatabase.getInstance();
        List<CategoryModel> expenseCategoryList = database.categoryDao().allExpenseCategory();

        // categoryName - categoryUniqueName Map
        HashMap<String, String> getCategoryUniqueNameByName = new HashMap<>();
        for (CategoryModel category : expenseCategoryList) {
            getCategoryUniqueNameByName.put(category.getName(), category.getUniqueName());
        }

        RecordEntity[] insertArray = new RecordEntity[recordList.size()];
        for (int i = 0; i < recordList.size(); i++) {
            BackupModelRecord backupExpense = recordList.get(i);

            // 0.6.0 版本之前，不支持收入类型的记录，不支持修改分类名称
            // 可以通过"分类名称"来获取对应的 categoryUniqueName
            String categoryUniqueName = getCategoryUniqueNameByName.get(backupExpense.getCategory());
            if (categoryUniqueName == null || categoryUniqueName.isEmpty()) {
                categoryUniqueName = backupExpense.getCategory() != null
                        ? backupExpense.getCategory() : "";
            }

            RecordEntity entity = new RecordEntity();
            entity.setAccountId(backupExpense.getAccountId());
            entity.setAmount(BigDecimal.valueOf(backupExpense.getAmount()));
            entity.setTime(backupExpense.getTime());
            entity.setCategoryUniqueName(categoryUniqueName);
            entity.setDesc(backupExpense.getDesc());
            entity.setSyncId(backupExpense.getSyncId());
            entity.setSyncStatus(backupExpense.getSyncStatus());
            entity.setType(backupExpense.getType());

            insertArray[i] = entity;
        }

        try {
            database.recordDao().insert(insertArray);
            return true;
        } catch (Exception e) {
            LOGE(TAG, "恢复数据失败-消费记录表", e);
        }

        return false;
    }

    /**
     * 读取数据库数据并格式化为{@link BackupModel}
     *
     * @return 返回从数据库读取的所有数据
     */
    private static BackupModel readData() {

        List<BackupModelCategory> categoryList = new ArrayList<>();
        List<BackupModelRecord> recordList = null;
        BackupModelMetadata metadata = new BackupModelMetadata();

        TallyDatabase database = TallyDatabase.getInstance();

        List<CategoryModel> categoryEntityList = database.categoryDao().allCategory();
        for (CategoryModel entity : categoryEntityList) {
            BackupModelCategory category = new BackupModelCategory();
            category.setName(entity.getName());
            category.setUniqueName(entity.getUniqueName());
            category.setIcon(entity.getIcon());
            category.setAccountId(entity.getAccountId());
            category.setType(entity.getType());
            category.setSyncStatus(entity.getSyncStatus());

            categoryList.add(category);
        }

        List<Record> recordEntityList = database.recordDao().queryAll();
        recordList = new ArrayList<>(recordEntityList.size());
        for (Record entity : recordEntityList) {
            BackupModelRecord expense = new BackupModelRecord();
            expense.setAmount(entity.getAmount() != null ? entity.getAmount().doubleValue() : 0.0);
            expense.setDesc(entity.getDesc());
            expense.setCategory(entity.getCategoryName());
            expense.setTime(entity.getTime());
            expense.setSyncId(entity.getSyncId());
            expense.setAccountId(entity.getAccountId());
            expense.setSyncStatus(entity.getSyncStatus());
            expense.setCategoryUniqueName(entity.getCategoryUniqueName());
            expense.setType(entity.getType());

            recordList.add(expense);
        }

        metadata.setBackupDate(System.currentTimeMillis());
        metadata.setDeviceName(Build.MODEL);
        metadata.setClientVersion(BuildConfig.VERSION_NAME);
        metadata.setClientVersionCode(BuildConfig.VERSION_CODE);
        metadata.setExpenseNumber(recordList.size());

        BackupModel backupModel = new BackupModel();

        backupModel.setMetadata(metadata);
        backupModel.setCategoryList(categoryList);
        backupModel.setExpenseList(recordList);

        return backupModel;
    }

    /**
     * 执行实际的数据导出操作
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param folder 导出文件夹路径
     * @param formatId 格式ID
     */
    public void performExport(Long startDate, Long endDate, String folder, int formatId) {
        performExport(startDate, endDate, folder, formatId, null);
    }

    /**
     * 执行实际的数据导出操作
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param folder 导出文件夹路径
     * @param formatId 格式ID
     * @param onComplete 导出完成回调（在后台线程调用）
     */
    public void performExport(Long startDate, Long endDate, String folder, int formatId,
                              SimpleCallback<Void> onComplete) {
        TallyDatabase database = TallyDatabase.getInstance();
        AsyncTaskExecutor.execute(() -> {

            //查询数据
            List<Record> records = database.recordDao().queryAllBetweenTimeTimeDesc(startDate, endDate);
            if (records == null || records.size() == 0) {
                String err = "导出失败: 没有查询到账单记录";
                exportTips(MineApp.getAppContext(), err);
                if (onComplete != null) onComplete.failure(
                        new NonThrowError(ErrorCode.ILLEGAL_ARGS, err));
                return;
            }

            //创建文件
            String dateFormatted = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().getTime());
            String format = formatId == R.id.rbCsv ? ".csv" : ".xls";
            String fileName = "bill_export"+ "_" + dateFormatted + format;
            File file = new File(folder, fileName);
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (onComplete != null) onComplete.failure(
                        new NonThrowError(ErrorCode.UNKNOWN, "创建文件失败: " + e.getMessage()));
                return;
            }

            //写出数据
            boolean writeOk;
            if (formatId == R.id.rbCsv) {
                writeOk = writeCsvFile(file, records);
            } else {
                writeOk = writeExcelFile(file, records);
            }
            if (!writeOk) {
                String err = "导出失败: 文件写入错误";
                exportTips(MineApp.getAppContext(), err);
                if (onComplete != null) onComplete.failure(
                        new NonThrowError(ErrorCode.UNKNOWN, err));
                return;
            }
            exportTips(MineApp.getAppContext(), "导出成功");
            if (onComplete != null) onComplete.success(null);
        });
    }

    /**
     * 通过 SAF 导出数据到用户选择的 URI
     * @param context 上下文
     * @param uri 用户通过 SAF 选择的目标文件 URI
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param formatId 格式ID
     * @param onComplete 导出完成回调
     */
    public void performExportToUri(Context context, Uri uri, Long startDate, Long endDate,
                                    int formatId, SimpleCallback<Void> onComplete) {
        TallyDatabase database = TallyDatabase.getInstance();
        AsyncTaskExecutor.execute(() -> {
            List<Record> records = database.recordDao().queryAllBetweenTimeTimeDesc(startDate, endDate);
            if (records == null || records.size() == 0) {
                String err = "导出失败: 没有查询到账单记录";
                exportTips(context, err);
                if (onComplete != null) onComplete.failure(
                        new NonThrowError(ErrorCode.ILLEGAL_ARGS, err));
                return;
            }

            try {
                ContentResolver resolver = context.getContentResolver();
                ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "w");
                if (pfd == null) {
                    if (onComplete != null) onComplete.failure(
                            new NonThrowError(ErrorCode.UNKNOWN, "无法打开目标文件"));
                    return;
                }
                java.io.FileOutputStream fos = new java.io.FileOutputStream(pfd.getFileDescriptor());
                boolean writeOk;
                if (formatId == R.id.rbCsv) {
                    writeOk = writeCsvToStream(fos, records);
                } else {
                    writeOk = writeExcelToStream(fos, records);
                }
                fos.close();
                pfd.close();

                if (!writeOk) {
                    String err = "导出失败: 文件写入错误";
                    exportTips(context, err);
                    if (onComplete != null) onComplete.failure(
                            new NonThrowError(ErrorCode.UNKNOWN, err));
                    return;
                }
                exportTips(context, "导出成功");
                if (onComplete != null) onComplete.success(null);
            } catch (IOException e) {
                e.printStackTrace();
                if (onComplete != null) onComplete.failure(
                        new NonThrowError(ErrorCode.UNKNOWN, "导出失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 将记录写入CSV文件
     * @param file 目标文件
     * @param records 记录列表
     * @return 写入是否成功
     */
    private boolean writeCsvFile(File file, List<Record> records) {
        try (java.io.FileWriter fileWriter = new java.io.FileWriter(file)) {
            // UTF-8 BOM，确保 Excel 正确识别中文
            fileWriter.append("\uFEFF");
            // 写入CSV头部
            fileWriter.append("时间,分类,金额,类型,备注\n");

            // 写入数据行
            for (Record record : records) {
                // 格式化时间
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(new java.util.Date(record.getTime()));

                // 获取类型文本
                String type = record.getType() == Record.TYPE_EXPENSE ? "支出" : "收入";

                // 写入一行数据（对分类和备注做 CSV 注入防护）
                fileWriter.append("\"")
                        .append(time)
                        .append("\"")
                        .append(",")
                        .append("\"")
                        .append(sanitizeCsvField(record.getCategoryName()))
                        .append("\"")
                        .append(",")
                        .append("\"")
                        .append(String.valueOf(record.getAmount()))
                        .append("\"")
                        .append(",")
                        .append("\"")
                        .append(type)
                        .append("\"")
                        .append(",")
                        .append("\"")
                        .append(sanitizeCsvField(record.getDesc()))
                        .append("\"")
                        .append("\n");
            }

            fileWriter.flush();
            return true;
        } catch (IOException e) {
            if (file.exists()) {
                file.delete();
            }
            return false;
        }
    }

    /**
     * 防止 CSV 注入：如果字段以 =、+、-、@ 开头（可能被 Excel 解释为公式），
     * 在前面加单引号强制作为文本处理。
     */
    private static String sanitizeCsvField(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        // 转义双引号，防止 CSV 字段逃逸
        String escaped = value.replace("\"", "\"\"");
        char first = escaped.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + escaped;
        }
        return escaped;
    }

    private boolean writeExcelFile(File file, List<Record> records) {
        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("账单明细");

            // 创建标题行（不使用CellStyle避免AWT依赖）
            Row headerRow = sheet.createRow(0);
            String[] headers = {"时间", "分类", "金额", "类型", "备注"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                // 不设置CellStyle以避免AWT依赖
            }

            // 填充数据
            for (int i = 0; i < records.size(); i++) {
                Record record = records.get(i);
                Row row = sheet.createRow(i + 1);

                // 时间
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(new java.util.Date(record.getTime()));
                row.createCell(0).setCellValue(time);

                // 分类
                row.createCell(1).setCellValue(record.getCategoryName() != null ? record.getCategoryName() : "");

                // 金额
                row.createCell(2).setCellValue(record.getAmount() != null ? record.getAmount().doubleValue() : 0.0);

                // 类型
                String type = record.getType() == Record.TYPE_EXPENSE ? "支出" : "收入";
                row.createCell(3).setCellValue(type);

                // 备注
                row.createCell(4).setCellValue(record.getDesc() != null ? record.getDesc() : "");
            }

            // 设置默认列宽，避免使用autoSizeColumn触发AWT依赖
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256); // 设置列宽为20个字符宽度
            }

            // 写入文件
            try (java.io.FileOutputStream fileOut = new java.io.FileOutputStream(file)) {
                workbook.write(fileOut);
            }
            return true;
        } catch (Exception e) {
            if (file.exists()) {
                file.delete();
            }
            return false;
        }
    }

    /**
     * 将记录写入CSV输出流
     */
    private boolean writeCsvToStream(java.io.OutputStream outputStream, List<Record> records) {
        try (java.io.Writer writer = new java.io.OutputStreamWriter(outputStream, "UTF-8")) {
            writer.append("﻿");
            writer.append("时间,分类,金额,类型,备注\n");
            for (Record record : records) {
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(new java.util.Date(record.getTime()));
                String type = record.getType() == Record.TYPE_EXPENSE ? "支出" : "收入";
                writer.append("\"").append(time).append("\"")
                        .append(",").append("\"").append(sanitizeCsvField(record.getCategoryName())).append("\"")
                        .append(",").append("\"").append(String.valueOf(record.getAmount())).append("\"")
                        .append(",").append("\"").append(type).append("\"")
                        .append(",").append("\"").append(sanitizeCsvField(record.getDesc())).append("\"")
                        .append("\n");
            }
            writer.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 将记录写入Excel输出流
     */
    private boolean writeExcelToStream(java.io.OutputStream outputStream, List<Record> records) {
        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("账单明细");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"时间", "分类", "金额", "类型", "备注"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            for (int i = 0; i < records.size(); i++) {
                Record record = records.get(i);
                Row row = sheet.createRow(i + 1);
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(new java.util.Date(record.getTime()));
                row.createCell(0).setCellValue(time);
                row.createCell(1).setCellValue(record.getCategoryName() != null ? record.getCategoryName() : "");
                row.createCell(2).setCellValue(record.getAmount() != null ? record.getAmount().doubleValue() : 0.0);
                String type = record.getType() == Record.TYPE_EXPENSE ? "支出" : "收入";
                row.createCell(3).setCellValue(type);
                row.createCell(4).setCellValue(record.getDesc() != null ? record.getDesc() : "");
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            workbook.write(outputStream);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void exportTips(Context context, String msg){
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        });
    }

}

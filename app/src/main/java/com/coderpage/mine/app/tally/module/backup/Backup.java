package com.coderpage.mine.app.tally.module.backup;



import static com.coderpage.base.utils.UIUtils.showToastShort;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.coderpage.base.common.Callback;
import com.coderpage.base.common.IError;
import com.coderpage.base.common.NonThrowError;
import com.coderpage.base.utils.ArrayUtils;
import com.coderpage.base.utils.CommonUtils;
import timber.log.Timber;
import com.coderpage.concurrency.AsyncTaskExecutor;
import com.coderpage.mine.BuildConfig;
import com.coderpage.mine.MineApp;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.error.ErrorCode;
import com.coderpage.mine.app.tally.persistence.model.CategoryModel;
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

    private static final String TAG = "Backup";

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
                listener.failure(new NonThrowError(ErrorCode.INTERNAL_ERR, "File is null"));
                return;
            }

            Timber.tag(TAG).d("Read backup json file: " + file.getAbsolutePath());

            if (!file.exists()) {
                listener.failure(new NonThrowError(ErrorCode.ILLEGAL_ARGS, "File not exist"));
                return;
            }
            if (file.isDirectory()) {
                listener.failure(new NonThrowError(ErrorCode.ILLEGAL_ARGS, "Illegal file type"));
                return;
            }

            FileInputStream fis;
            try {
                fis = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                LOGE(TAG, "File not found", e);
                listener.failure(new NonThrowError(ErrorCode.ILLEGAL_ARGS, "File not found"));
                return;
            }

            InputStreamReader inputStreamReader = null;
            BufferedReader bufferedReader = null;
            String sourceString = null;
            try {
                inputStreamReader = new InputStreamReader(fis);
                bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                StringBuilder sourceBuilder = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null) {
                    sourceBuilder.append(line);
                }
                sourceString = sourceBuilder.toString();
            } catch (IOException e) {
                LOGE(TAG, "IO Err", e);
                listener.failure(new NonThrowError(ErrorCode.INTERNAL_ERR, "File io err"));
                return;
            } finally {
                try {
                    bufferedReader.close();
                    inputStreamReader.close();
                } catch (IOException e) {
                    // no-op
                }
            }

            listener.onProgressUpdate(RestoreProgress.CHECK_FILE_FORMAT);
            try {
                BackupModel backupModel = JSON.parseObject(sourceString, BackupModel.class);
                listener.success(backupModel);
            } catch (Exception e) {
                LOGE(TAG, "Parse json err", e);
                listener.failure(new NonThrowError(ErrorCode.INTERNAL_ERR, "not a json file"));
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
                    listener.failure(new NonThrowError(ErrorCode.SQL_ERR, "恢复分类数据失败"));
                }
            }

            // 恢复消费表数据
            List<BackupModelRecord> expenseList = backupModel.getExpenseList();
            if (expenseList != null && !expenseList.isEmpty()) {
                boolean restoreExpenseOk = restoreExpenseTable(metadata, expenseList);
                if (!restoreExpenseOk) {
                    listener.failure(new NonThrowError(ErrorCode.SQL_ERR, "恢复消费数据失败"));
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
            entity.setAmount(backupExpense.getAmount());
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

            RecordEntity entity = new RecordEntity();
            entity.setAccountId(backupExpense.getAccountId());
            entity.setAmount(backupExpense.getAmount());
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

        List<RecordEntity> recordEntityList = database.recordDao().queryAll();
        recordList = new ArrayList<>(recordEntityList.size());
        for (RecordEntity entity : recordEntityList) {
            BackupModelRecord expense = new BackupModelRecord();
            expense.setAmount(entity.getAmount());
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
        TallyDatabase database = TallyDatabase.getInstance();
        AsyncTaskExecutor.execute(() -> {

            //查询数据
            List<RecordEntity> records = database.recordDao().queryAllBetweenTimeTimeDesc(startDate, endDate);
            if (records == null || records.size() == 0) {
                exportTips(MineApp.getAppContext(), "导出失败: 没有查询到账单记录");
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
                //提示用户创建文件报错
                e.printStackTrace();
                return;
            }

            //写出数据
            if (formatId == R.id.rbCsv) {
                writeCsvFile(file, records);
            } else {
                writeExcelFile(file, records);
            }
            exportTips(MineApp.getAppContext(), "导出成功");
        });
    }

    /**
     * 将记录写入CSV文件
     * @param file 目标文件
     * @param records 记录列表
     */
    private void writeCsvFile(File file, List<RecordEntity> records) {
        try {
            java.io.FileWriter fileWriter = new java.io.FileWriter(file);
            // 写入CSV头部
            fileWriter.append("时间,分类,金额,类型,备注\n");

            // 写入数据行
            for (RecordEntity record : records) {
                // 格式化时间
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(new java.util.Date(record.getTime()));

                // 获取类型文本
                String type = record.getType() == RecordEntity.TYPE_EXPENSE ? "支出" : "收入";

                // 写入一行数据
                fileWriter.append("\"")
                        .append(time)
                        .append("\"")
                        .append(",")
                        .append("\"")
                        .append(record.getCategoryName() != null ? record.getCategoryName() : "")
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
                        .append(record.getDesc() != null ? record.getDesc() : "")
                        .append("\"")
                        .append("\n");
            }

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private void writeExcelFile(File file, List<RecordEntity> records) {
        try {
            // 创建工作簿
            Workbook workbook = new HSSFWorkbook();
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
                RecordEntity record = records.get(i);
                Row row = sheet.createRow(i + 1);

                // 时间
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(new java.util.Date(record.getTime()));
                row.createCell(0).setCellValue(time);

                // 分类
                row.createCell(1).setCellValue(record.getCategoryName() != null ? record.getCategoryName() : "");

                // 金额
                row.createCell(2).setCellValue(record.getAmount());

                // 类型
                String type = record.getType() == RecordEntity.TYPE_EXPENSE ? "支出" : "收入";
                row.createCell(3).setCellValue(type);

                // 备注
                row.createCell(4).setCellValue(record.getDesc() != null ? record.getDesc() : "");
            }

            // 设置默认列宽，避免使用autoSizeColumn触发AWT依赖
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256); // 设置列宽为20个字符宽度
            }

            // 写入文件
            java.io.FileOutputStream fileOut = new java.io.FileOutputStream(file);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
        } catch (Exception e) {
            if (file.exists()) {
                file.delete();
            }
        }
    }
    private void exportTips(Context context, String msg){
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        });
    }

}

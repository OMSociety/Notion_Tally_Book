package com.coderpage.mine.app.tally.module.edit.record;

import com.coderpage.base.common.IError;
import com.coderpage.base.common.Result;
import com.coderpage.base.common.SimpleCallback;
import com.coderpage.concurrency.MineExecutors;
import com.coderpage.mine.app.tally.common.RecordType;
import com.coderpage.mine.app.tally.persistence.model.CategoryModel;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * 记账记录 Repository
 * 
 * 封装记账记录和分类的 CRUD 操作，提供给 ViewModel 调用。
 * 
 * @author lc. 2018-08-29 20:34
 * @since 0.6.0
 */
public class RecordRepository {

    private final TallyDatabase database;

    /**
     * 构造方法，每次从 DatabaseProvider 获取实例
     */
    public RecordRepository() {
        this.database = TallyDatabase.getInstance();
    }

    /**
     * 查询所有分类
     *
     * @param type     分类类型（支出/收入）
     * @param callback  回调
     */
    public void queryAllCategory(RecordType type, SimpleCallback<List<CategoryModel>> callback) {
        MineExecutors.ioExecutor().execute(() -> {
            if (type == RecordType.EXPENSE) {
                List<CategoryModel> categoryList = database.categoryDao().allExpenseCategory();
                MineExecutors.executeOnUiThread(() -> callback.success(categoryList));
                return;
            }
            if (type == RecordType.INCOME) {
                List<CategoryModel> categoryList = database.categoryDao().allIncomeCategory();
                MineExecutors.executeOnUiThread(() -> callback.success(categoryList));
                return;
            }
            MineExecutors.executeOnUiThread(() -> callback.success(new ArrayList<>()));
        });
    }

    /**
     * 通过 ID 查询记录
     *
     * @param recordId 记录 ID
     * @param callback 回调
     */
    public void queryRecordById(long recordId, SimpleCallback<RecordEntity> callback) {
        MineExecutors.ioExecutor().execute(() -> {
            RecordEntity record = database.recordDao().queryById(recordId);
            MineExecutors.executeOnUiThread(() -> callback.success(record));
        });
    }

    /**
     * 保存新记录
     *
     * @param record   记录数据
     * @param callback 回调
     */
    public void saveRecord(RecordEntity record, SimpleCallback<Result<Long, IError>> callback) {
        MineExecutors.ioExecutor().execute(() -> {
            long id = database.recordDao().insert(record.createEntity());
            MineExecutors.executeOnUiThread(() -> callback.success(new Result<>(id, null)));
        });
    }

    /**
     * 更新记录
     *
     * @param record   记录数据
     * @param callback 回调
     */
    public void updateExpense(RecordEntity record, SimpleCallback<Result<Long, IError>> callback) {
        MineExecutors.ioExecutor().execute(() -> {
            // 修改已同步记录时，重置同步状态，下次同步会上传变更
            record.setSyncStatus(0);
            long id = database.recordDao().update(record.createEntity());
            MineExecutors.executeOnUiThread(() -> callback.success(new Result<>(id, null)));
        });
    }
}

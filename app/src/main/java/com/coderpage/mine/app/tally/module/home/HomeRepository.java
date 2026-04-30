package com.coderpage.mine.app.tally.module.home;

import androidx.annotation.Nullable;
import android.util.Pair;

import com.coderpage.base.common.IError;
import com.coderpage.base.common.NonThrowError;
import com.coderpage.base.common.Result;
import com.coderpage.base.common.SimpleCallback;
import com.coderpage.concurrency.MineExecutors;
import com.coderpage.mine.app.tally.persistence.model.Record;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.app.tally.utils.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author lc. 2018-07-08 15:25
 * @since 0.6.0
 */

class HomeRepository {

    static class Snapshot {
        final int recent3DayRecordCount;
        final double todayExpenseTotalAmount;
        final double todayIncomeTotalAmount;
        final double currentMonthExpenseTotalAmount;
        final double currentMonthIncomeTotalAmount;
        final List<Pair<String, Double>> categoryExpenseTotal;
        final List<Record> recent3DayRecordList;
        final List<Record> todayRecordList;

        Snapshot(int recent3DayRecordCount,
                 double todayExpenseTotalAmount,
                 double todayIncomeTotalAmount,
                 double currentMonthExpenseTotalAmount,
                 double currentMonthIncomeTotalAmount,
                 List<Pair<String, Double>> categoryExpenseTotal,
                 List<Record> recent3DayRecordList,
                 List<Record> todayRecordList) {
            this.recent3DayRecordCount = recent3DayRecordCount;
            this.todayExpenseTotalAmount = todayExpenseTotalAmount;
            this.todayIncomeTotalAmount = todayIncomeTotalAmount;
            this.currentMonthExpenseTotalAmount = currentMonthExpenseTotalAmount;
            this.currentMonthIncomeTotalAmount = currentMonthIncomeTotalAmount;
            this.categoryExpenseTotal = Collections.unmodifiableList(categoryExpenseTotal);
            this.recent3DayRecordList = Collections.unmodifiableList(recent3DayRecordList);
            this.todayRecordList = Collections.unmodifiableList(todayRecordList);
        }
    }

    private final AtomicReference<Snapshot> mLatestSnapshot = new AtomicReference<>();
    private final Object mSnapshotLock = new Object();

    @Nullable
    Snapshot getLatestSnapshot() {
        synchronized (mSnapshotLock) {
            return mLatestSnapshot.get();
        }
    }

    /** 读取本月消费数据 */
    void loadCurrentMonthExpenseData(SimpleCallback<Result<Snapshot, IError>> callback) {
        MineExecutors.ioExecutor().execute(() -> {
            try {
            long todayStartTime = DateUtils.todayStartUnixTime();
            long todayEndTime = DateUtils.todayEndUnixTime();
            long day3AgoStartTime = todayStartTime - (1000 * 60 * 60 * 24 * 2);
            long monthStartTime = DateUtils.currentMonthStartUnixTime();
            long monthEndTime = System.currentTimeMillis();

            int recent3DayRecordCount = 0;
            double monthExpenseTotalAmount = 0;
            double monthIncomeTotalAmount = 0;
            double todayExpenseTotalAmount = 0;
            double todayIncomeTotalAmount = 0;

            List<Record> recent3DayList = new ArrayList<>();
            List<Record> todayRecordList = new ArrayList<>();
            Map<String, Double> getAmountByCategoryName = new HashMap<>();

            List<Record> currentMonthList =
                    TallyDatabase.getInstance().recordDao().queryAll(monthStartTime, monthEndTime, Integer.MAX_VALUE, 0);
            for (Record record : currentMonthList) {
                boolean isTodayRecord = record.getTime() >= todayStartTime && record.getTime() < todayEndTime;
                boolean isRecent3DayRecord = record.getTime() >= day3AgoStartTime && record.getTime() < todayEndTime;

                if (isTodayRecord) {
                    if (record.getType() == Record.TYPE_EXPENSE) {
                        todayExpenseTotalAmount += record.getAmount().doubleValue();
                    }
                    if (record.getType() == Record.TYPE_INCOME) {
                        todayIncomeTotalAmount += record.getAmount().doubleValue();
                    }
                    todayRecordList.add(record);
                }

                if (isRecent3DayRecord) {
                    recent3DayRecordCount++;
                    recent3DayList.add(record);
                }

                if (record.getType() == Record.TYPE_EXPENSE) {
                    monthExpenseTotalAmount += record.getAmount().doubleValue();
                }
                if (record.getType() == Record.TYPE_INCOME) {
                    monthIncomeTotalAmount += record.getAmount().doubleValue();
                }

                if (record.getType() == Record.TYPE_EXPENSE) {
                    Double categoryAmountTotal = getAmountByCategoryName.get(record.getCategoryName());
                    if (categoryAmountTotal == null) {
                        categoryAmountTotal = 0.0D;
                    }
                    categoryAmountTotal += record.getAmount().doubleValue();
                    getAmountByCategoryName.put(record.getCategoryName(), categoryAmountTotal);
                }
            }

            List<Pair<String, Double>> categoryExpenseTotal = new ArrayList<>(getAmountByCategoryName.size());
            for (Map.Entry<String, Double> entry : getAmountByCategoryName.entrySet()) {
                categoryExpenseTotal.add(new Pair<>(entry.getKey(), entry.getValue()));
            }
            Collections.sort(categoryExpenseTotal, (entry1, entry2) -> {
                double v1 = entry1.second == null ? 0 : entry1.second;
                double v2 = entry2.second == null ? 0 : entry2.second;
                if (v1 == v2) {
                    return 0;
                }
                return v1 > v2 ? -1 : 1;
            });

            Snapshot snapshot = new Snapshot(
                    recent3DayRecordCount,
                    todayExpenseTotalAmount,
                    todayIncomeTotalAmount,
                    monthExpenseTotalAmount,
                    monthIncomeTotalAmount,
                    categoryExpenseTotal,
                    recent3DayList,
                    todayRecordList);

            synchronized (mSnapshotLock) {
                mLatestSnapshot.set(snapshot);
            }

            Result<Snapshot, IError> result = new Result<>();
            result.setData(snapshot);
            MineExecutors.executeOnUiThread(() -> callback.success(result));
            } catch (Exception e) {
                Result<Snapshot, IError> errorResult = new Result<>();
                errorResult.setError(new NonThrowError(-1, e.getMessage()));
                MineExecutors.executeOnUiThread(() -> callback.success(errorResult));
            }
        });
    }
}

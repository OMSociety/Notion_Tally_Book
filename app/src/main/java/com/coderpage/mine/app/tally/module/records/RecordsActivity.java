package com.coderpage.mine.app.tally.module.records;

import android.app.Activity;
import android.app.DatePickerDialog;
import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.coderpage.base.widget.LoadingLayout;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.router.TallyRouter;
import com.coderpage.mine.app.tally.ui.refresh.RefreshFootView;
import com.coderpage.mine.app.tally.ui.refresh.RefreshHeadView;
import com.coderpage.mine.module.records.RecordsActivityBinding;
import com.coderpage.mine.ui.BaseActivity;
import com.lcodecore.tkrefreshlayout.RefreshListenerAdapter;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;

import java.util.Calendar;

/**
 * @author lc. 2019-01-05 10:24
 * @since 0.6.0
 *
 * 记录页
 */
@Route(path = TallyRouter.RECORDS)
public class RecordsActivity extends BaseActivity {

    static final String EXTRA_QUERY = "extra_query";

    private RecordsActivityBinding mBinding;
    private RecordsViewModel mViewModel;

    private LoadingLayout mLoadingLayout;
    private TwinklingRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private RecordsAdapter mAdapter;
    private FloatingActionButton mFabClearDate; // 添加清除按钮引用

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(self(), R.layout.tally_module_records_activity);
        mViewModel = ViewModelProviders.of(this).get(RecordsViewModel.class);
        getLifecycle().addObserver(mViewModel);
        initView();
        subscribeUi();
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setToolbarAsClose(v -> finish());
    }

    /**
     * 打开记录页
     *
     * @param context context
     * @param query   记录的查询条件
     */
    public static void open(Context context, RecordQuery query) {
        Intent intent = new Intent(context, RecordsActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra(EXTRA_QUERY, query);
        context.startActivity(intent);
    }

    private void initView() {
        mLoadingLayout = mBinding.loadingLayout;
        mRefreshLayout = mBinding.refreshLayout;
        mRecyclerView = mBinding.recyclerView;
        mFabClearDate = mBinding.fabClearDate;

        // 空数据页面点击事件处理
        mLoadingLayout.setUserActionListener(new LoadingLayout.BaseUserActionListener() {
            @Override
            public void onPositiveButtonClick(LoadingLayout layout, View view) {
                mViewModel.load();
            }

            @Override
            public void onIconClick(LoadingLayout layout, View view) {
                mViewModel.load();
            }
        });

        mRefreshLayout.setAutoLoadMore(true);
        mRefreshLayout.setHeaderView(new RefreshHeadView(this));
        mRefreshLayout.setHeaderHeight(120);
        mRefreshLayout.setBottomView(new RefreshFootView(this));
        mRefreshLayout.setBottomHeight(120);
        mRefreshLayout.setOnRefreshListener(new RefreshListenerAdapter() {
            @Override
            public void onRefresh(TwinklingRefreshLayout refreshLayout) {
                mViewModel.refresh();
            }

            @Override
            public void onLoadMore(TwinklingRefreshLayout refreshLayout) {
                mViewModel.loadMore();
            }
        });

        mAdapter = new RecordsAdapter(self());
        LinearLayoutManager layoutManager = new LinearLayoutManager(self(), LinearLayoutManager.VERTICAL, false);
        // 设置新 adapter 前，清除旧 adapter 上可能残留的 observer，防止泄漏
        RecyclerView.Adapter oldAdapter = mRecyclerView.getAdapter();
        if (oldAdapter != null) {
            try {
                oldAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
            } catch (Exception ignored) {
                // observer 未注册时会抛异常，忽略即可
            }
        }
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(layoutManager);

        // 添加 FloatingActionButton 的点击事件处理
        FloatingActionButton fabCalendar = mBinding.fabCalendar;
        fabCalendar.setOnClickListener(v -> showDatePicker());

        // 添加清除日期按钮的点击事件处理
        mFabClearDate.setOnClickListener(v -> clearDateFilter());
    }

    private void subscribeUi() {
        mViewModel.getLoadingStatus().observe(this, loadingStatus -> {
            if (loadingStatus != null) {
                mLoadingLayout.setStatus(loadingStatus);
            }
        });
        mViewModel.getRefreshing().observe(this, refreshing -> {
            if (refreshing != null && !refreshing) {
                mRefreshLayout.finishRefreshing();
            }
        });
        mViewModel.getLoadingMore().observe(this, loadingMore -> {
            if (loadingMore != null && !loadingMore) {
                mRefreshLayout.finishLoadmore();
            }
        });
        mViewModel.getRecordList().observe(this, recordList -> {
            if (recordList != null) {
                mAdapter.setDataList(recordList);
            }
        });
        mViewModel.getToolbarTitle().observe(this, subTitle -> {
            if (TextUtils.isEmpty(subTitle)) {
                return;
            }
            setToolbarTitle(subTitle);
            updateClearButtonVisibility();
        });
    }

    // 更新清除按钮可见性 - 使用更可靠的方法
    private void updateClearButtonVisibility() {
        RecordQuery currentQuery = mViewModel.getCurrentQuery();
        // 如果开始时间大于0，说明设置了日期筛选条件
        if (currentQuery != null && currentQuery.getStartTime() > 0) {
            mFabClearDate.setVisibility(View.VISIBLE);
        } else {
            mFabClearDate.setVisibility(View.GONE);
        }
    }



    // 添加显示日期选择器的方法
    private void showDatePicker() {
        // 获取当前日期作为默认值
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // 创建并显示 DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // 用户选择日期后的回调处理
                    // 构造查询条件，查询选定日期的记录
                    RecordQuery.Builder builder = new RecordQuery.Builder()
                            .setType(RecordQuery.TYPE_ALL); // 查询所有类型的记录

                    // 设置开始时间和结束时间（选定日期的00:00:00到23:59:59）
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0);
                    long startTime = selectedDate.getTimeInMillis();

                    // 设置结束时间为当天的最后一毫秒
                    selectedDate.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59);
                    long endTime = selectedDate.getTimeInMillis();

                    builder.setStartTime(startTime);
                    builder.setEndTime(endTime);

                    // 应用新的查询条件并重新加载数据
                    mViewModel.setQuery(builder.build());
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    // 更新清除按钮可见性
    private void updateClearButtonVisibility(CharSequence title) {
        if (title != null && title.toString().contains(" - ")) {
            // 如果标题包含日期范围信息，则显示清除按钮
            mFabClearDate.setVisibility(View.VISIBLE);
        } else {
            // 否则隐藏清除按钮
            mFabClearDate.setVisibility(View.GONE);
        }
    }


    // 清除日期筛选
    private void clearDateFilter() {
        // 创建一个新的查询条件，不设置时间范围
        RecordQuery.Builder builder = new RecordQuery.Builder()
                .setType(RecordQuery.TYPE_ALL)
                .setStartTime(0) // 设置为0表示不限制开始时间
                .setEndTime(System.currentTimeMillis()); // 结束时间设为当前时间

        // 应用新的查询条件并重新加载数据
        mViewModel.setQuery(builder.build());
        // 不需要手动隐藏按钮，updateClearButtonVisibility() 会在 setQuery() 后被调用
    }
}

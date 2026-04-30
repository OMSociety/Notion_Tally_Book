package com.coderpage.mine.app.tally.module.home;

import android.app.Activity;
import androidx.lifecycle.ViewModelProviders;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.module.home.model.HomeDisplayData;
import com.coderpage.mine.app.tally.module.records.RecordItemViewModel;
import com.coderpage.mine.app.tally.persistence.model.Record;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lc. 2018-07-21 10:48
 * @since 0.6.0
 */

class HomeAdapter extends RecyclerView.Adapter<BaseViewHolder> {

    private WeakReference<Activity> mActivityRef;
    private HomeViewModel mViewModel;
    private HomeMonthInfoViewModel mMonthInfoViewModel;
    private RecordItemViewModel mRecordItemViewModel;
    private LayoutInflater mInflater;
    private List<HomeDisplayData> mDataList = new ArrayList<>();

    HomeAdapter(FragmentActivity activity, HomeViewModel viewModel, RecordItemViewModel recordItemViewModel) {
        mActivityRef = new WeakReference<>(activity);
        mViewModel = viewModel;
        mMonthInfoViewModel = ViewModelProviders.of(activity).get(HomeMonthInfoViewModel.class);
        mRecordItemViewModel = recordItemViewModel;
        mInflater = LayoutInflater.from(activity);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mActivityRef = null;
        mViewModel = null;
        mMonthInfoViewModel = null;
        mRecordItemViewModel = null;
        mInflater = null;
    }

    void setDataList(List<HomeDisplayData> dataList) {
        if (dataList == null) {
            return;
        }
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mDataList.size();
            }

            @Override
            public int getNewListSize() {
                return dataList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                HomeDisplayData oldData = mDataList.get(oldItemPosition);
                HomeDisplayData newData = dataList.get(newItemPosition);

                if (oldData == null || newData == null) {
                    return oldData == newData;
                }

                if (oldData.getType() != newData.getType()) {
                    return false;
                }

                switch (newData.getType()) {
                    case HomeDisplayData.TYPE_MONTH_INFO:
                    case HomeDisplayData.TYPE_RECENT_DAY_INFO:
                        return true;

                    case HomeDisplayData.TYPE_RECORD_ITEM:
                        if (!(oldData.getInternal() instanceof Record)
                                || !(newData.getInternal() instanceof Record)) {
                            return false;
                        }
                        Record oldRecord = (Record) oldData.getInternal();
                        Record newRecord = (Record) newData.getInternal();
                        return oldRecord.getId() == newRecord.getId();

                    default:
                        break;
                }
                return false;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return false;
            }
        });
        mDataList.clear();
        mDataList.addAll(dataList);
        result.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int position) {
        return mDataList.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Activity activity = mActivityRef != null ? mActivityRef.get() : null;
        if (activity == null) {
            throw new IllegalStateException("Activity has been garbage collected");
        }

        switch (viewType) {

            // 月消费、月收入信息模块
            case HomeDisplayData.TYPE_MONTH_INFO:
                return new ViewHolderMonthInfo(activity, mMonthInfoViewModel,
                        DataBindingUtil.inflate(mInflater, R.layout.tally_module_home_item_month_info, parent, false));

            // 今日账单数据
            case HomeDisplayData.TYPE_RECENT_DAY_INFO:
                return new ViewHolderTodayExpense(activity, mViewModel,
                        DataBindingUtil.inflate(mInflater, R.layout.tally_module_home_item_today_expense, parent, false));

            // 消费记录&支出记录 ITEM
            case HomeDisplayData.TYPE_RECORD_ITEM:
                return new ViewHolderRecordItem(activity, mRecordItemViewModel, DataBindingUtil.inflate(
                        mInflater, R.layout.tally_item_record_common, parent, false));

            // 底部 View
            case HomeDisplayData.TYPE_BOTTOM:
                return new ViewHolderBottom(mInflater.inflate(R.layout.tally_module_home_item_bottom, parent, false));

            default:
                throw new IllegalArgumentException("Unknown viewType: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.bindData(mDataList.get(position));
    }
}

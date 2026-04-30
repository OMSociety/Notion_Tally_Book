package com.coderpage.mine.app.tally.module.home;

import android.app.Activity;

import com.coderpage.mine.app.tally.module.home.model.HomeDisplayData;
import com.coderpage.mine.app.tally.module.records.RecordItemViewModel;
import com.coderpage.mine.app.tally.persistence.model.Record;
import com.coderpage.mine.tally.module.records.RecordItemBinding;

import java.lang.ref.WeakReference;

/**
 * @author lc. 2019-03-25 17:04
 * @since 0.6.0
 */

public class ViewHolderRecordItem extends BaseViewHolder {

    private WeakReference<Activity> mActivityRef;
    private RecordItemBinding mBinding;
    private RecordItemViewModel mViewModel;

    ViewHolderRecordItem(Activity activity, RecordItemViewModel viewModel, RecordItemBinding binding) {
        super(binding.getRoot());
        mBinding = binding;
        mActivityRef = new WeakReference<>(activity);
        mViewModel = viewModel;
    }

    void bindData(HomeDisplayData data) {
        if (data != null && data.getInternal() != null && data.getInternal() instanceof Record) {
            Activity activity = mActivityRef != null ? mActivityRef.get() : null;
            if (activity == null) {
                return;
            }
            Record record = (Record) data.getInternal();
            mBinding.setActivity(activity);
            mBinding.setData(record);
            mBinding.setVm(mViewModel);
            mBinding.executePendingBindings();
        }
    }
}

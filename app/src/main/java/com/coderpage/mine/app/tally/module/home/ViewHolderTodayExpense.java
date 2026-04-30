package com.coderpage.mine.app.tally.module.home;

import android.app.Activity;

import com.coderpage.mine.app.tally.module.home.model.HomeDisplayData;
import com.coderpage.mine.app.tally.module.home.model.HomeTodayDayRecordsModel;
import com.coderpage.mine.tally.module.home.TodayExpenseItemBinding;

import java.lang.ref.WeakReference;

/**
 * @author lc. 2018-08-14 20:52
 * @since 0.6.0
 */
class ViewHolderTodayExpense extends BaseViewHolder {

    private WeakReference<Activity> mActivityRef;
    private HomeViewModel mViewModel;
    private TodayExpenseItemBinding mBinding;

    ViewHolderTodayExpense(Activity activity, HomeViewModel viewModel, TodayExpenseItemBinding binding) {
        super(binding.getRoot());
        mActivityRef = new WeakReference<>(activity);
        mViewModel = viewModel;
        mBinding = binding;
    }

    @Override
    void bindData(HomeDisplayData data) {
        if (data != null && data.getInternal() != null && data.getInternal() instanceof HomeTodayDayRecordsModel) {
            Activity activity = mActivityRef != null ? mActivityRef.get() : null;
            if (activity == null) {
                return;
            }
            HomeTodayDayRecordsModel todayModel = (HomeTodayDayRecordsModel) data.getInternal();
            mBinding.setActivity(activity);
            mBinding.setVm(mViewModel);
            mBinding.setData(todayModel);
            mBinding.executePendingBindings();
        }
    }
}

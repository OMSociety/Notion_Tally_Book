package com.coderpage.mine.app.tally.module.home;

import android.app.Activity;

import com.coderpage.mine.app.tally.module.home.model.HomeDisplayData;
import com.coderpage.mine.app.tally.module.home.model.HomeMonthModel;
import com.coderpage.mine.tally.module.home.MonthInfoItemBinding;

import java.lang.ref.WeakReference;

/**
 * @author lc. 2018-07-21 10:59
 * @since 0.6.0
 */

class ViewHolderMonthInfo extends BaseViewHolder {

    private WeakReference<Activity> mActivityRef;
    private HomeMonthInfoViewModel mViewModel;
    private MonthInfoItemBinding mBinding;

    ViewHolderMonthInfo(Activity activity, HomeMonthInfoViewModel viewModel, MonthInfoItemBinding binding) {
        super(binding.getRoot());
        mActivityRef = new WeakReference<>(activity);
        mViewModel = viewModel;
        mBinding = binding;
    }

    @Override
    void bindData(HomeDisplayData data) {
        if (data != null && data.getInternal() != null && data.getInternal() instanceof HomeMonthModel) {
            Activity activity = mActivityRef != null ? mActivityRef.get() : null;
            if (activity == null) {
                return;
            }
            HomeMonthModel monthModel = (HomeMonthModel) data.getInternal();
            mBinding.setActivity(activity);
            mBinding.setVm(mViewModel);
            mBinding.setData(monthModel);
            mViewModel.setData(monthModel);
            mBinding.executePendingBindings();
        }
    }
}

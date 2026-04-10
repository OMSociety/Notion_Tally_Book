package com.coderpage.mine.app.tally.module.debug;

import androidx.lifecycle.ViewModelProvider;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.coderpage.mine.R;
import com.coderpage.mine.module.debug.DebugActivityBinding;
import com.coderpage.mine.ui.BaseActivity;

/**
 * @author lc. 2019-04-28 10:07
 * @since 0.6.2
 */
public class DebugActivity extends BaseActivity {

    private DebugActivityBinding mBinding;
    private DebugViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.tally_module_debug_activity);
        mViewModel = new ViewModelProvider(this).get(DebugViewModel.class);

        subscribeUi();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setToolbarAsClose(v -> onBackPressed());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mViewModel.onRequestPermissionsResult(self(), requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void subscribeUi() {
        mBinding.setActivity(this);
        mBinding.setVm(mViewModel);
    }
}

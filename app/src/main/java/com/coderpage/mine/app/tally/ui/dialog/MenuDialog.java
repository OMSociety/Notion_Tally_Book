package com.coderpage.mine.app.tally.ui.dialog;

import androidx.lifecycle.ViewModelProvider;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.coderpage.mine.R;
import com.coderpage.mine.dialog.MenuDialogBinding;

/**
 * @author lc. 2019-04-07 09:09
 * @since 0.6.0
 *
 * 弹框弹框
 */

public class MenuDialog extends DialogFragment {

    private MenuDialogAdapter mAdapter;

    private MenuDialogBinding mBinding;
    private MenuDialogViewModel mViewModel;

    /**
     * 打开菜单弹框
     *
     * @param activity activity
     */
    public static void show(FragmentActivity activity) {
        new MenuDialog().show(activity.getSupportFragmentManager(), "menu_dialog");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_Tally_Menu);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.tally_dialog_menu, container, false);
        mViewModel = new ViewModelProvider(this).get(MenuDialogViewModel.class);
        initView();
        subscribeUi();
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void initView() {
        RecyclerView recyclerMenu = mBinding.recyclerMenu;
        recyclerMenu.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mAdapter = new MenuDialogAdapter(mViewModel);
        recyclerMenu.setAdapter(mAdapter);
    }

    private void subscribeUi() {
        mBinding.setVm(mViewModel);
        mViewModel.getMenuList().observe(this, menuList -> {
            mAdapter.setMenuList(menuList);
        });
        mViewModel.getViewReliedTask().observe(this, task -> {
            if (task != null) {
                task.execute(MenuDialog.this);
            }
        });
    }
}

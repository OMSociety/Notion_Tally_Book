package com.coderpage.mine.app.tally.module.home;

import android.Manifest;
import androidx.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.coderpage.base.utils.ResUtils;
import com.coderpage.base.utils.UIUtils;
import com.coderpage.mine.BuildConfig;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.permission.PermissionReqHandler;
import com.coderpage.mine.app.tally.module.debug.DebugActivity;
import com.coderpage.mine.app.tally.module.records.RecordItemViewModel;
import com.coderpage.mine.app.tally.module.search.SearchActivity;
import com.coderpage.mine.app.tally.ui.dialog.PermissionReqDialog;
import com.coderpage.mine.app.tally.ui.dialog.PrivacyPolicyDialog;
import com.coderpage.mine.app.tally.ui.refresh.RefreshHeadView;
import com.coderpage.mine.app.tally.update.UpdateUtils;
import com.coderpage.mine.ui.BaseActivity;
import com.coderpage.mine.ui.widget.recyclerview.ItemMarginDecoration;
import com.coderpage.mine.utils.AndroidUtils;
import com.lcodecore.tkrefreshlayout.RefreshListenerAdapter;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lc. 2018-07-07 11:04
 * @since 0.6.0
 *
 * 记账本首页
 */

public class HomeActivity extends BaseActivity {

    private static final String PREF_NAME = "app_policy";
    private static final String KEY_PRIVACY_ACCEPTED = "privacy_accepted";

    private PermissionReqHandler mPermissionReqHandler;
    private String[] mNeedPermissionArray = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.READ_PHONE_STATE
    };

    private HomeViewModel mViewModel;
    private com.coderpage.mine.app.tally.module.home.HomeActivityBinding mBinding;
    private HomeAdapter mAdapter;
    private TwinklingRefreshLayout mRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.module_home_activity_home);
        mViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        getLifecycle().addObserver(mViewModel);

        // 检查是否已同意隐私政策
        checkPrivacyPolicy();

        initView();
        subscribeUi();

        mPermissionReqHandler = new PermissionReqHandler(self());
    }

    /**
     * 检查隐私政策是否已同意
     */
    private void checkPrivacyPolicy() {
        if (isPrivacyAccepted()) {
            return;
        }
        showPrivacyPolicyDialog(true);
    }

    private boolean isPrivacyAccepted() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return sp.getBoolean(KEY_PRIVACY_ACCEPTED, false);
    }

    /**
     * 显示隐私政策对话框
     * @param isFirstTime 是否首次显示
     */
    private void showPrivacyPolicyDialog(boolean isFirstTime) {
        new PrivacyPolicyDialog(this)
                .setFirstTime(isFirstTime)
                .setOnPositiveClickListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 用户同意隐私政策
                        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                        sp.edit().putBoolean(KEY_PRIVACY_ACCEPTED, true).apply();
                        dialog.dismiss();

                        // 继续处理权限请求
                        handlePermission();
                    }
                })
                .setOnNegativeClickListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 用户拒绝隐私政策，退出应用
                        dialog.dismiss();
                        showExitDialog();
                    }
                })
                .create()
                .show();
    }

    /**
     * 显示退出应用对话框
     */
    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("您需要同意隐私政策才能使用本应用")
                .setPositiveButton(R.string.exit_app, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        UpdateUtils.checkPersistedNewVersionAndShowUpdateConfirmDialog(this);
        if (isPrivacyAccepted()) {
            handlePermission();
        }
    }

    private void handlePermission() {
        // 检查授权，请求权限
        String[] notGrantedPermissionArray = mPermissionReqHandler.getNotGrantedPermissionArray(self(), mNeedPermissionArray);
        if (notGrantedPermissionArray.length == 0) {
            return;
        }

        // 读存储权限 写存储权限，显示一条即可
        List<String> permissionList = new ArrayList<>(Arrays.asList(notGrantedPermissionArray));
        if (permissionList.contains(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                && permissionList.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissionList.remove(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        new PermissionReqDialog(self(), permissionList)
                .setTitleText(ResUtils.getString(self(), R.string.permission_req_title_format, ResUtils.getString(self(), R.string.app_name)))
                .setPositiveText(ResUtils.getString(self(), R.string.permission_req_open))
                .setListener(new PermissionReqDialog.Listener() {
                    @Override
                    public void onCancelClick(DialogInterface dialog) {
                        dialog.dismiss();
                    }

                    @Override
                    public void onConfirmClick(DialogInterface dialog) {
                        dialog.dismiss();
                        // 请求权限
                        mPermissionReqHandler.requestPermission(true, notGrantedPermissionArray, new PermissionReqHandler.Listener() {
                            @Override
                            public void onGranted(boolean grantedAll, String[] permissionArray) {
                                // 全部授权 no-op
                            }

                            @Override
                            public void onDenied(String[] permissionArray) {
                                showPermissionNeedDialog(permissionArray);
                            }
                        });
                    }
                }).show();
    }

    private void showPermissionNeedDialog(String[] permissionArray) {
        // 读存储权限 写存储权限，显示一条即可
        List<String> permissionList = new ArrayList<>(Arrays.asList(permissionArray));
        if (permissionList.contains(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                && permissionList.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissionList.remove(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        new PermissionReqDialog(self(), permissionList)
                .setTitleText(ResUtils.getString(self(), R.string.permission_req_title_format, ResUtils.getString(self(), R.string.app_name)))
                .setPositiveText(ResUtils.getString(self(), R.string.permission_req_go_open))
                .setListener(new PermissionReqDialog.Listener() {
                    @Override
                    public void onCancelClick(DialogInterface dialog) {
                        dialog.dismiss();
                    }

                    @Override
                    public void onConfirmClick(DialogInterface dialog) {
                        dialog.dismiss();
                        AndroidUtils.openAppSettingPage(self());
                    }
                })
                .show();
    }

    private void initView() {
        mRefreshLayout = mBinding.refreshLayout;
        mRefreshLayout.setHeaderView(new RefreshHeadView(this));
        mRefreshLayout.setOnRefreshListener(new RefreshListenerAdapter() {
            @Override
            public void onRefresh(TwinklingRefreshLayout refreshLayout) {
                mViewModel.refresh();
            }
        });

        RecyclerView recyclerView = mBinding.recyclerView;
        mAdapter = new HomeAdapter(this, mViewModel, ViewModelProviders.of(this).get(RecordItemViewModel.class));

        ItemMarginDecoration itemMarginDecoration = new ItemMarginDecoration(0, 0, 0, 0);
        // 最后一个 ITEM 距离底部距离大一些，防止被底部按钮遮挡
        itemMarginDecoration.setLastItemOffset(0, 0, 0, UIUtils.dp2px(this, 80));
        recyclerView.addItemDecoration(itemMarginDecoration);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(mAdapter);
    }

    private void subscribeUi() {
        mBinding.setActivity(this);
        mBinding.setVm(mViewModel);
        mViewModel.observableDataList().observe(this,
                dataList -> mAdapter.setDataList(dataList));
        mViewModel.observableRefreshing().observe(this, refreshing -> {
            if (refreshing == null || !refreshing) {
                mRefreshLayout.finishRefreshing();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionReqHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_tally_main, menu);
        MenuItem debugMenu = menu.findItem(R.id.menu_debug);
        debugMenu.setVisible(BuildConfig.DEBUG);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_debug:
                startActivity(new Intent(this, DebugActivity.class));
                break;
            case R.id.menu_search:
                startActivity(new Intent(this, SearchActivity.class));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}

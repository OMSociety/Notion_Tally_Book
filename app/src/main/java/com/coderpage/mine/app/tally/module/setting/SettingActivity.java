package com.coderpage.mine.app.tally.module.setting;

import static com.coderpage.base.utils.LogUtils.makeLogTag;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import com.alibaba.android.arouter.launcher.ARouter;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.coderpage.base.common.Callback;
import com.coderpage.base.common.IError;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.router.TallyRouter;
import com.coderpage.mine.app.tally.eventbus.EventRecordDelete;
import com.coderpage.mine.module.setting.SettingActivityBinding;
import com.coderpage.mine.ui.BaseActivity;

import org.greenrobot.eventbus.EventBus;

/**
 * @author abner-l. 2017-06-01
 */

@Route(path = TallyRouter.SETTING)
public class SettingActivity extends BaseActivity {

    private static final String TAG = makeLogTag(SettingActivity.class);

    private SettingActivityBinding mBinding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_setting);

        // 初始化 ViewModel
        if (mBinding.getVm() == null) {
            mBinding.setVm(new SettingViewModel(getApplication()));
        }

        // 添加文本变化监听器，自动保存设置
        addTextWatchers();

        Button btnClearData = findViewById(R.id.btnClearData);
        btnClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearDataConfirmation();
            }
        });

        
        // 初始化 AI 设置按钮

        // 初始化 Notion 同步按钮
        Button btnSyncNow = findViewById(R.id.btnSyncNow);
        btnSyncNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingViewModel viewModel = mBinding.getVm();
                // 先保存配置
                viewModel.saveNotionToken(viewModel.notionToken.get());
                viewModel.saveNotionDatabaseId(viewModel.notionDatabaseId.get());
                // 开始同步
                viewModel.startSync(SettingActivity.this);
                Toast.makeText(SettingActivity.this, "开始同步...", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 初始化创建数据库按钮
        Button btnCreateDatabase = findViewById(R.id.btnCreateDatabase);
        btnCreateDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到 Notion 数据库创建帮助页面
                ARouter.getInstance()
                    .build(TallyRouter.NOTION_DATABASE_HELP)
                    .navigation(SettingActivity.this);
            }
        });
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setToolbarAsBack(view -> finish());
    }

    // 添加确认对话框方法
    private void showClearDataConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("确认清除数据")
                .setMessage("确定要清除所有数据吗？此操作无法撤销。")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 执行数据清除操作
                        clearAllData();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 添加数据清除方法
    private void clearAllData() {
        // 实现实际的账单记录清除逻辑
        SettingViewModel viewModel = mBinding.getVm();

        // 调用 ViewModel 中的方法清除所有账单记录
        viewModel.clearAllRecords(new Callback<Boolean, IError>() {
            @Override
            public void success(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingActivity.this, "所有账单记录已清除", Toast.LENGTH_SHORT).show();
                    // 发送事件通知其他组件数据已更新
                    EventBus.getDefault().post(new EventRecordDelete(null));
                });
            }

            @Override
            public void failure(IError error) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingActivity.this, "清除失败: " + error.msg(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void addTextWatchers() {
        // 为 API Key 输入框添加监听
        mBinding.etApiKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 不需要处理
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 不需要处理
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 文本变化后保存 API Key
                mBinding.getVm().saveApiKey(s.toString());
            }
        });
        // 为 AI Model 输入框添加监听
        mBinding.etAiModel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 不需要处理
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 不需要处理
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 文本变化后保存 AI Model
                mBinding.getVm().saveAiModel(s.toString());
            }
        });
    }
}

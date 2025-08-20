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

//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
//                != PackageManager.PERMISSION_GRANTED ||
//                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
//                        != PackageManager.PERMISSION_GRANTED) {
//
//            ActivityCompat.requestPermissions(this,
//                    new String[]{
//                            Manifest.permission.RECEIVE_SMS,
//                            Manifest.permission.READ_SMS
//                    }, 1001);
//        }

        // 初始化短信识别开关按钮
        Button btnRecognitionSwitch = findViewById(R.id.btnRecognitionSwitch);
        // 设置初始状态
        btnRecognitionSwitch.setText(mBinding.getVm().smsRecognitionEnabled.get() ? "已开启" : "已关闭");
        btnRecognitionSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 在点击时检查权限
                if (ContextCompat.checkSelfPermission(SettingActivity.this, Manifest.permission.RECEIVE_SMS)
                        != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(SettingActivity.this, Manifest.permission.READ_SMS)
                                != PackageManager.PERMISSION_GRANTED) {

                    // 如果没有权限，先请求权限
                    ActivityCompat.requestPermissions(SettingActivity.this,
                            new String[]{
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS
                            }, 1001);
                } else {
                    // 如果已有权限，执行开关逻辑
                    SettingViewModel viewModel = mBinding.getVm();
                    boolean currentStatus = viewModel.smsRecognitionEnabled.get();
                    boolean newStatus = !currentStatus;

                    // 保存新状态到数据库
                    viewModel.saveSmsRecognitionEnabled(newStatus);

                    // 更新按钮文本
                    btnRecognitionSwitch.setText(newStatus ? "已开启" : "已关闭");

                    // 显示状态变更提示
                    Toast.makeText(SettingActivity.this, newStatus ? "短信识别功能已开启" : "短信识别功能已关闭", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 监听开关状态变化以保持UI同步
        mBinding.getVm().getSmsRecognitionEnabledLiveData().observe(this, enabled -> {
            if (enabled != null) {
                btnRecognitionSwitch.setText(enabled ? "已开启" : "已关闭");
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
        // 为检测名单输入框添加监听
        mBinding.etDetectionList.addTextChangedListener(new TextWatcher() {
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
                // 文本变化后保存检测名单
                mBinding.getVm().saveDetectionList(s.toString());
            }
        });
    }
}

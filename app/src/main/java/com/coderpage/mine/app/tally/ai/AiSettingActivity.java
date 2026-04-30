package com.coderpage.mine.app.tally.ai;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.coderpage.mine.app.tally.common.router.TallyRouter;
import com.coderpage.mine.R;
import com.coderpage.mine.databinding.ActivityAiSettingBinding;
import com.coderpage.mine.ui.BaseActivity;

/**
 * AI 设置页面
 * 
 * @author Flandre Scarlet
 */
@Route(path = TallyRouter.AI_SETTING)
public class AiSettingActivity extends BaseActivity {

    private static final String TAG = "AiSettingActivity";
    private ActivityAiSettingBinding mBinding;
    private AiSettingViewModel mViewModel;
    private ArrayAdapter<AiApiConfig.ProviderTemplate> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityAiSettingBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        initToolbar();
        initViewModel();
        initViews();
        setupSpinner();
    }

    private void initToolbar() {
        setToolbarAsBack(view -> finish());
        if (getToolbar() != null) {
            getToolbar().setTitle("AI 识别设置");
        }
    }

    private void initViewModel() {
        mViewModel = new AiSettingViewModel(getApplication());
        mBinding.setVm(mViewModel);
    }

    private void initViews() {
        // 保存按钮
        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> {
            try {
                mViewModel.saveConfig();
                Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
                finish();
            } catch (RuntimeException e) {
                Log.e(TAG, "保存配置失败", e);
                Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // 测试按钮
        Button btnTest = findViewById(R.id.btnTest);
        btnTest.setOnClickListener(v -> {
            mViewModel.testConnection();
        });

        // 观察测试结果
        mViewModel.getTestResult().observe(this, result -> {
            if (result != null) {
                showTestResult(result);
            }
        });
    }

    private void setupSpinner() {
        Spinner spinner = findViewById(R.id.spinnerProvider);
        mAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                mViewModel.getProviderTemplates());
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(mAdapter);

        // 设置当前选中项
        spinner.setSelection(mViewModel.getProviderIndex());

        // 选择监听
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mViewModel.setProviderIndex(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void showTestResult(AiSettingViewModel.TestResult result) {
        View cardResult = findViewById(R.id.cardResult);
        TextView tvResultTitle = findViewById(R.id.tvResultTitle);
        TextView tvResultMessage = findViewById(R.id.tvResultMessage);

        cardResult.setVisibility(View.VISIBLE);
        tvResultMessage.setText(result.message);

        if (result.success) {
            tvResultTitle.setText("✅ 测试成功");
            tvResultTitle.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvResultTitle.setText("❌ 测试失败");
            tvResultTitle.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
}

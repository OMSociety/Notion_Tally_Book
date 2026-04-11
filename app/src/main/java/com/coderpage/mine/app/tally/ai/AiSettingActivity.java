package com.coderpage.mine.app.tally.ai;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.router.TallyRouter;
import com.coderpage.mine.ui.BaseActivity;

/**
 * AI 设置页面
 * 使用传统 findViewById 方式，避免 DataBinding 问题
 * 
 * @author Flandre Scarlet
 */
@Route(path = TallyRouter.AI_SETTING)
public class AiSettingActivity extends BaseActivity {

    private EditText etApiUrl;
    private EditText etApiKey;
    private EditText etModel;
    private Spinner spinnerProvider;
    private Button btnSave;
    private Button btnTest;
    private View cardResult;
    private TextView tvResultTitle;
    private TextView tvResultMessage;
    private ProgressBar progressBar;
    
    private AiSettingViewModel mViewModel;
    private ArrayAdapter<AiApiConfig.ProviderTemplate> mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_setting);
        
        // 初始化视图
        initViews();
        
        // 初始化 ViewModel
        mViewModel = new AiSettingViewModel(getApplication());
        
        // 初始化 Spinner
        setupSpinner();
        
        // 加载配置
        loadConfig();
    }

    private void initViews() {
        // 初始化工具栏
        setToolbarAsBack(view -> finish());
        if (getToolbar() != null) {
            getToolbar().setTitle("AI 图片识别设置");
        }
        
        // 绑定视图
        etApiUrl = findViewById(R.id.etApiUrl);
        etApiKey = findViewById(R.id.etApiKey);
        etModel = findViewById(R.id.etModel);
        spinnerProvider = findViewById(R.id.spinnerProvider);
        btnSave = findViewById(R.id.btnSave);
        btnTest = findViewById(R.id.btnTest);
        cardResult = findViewById(R.id.cardResult);
        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvResultMessage = findViewById(R.id.tvResultMessage);
        progressBar = findViewById(R.id.progressBar);
        
        // 保存按钮
        btnSave.setOnClickListener(v -> {
            saveConfig();
        });
        
        // 测试按钮
        btnTest.setOnClickListener(v -> {
            testConnection();
        });
    }

    private void setupSpinner() {
        mAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                mViewModel.getProviderTemplates());
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvider.setAdapter(mAdapter);
        
        spinnerProvider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mViewModel.setProviderIndex(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadConfig() {
        AiApiConfig config = AiApiConfig.load(this);
        etApiUrl.setText(config.getApiUrl());
        etApiKey.setText(config.getApiKey());
        etModel.setText(config.getModel());
        
        // 设置 Spinner 选中项
        int index = mViewModel.getProviderIndex();
        if (index >= 0) {
            spinnerProvider.setSelection(index);
        }
    }

    private void saveConfig() {
        AiApiConfig config = new AiApiConfig();
        config.setProvider(mViewModel.selectedProvider.get());
        config.setApiUrl(etApiUrl.getText().toString().trim());
        config.setApiKey(etApiKey.getText().toString().trim());
        config.setModel(etModel.getText().toString().trim());
        config.save(this);
        
        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void testConnection() {
        // 显示加载中
        progressBar.setVisibility(View.VISIBLE);
        cardResult.setVisibility(View.GONE);
        
        // 更新 ViewModel
        mViewModel.setApiUrl(etApiUrl.getText().toString().trim());
        mViewModel.setApiKey(etApiKey.getText().toString().trim());
        mViewModel.setModel(etModel.getText().toString().trim());
        
        // 测试连接
        mViewModel.testConnection();
        
        // 观察结果
        mViewModel.getTestResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);
            cardResult.setVisibility(View.VISIBLE);
            
            if (result != null) {
                tvResultMessage.setText(result.message);
                if (result.success) {
                    tvResultTitle.setText("测试成功");
                    tvResultTitle.setTextColor(0xFF4CAF50);
                } else {
                    tvResultTitle.setText("测试失败");
                    tvResultTitle.setTextColor(0xFFF44336);
                }
            }
        });
    }
}

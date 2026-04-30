package com.coderpage.mine.app.tally.ui.dialog;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.coderpage.mine.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author lingma
 * <p>
 * 文档查看Activity，用于显示用户协议和隐私政策
 */
public class DocumentActivity extends AppCompatActivity {

    public static final String EXTRA_DOCUMENT_TYPE = "document_type";
    public static final int TYPE_USER_AGREEMENT = 1;
    public static final int TYPE_PRIVACY_POLICY = 2;

    private TextView mDocumentContent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document);

        initToolbar();
        initView();

        int documentType = getIntent().getIntExtra(EXTRA_DOCUMENT_TYPE, TYPE_USER_AGREEMENT);
        loadDocument(documentType);
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initView() {
        mDocumentContent = findViewById(R.id.tv_document_content);
    }

    private void loadDocument(int documentType) {
        String fileName = "";
        String title = "";

        switch (documentType) {
            case TYPE_USER_AGREEMENT:
                fileName = "tally/user_agreement.txt";
                title = "用户协议";
                break;
            case TYPE_PRIVACY_POLICY:
                fileName = "tally/privacy_policy.txt";
                title = "隐私政策";
                break;
            default:
                fileName = "tally/user_agreement.txt";
                title = "用户协议";
                break;
        }

        setTitle(title);

        try (InputStream inputStream = getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            mDocumentContent.setText(content.toString());
        } catch (IOException e) {
            e.printStackTrace();
            mDocumentContent.setText("无法加载文档内容");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

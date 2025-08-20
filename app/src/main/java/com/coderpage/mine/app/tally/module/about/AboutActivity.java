package com.coderpage.mine.app.tally.module.about;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.coderpage.lib.update.ApkModel;
import com.coderpage.lib.update.Error;
import com.coderpage.lib.update.Updater;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.router.TallyRouter;
import com.coderpage.mine.app.tally.ui.dialog.DocumentActivity;
import com.coderpage.mine.app.tally.update.UpdateUtils;
import com.coderpage.mine.ui.BaseActivity;
import com.coderpage.mine.BuildConfig;

import java.util.Locale;

/**
 * @author abner-l. 2017-03-23
 */
@Route(path = TallyRouter.ABOUT)
public class AboutActivity extends BaseActivity {
    private TextView mNewVersionTv;
    // 添加一个变量来跟踪当前使用的更新源
    private int mCurrentUpdateSource = 1; // 0 = 默认服务器, 1 = Gitee

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tally_about);

        // 当前版本号信息
        TextView appVersionTv = (TextView) findViewById(R.id.tvAppVersion);
        String version = String.format(Locale.US, "%s (%d)",
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        appVersionTv.setText(version);

        // 新版本信息
        mNewVersionTv = (TextView) findViewById(R.id.tvCheckNewVersion);
        if (Updater.hasNewVersion(this)) {
            ApkModel apkModel = Updater.getNewVersionApkModelPersisted(this);
            mNewVersionTv.setText(getString(R.string.tally_about_find_new_version,
                    apkModel.getVersion(), apkModel.getBuildCode()));
            mNewVersionTv.setTextColor(getResources().getColor(R.color.libupdate_warning));
        }

        findViewById(R.id.lyAppInfo).setOnClickListener(mOnClickListener);
        findViewById(R.id.lyAppInfo).setOnLongClickListener(v -> {
            Toast.makeText(this, BuildConfig.FLAVOR, Toast.LENGTH_SHORT).show();
            return true;
        });
        findViewById(R.id.lyWeChatInfo).setOnClickListener(mOnClickListener);
        // 添加用户协议和隐私政策点击事件
        findViewById(R.id.lyUserAgreement).setOnClickListener(mOnClickListener);
        findViewById(R.id.lyPrivacyPolicy).setOnClickListener(mOnClickListener);
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setToolbarAsBack((View v) -> finish());
    }
    private View.OnClickListener mOnClickListener = (v) -> {
        int id = v.getId();
        switch (id) {
            // 检查更新
            case R.id.lyAppInfo:
                // 切换更新源
                if (mCurrentUpdateSource == 0) {
                    // 使用默认服务器更新
                    UpdateUtils.startNewClientVersionCheck(AboutActivity.this, new Updater.NewVersionCheckCallBack() {
                        @Override
                        public void onFindNewVersion(ApkModel apkModel) {
                            mNewVersionTv.setText(getString(R.string.tally_about_find_new_version,
                                    apkModel.getVersion(), apkModel.getBuildCode()));
                            mNewVersionTv.setTextColor(getResources().getColor(R.color.libupdate_warning));
                        }

                        @Override
                        public void onCheckStart() {
                            mNewVersionTv.setText("正在检查默认服务器更新...");
                            mNewVersionTv.setTextColor(getResources().getColor(R.color.appTextColorPrimary));
                        }

                        @Override
                        public void onAlreadyNewestVersion(ApkModel apkModel) {
                            mNewVersionTv.setText("已是最新版");
                            mNewVersionTv.setTextColor(getResources().getColor(R.color.appTextColorPrimary));
                        }

                        @Override
                        public void onCheckFail(Error error) {
                            // 根据错误类型显示不同的提示信息
                            String errorMsg = error.message();
                            if (errorMsg != null && (errorMsg.contains("Failed to connect") || errorMsg.contains("failed to connect") || errorMsg.contains("UnknownHostException"))) {
                                mNewVersionTv.setText("网络出差了");
                            } else {
                                mNewVersionTv.setText("更新遇到些问题");
                            }
                            mNewVersionTv.setTextColor(getResources().getColor(R.color.libupdate_warning));
                        }
                    });
                } else if (mCurrentUpdateSource == 1){
                    // 使用Gitee更新
                    UpdateUtils.startNewClientVersionCheckFromGitee(AboutActivity.this, new Updater.NewVersionCheckCallBack() {
                        @Override
                        public void onFindNewVersion(ApkModel apkModel) {
                            mNewVersionTv.setText(getString(R.string.tally_about_find_new_version,
                                    apkModel.getVersion(), apkModel.getBuildCode()));
                            mNewVersionTv.setTextColor(getResources().getColor(R.color.libupdate_warning));
                        }

                        @Override
                        public void onCheckStart() {
                            mNewVersionTv.setText("正在检查Gitee更新...");
                            mNewVersionTv.setTextColor(getResources().getColor(R.color.appTextColorPrimary));
                        }

                        @Override
                        public void onAlreadyNewestVersion(ApkModel apkModel) {
                            mNewVersionTv.setText("已是最新版");
                            mNewVersionTv.setTextColor(getResources().getColor(R.color.appTextColorPrimary));
                        }

                        @Override
                        public void onCheckFail(Error error) {
                            // 根据错误类型显示不同的提示信息
                            String errorMsg = error.message();
                            if (errorMsg != null && (errorMsg.contains("Failed to connect") || errorMsg.contains("failed to connect") || errorMsg.contains("UnknownHostException"))) {
                                mNewVersionTv.setText("网络出差了");
                            } else {
                                mNewVersionTv.setText("更新遇到些问题");
                            }
                            mNewVersionTv.setTextColor(getResources().getColor(R.color.libupdate_warning));
                        }
                    });
                }
                break;

            // 微信公众号点击
            case R.id.lyWeChatInfo:
                copyWeChatNumber();
                Toast.makeText(this, R.string.tally_about_wechat_copied, Toast.LENGTH_SHORT).show();
                break;

            // 用户协议点击
            case R.id.lyUserAgreement:
                Intent userAgreementIntent = new Intent(this, DocumentActivity.class);
                userAgreementIntent.putExtra(DocumentActivity.EXTRA_DOCUMENT_TYPE, DocumentActivity.TYPE_USER_AGREEMENT);
                startActivity(userAgreementIntent);
                break;

            // 隐私政策点击
            case R.id.lyPrivacyPolicy:
                Intent privacyPolicyIntent = new Intent(this, DocumentActivity.class);
                privacyPolicyIntent.putExtra(DocumentActivity.EXTRA_DOCUMENT_TYPE, DocumentActivity.TYPE_PRIVACY_POLICY);
                startActivity(privacyPolicyIntent);
                break;
        }
    };


    /** 复制微信公众号 */
    public void copyWeChatNumber() {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }
        ClipData.Item clipItem = new ClipData.Item("没有你也点(≧∀≦)ゞ开心!");
        ClipData clipData = new ClipData("微信公众号", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, clipItem);
        clipboardManager.setPrimaryClip(clipData);
    }
}

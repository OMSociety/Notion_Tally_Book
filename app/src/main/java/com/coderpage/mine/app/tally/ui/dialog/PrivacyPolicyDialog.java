package com.coderpage.mine.app.tally.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import com.coderpage.mine.R;

/**
 * 隐私政策和开发者公示对话框
 */
public class PrivacyPolicyDialog {

    private Context mContext;
    private DialogInterface.OnClickListener mPositiveListener;
    private DialogInterface.OnClickListener mNegativeListener;
    private boolean mIsFirstTime = true;

    public PrivacyPolicyDialog(@NonNull Context context) {
        this.mContext = context;
    }

    public PrivacyPolicyDialog setFirstTime(boolean firstTime) {
        this.mIsFirstTime = firstTime;
        return this;
    }

    public PrivacyPolicyDialog setOnPositiveClickListener(DialogInterface.OnClickListener listener) {
        this.mPositiveListener = listener;
        return this;
    }

    public PrivacyPolicyDialog setOnNegativeClickListener(DialogInterface.OnClickListener listener) {
        this.mNegativeListener = listener;
        return this;
    }

    public Dialog create() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.privacy_policy_title);

        // 构建包含可点击链接的内容
        String contentText = mContext.getString(R.string.privacy_policy_content);

        SpannableString spannableContent = new SpannableString(contentText);

        // 查找并替换《用户协议》和《隐私政策》为可点击链接
        ClickableSpan userAgreementSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(mContext, DocumentActivity.class);
                intent.putExtra(DocumentActivity.EXTRA_DOCUMENT_TYPE, DocumentActivity.TYPE_USER_AGREEMENT);
                mContext.startActivity(intent);
            }
        };

        ClickableSpan privacyPolicySpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(mContext, DocumentActivity.class);
                intent.putExtra(DocumentActivity.EXTRA_DOCUMENT_TYPE, DocumentActivity.TYPE_PRIVACY_POLICY);
                mContext.startActivity(intent);
            }
        };

        // 查找《用户协议》并设置点击事件
        int userAgreementStart = contentText.indexOf("《用户协议》");
        if (userAgreementStart >= 0) {
            spannableContent.setSpan(userAgreementSpan, userAgreementStart, userAgreementStart + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 查找《隐私政策》并设置点击事件
        int privacyPolicyStart = contentText.indexOf("《隐私政策》");
        if (privacyPolicyStart >= 0) {
            spannableContent.setSpan(privacyPolicySpan, privacyPolicyStart, privacyPolicyStart + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        SpannableString fullContent = new SpannableString(spannableContent.toString());
        // 复制原来的span
        for (Object span : spannableContent.getSpans(0, spannableContent.length(), Object.class)) {
            fullContent.setSpan(span, spannableContent.getSpanStart(span), spannableContent.getSpanEnd(span), spannableContent.getSpanFlags(span));
        }

        TextView messageView = new TextView(mContext);
        messageView.setText(fullContent);
        messageView.setMovementMethod(LinkMovementMethod.getInstance());
        // 使用已存在的dimen资源
        int paddingLarge = mContext.getResources().getDimensionPixelSize(R.dimen.spacing_large);
        int paddingNormal = mContext.getResources().getDimensionPixelSize(R.dimen.padding_normal);
        messageView.setPadding(
                paddingLarge,
                paddingNormal,
                paddingLarge,
                paddingNormal
        );

        builder.setView(messageView);
        builder.setPositiveButton(R.string.agree, mPositiveListener);

        // 总是显示"不同意"按钮，让用户可以选择拒绝
        builder.setNegativeButton(R.string.disagree, mNegativeListener);

        AlertDialog dialog = builder.create();
        // 设置对话框不可通过点击外部区域取消，确保用户必须做出选择
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }
}

package com.coderpage.mine.app.tally.databinding;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.ObservableField;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.coderpage.mine.app.tally.common.utils.TallyUtils;
import com.coderpage.mine.app.tally.data.CategoryIconHelper;
import com.coderpage.mine.common.Font;

import java.text.DecimalFormat;

/**
 * @author lc. 2018-07-21 12:15
 * @since 0.6.0
 */

public class CommonBindAdapter {

    private static final DecimalFormat MONEY_DECIMAL_FORMAT = new DecimalFormat("0.00");

    // ==================== categoryIcon 适配器 ====================

    /** 设置分类 ICON - String 类型 */
    @BindingAdapter(value = {"categoryIcon"}, requireAll = false)
    public static void setCategoryIcon(ImageView imageView, String categoryIconName) {
        int resId = CategoryIconHelper.resId(categoryIconName);
        imageView.setImageResource(resId);
    }

    /** 设置分类 ICON - AppCompatImageView */
    @BindingAdapter(value = {"categoryIcon"}, requireAll = false)
    public static void setCategoryIcon(AppCompatImageView imageView, String categoryIconName) {
        int resId = CategoryIconHelper.resId(categoryIconName);
        imageView.setImageResource(resId);
    }

    /** 设置分类 ICON - ObservableField 类型 */
    @BindingAdapter(value = {"categoryIcon"}, requireAll = false)
    public static void setCategoryIcon(ImageView imageView, ObservableField<String> categoryIconName) {
        if (categoryIconName != null && categoryIconName.get() != null) {
            setCategoryIcon(imageView, categoryIconName.get());
        }
    }

    /** 设置分类 ICON - AppCompatImageView + ObservableField */
    @BindingAdapter(value = {"categoryIcon"}, requireAll = false)
    public static void setCategoryIcon(AppCompatImageView imageView, ObservableField<String> categoryIconName) {
        if (categoryIconName != null && categoryIconName.get() != null) {
            setCategoryIcon((ImageView) imageView, categoryIconName.get());
        }
    }

    // ==================== visibility 适配器 ====================

    /** 设置 visibility - int 类型 (View.VISIBLE / GONE / INVISIBLE) */
    @BindingAdapter(value = {"android:visibility"}, requireAll = false)
    public static void setVisibility(View view, int visibility) {
        view.setVisibility(visibility);
    }

    /** 设置 visibility - boolean 类型 */
    @BindingAdapter(value = {"android:visibility"}, requireAll = false)
    public static void setVisibility(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** 设置 visibility - ConstraintLayout + int */
    @BindingAdapter(value = {"android:visibility"}, requireAll = false)
    public static void setVisibility(ConstraintLayout view, int visibility) {
        view.setVisibility(visibility);
    }

    /** 设置 visibility - ConstraintLayout + boolean */
    @BindingAdapter(value = {"android:visibility"}, requireAll = false)
    public static void setVisibility(ConstraintLayout view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // ==================== onClick 适配器 ====================

    /** 设置 onClick - 通用 View */
    @BindingAdapter(value = {"android:onClick"}, requireAll = false)
    public static void setOnClick(View view, View.OnClickListener listener) {
        view.setOnClickListener(listener);
    }

    /** 设置 onClick - ConstraintLayout */
    @BindingAdapter(value = {"android:onClick"}, requireAll = false)
    public static void setOnClick(ConstraintLayout view, View.OnClickListener listener) {
        view.setOnClickListener(listener);
    }

    /** 设置 onClick - CardView */
    @BindingAdapter(value = {"android:onClick"}, requireAll = false)
    public static void setOnClick(CardView view, View.OnClickListener listener) {
        view.setOnClickListener(listener);
    }

    /** 设置 onClick - AppCompatImageView */
    @BindingAdapter(value = {"android:onClick"}, requireAll = false)
    public static void setOnClick(AppCompatImageView view, View.OnClickListener listener) {
        view.setOnClickListener(listener);
    }

    // ==================== 其他适配器 ====================

    /** 显示金额 */
    @BindingAdapter(value = {"selected"}, requireAll = false)
    public static void setViewSelect(View view, boolean selected) {
        if (view != null) {
            view.setSelected(selected);
        }
    }

    /**
     * 设置 {@link ImageView} src
     *
     * @param imageView {@link ImageView}
     * @param src       drawable
     */
    @BindingAdapter(value = {"imageSrc"}, requireAll = true)
    public static void setImageViewDrawable(ImageView imageView, Drawable src) {
        try {
            imageView.setImageDrawable(src);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置字体
     *
     * @param font 字体
     */
    @BindingAdapter(value = {"textTypeFace"}, requireAll = false)
    public static void setTypeFace(TextView textView, Font font) {
        try {
            Typeface typeface = Typeface.createFromAsset(textView.getContext().getAssets(), "font/" + font.getName());
            textView.setTypeface(typeface);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示金额
     */
    @BindingAdapter(value = {"textMoney", "format"}, requireAll = false)
    public static void setMoneyText(TextView textView, Object money, String format) {
        if (money == null) {
            textView.setText("");
            return;
        }

        if (TextUtils.isEmpty(format)) {
            textView.setText(MONEY_DECIMAL_FORMAT.format(money));
        } else {
            textView.setText(String.format(format, MONEY_DECIMAL_FORMAT.format(money)));
        }
    }

    /**
     * 显示时间
     *
     * @param timeMills unix 时间戳（单位：毫秒）
     */
    @BindingAdapter(value = {"textDisplayTime"}, requireAll = false)
    public static void setDisplayTimeText(TextView textView, long timeMills) {
        textView.setText(TallyUtils.formatDisplayTime(timeMills));
    }
}

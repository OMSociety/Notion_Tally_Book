package com.coderpage.mine.app.tally.databinding;

import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.databinding.BindingAdapter;

/**
 * Data Binding 自定义适配器
 */
public class BindingAdapters {
    
    /**
     * 处理 visibility int 绑定
     */
    @BindingAdapter("android:visibility")
    public static void setVisibility(View view, int visibility) {
        view.setVisibility(visibility);
    }
    
    /**
     * 处理 visibility boolean 绑定
     */
    @BindingAdapter("android:visibility")
    public static void setVisibility(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
    
    /**
     * 处理 categoryIcon String 绑定
     */
    @BindingAdapter("categoryIcon")
    public static void setCategoryIcon(ImageView view, String icon) {
        if (icon == null || icon.isEmpty()) {
            return;
        }
        // 作为资源名加载
        try {
            int resId = view.getContext().getResources().getIdentifier(
                icon, "drawable", view.getContext().getPackageName()
            );
            if (resId != 0) {
                view.setImageResource(resId);
            }
        } catch (Exception e) {
            // 忽略
        }
    }
    
    /**
     * 处理 onClick lambda (通用)
     */
    @BindingAdapter("android:onClick")
    public static void setOnClick(View view, android.view.View.OnClickListener listener) {
        view.setOnClickListener(listener);
    }
    
    /**
     * 处理 AppCompatImageView click
     */
    @BindingAdapter("android:onClick")
    public static void setOnClick(AppCompatImageView view, android.view.View.OnClickListener listener) {
        view.setOnClickListener(listener);
    }
    
    /**
     * 处理 CardView click
     */
    @BindingAdapter("android:onClick")
    public static void setOnClick(CardView view, android.view.View.OnClickListener listener) {
        view.setOnClickListener(listener);
    }
}

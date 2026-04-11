package com.coderpage.mine.app.tally.persistence.preference;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author lc. 2019-04-06 23:17
 * @since 0.6.0
 *
 * 账本设置
 */

public class SettingPreference {

    private static final String FILE_NAME = "tally_setting_preference";

    private static final String KEY_HIDE_MONEY = "key_hide_money";
    private static final String KEY_BUDGET_MONTH = "key_budget_month";
    
    // AI 配置相关
    private static final String KEY_AI_API_URL = "key_ai_api_url";
    private static final String KEY_AI_API_KEY = "key_ai_api_key";
    private static final String KEY_AI_MODEL = "key_ai_model";

    /** 是否隐藏金额 */
    public static boolean getHideMoney(Context context) {
        return getPreference(context).getBoolean(KEY_HIDE_MONEY, false);
    }

    /** 设置是否隐藏金额 */
    public static void setHideMoney(Context context, boolean hideMoney) {
        getPreference(context).edit().putBoolean(KEY_HIDE_MONEY, hideMoney).apply();
    }

    /** 月预算金额 */
    public static float getBudgetMonth(Context context) {
        return getPreference(context).getFloat(KEY_BUDGET_MONTH, 0);
    }

    /** 设置月预算金额 */
    public static void setBudgetMonth(Context context, float budget) {
        getPreference(context).edit().putFloat(KEY_BUDGET_MONTH, budget).apply();
    }

    // AI 配置方法
    public static String getAiApiUrl(Context context) {
        return getPreference(context).getString(KEY_AI_API_URL, "");
    }

    public static void setAiApiUrl(Context context, String url) {
        getPreference(context).edit().putString(KEY_AI_API_URL, url).apply();
    }

    public static String getAiApiKey(Context context) {
        return getPreference(context).getString(KEY_AI_API_KEY, "");
    }

    public static void setAiApiKey(Context context, String key) {
        getPreference(context).edit().putString(KEY_AI_API_KEY, key).apply();
    }

    public static String getAiModel(Context context) {
        return getPreference(context).getString(KEY_AI_MODEL, "Qwen/Qwen2-VL-7B-Instruct");
    }

    public static void setAiModel(Context context, String model) {
        getPreference(context).edit().putString(KEY_AI_MODEL, model).apply();
    }

    private static SharedPreferences getPreference(Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }
}

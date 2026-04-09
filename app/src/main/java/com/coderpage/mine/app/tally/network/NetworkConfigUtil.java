package com.coderpage.mine.app.tally.network;

import android.text.TextUtils;

/**
 * 网络配置工具类
 *
 * 用于管理 API 地址的动态配置，支持调试/生产环境切换。
 *
 * <b>使用示例：</b>
 * <pre>
 * // 切换到测试服务器
 * NetworkConfigUtil.setDebugApiBaseUrl("https://debug-api.example.com");
 *
 * // 重置为默认配置
 * NetworkConfigUtil.resetToDefault();
 * </pre>
 *
 * @author Flan
 * @since 0.7.6
 */
public class NetworkConfigUtil {

    /** 默认 API 地址（生产环境） */
    private static final String DEFAULT_API_BASE_URL = "https://api.coderpage.com";

    /** 调试环境 API 地址（可自定义） */
    private static String sDebugApiBaseUrl = null;

    /** 是否启用调试模式 */
    private static boolean sDebugMode = false;

    /**
     * 启用调试模式
     *
     * @param debugApiBaseUrl 调试服务器地址，设为 null 则使用 DEFAULT_API_BASE_URL
     */
    public static void enableDebugMode(String debugApiBaseUrl) {
        sDebugMode = true;
        sDebugApiBaseUrl = debugApiBaseUrl;
        RequestInterceptor.setApiBaseUrl(
            TextUtils.isEmpty(debugApiBaseUrl) ? DEFAULT_API_BASE_URL : debugApiBaseUrl
        );
    }

    /**
     * 禁用调试模式，恢复默认配置
     */
    public static void disableDebugMode() {
        sDebugMode = false;
        sDebugApiBaseUrl = null;
        RequestInterceptor.resetToDefault();
    }

    /**
     * 获取当前生效的 API 地址
     *
     * @return API 地址
     */
    public static String getCurrentApiBaseUrl() {
        if (sDebugMode) {
            return TextUtils.isEmpty(sDebugApiBaseUrl) ? DEFAULT_API_BASE_URL : sDebugApiBaseUrl;
        }
        return RequestInterceptor.getApiBaseUrl() != null 
            ? RequestInterceptor.getApiBaseUrl() 
            : DEFAULT_API_BASE_URL;
    }

    /**
     * 检查是否处于调试模式
     *
     * @return true 表示调试模式
     */
    public static boolean isDebugMode() {
        return sDebugMode;
    }
}

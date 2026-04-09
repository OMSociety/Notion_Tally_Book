package com.coderpage.mine.app.tally.network;

import android.os.Build;
import android.text.TextUtils;

import com.coderpage.mine.BuildConfig;
import com.coderpage.mine.MineApp;
import com.coderpage.mine.utils.AndroidUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 网络请求拦截器
 *
 * 功能：
 * 1. 添加通用请求头（平台、设备信息、版本号等）
 * 2. 支持动态配置 API 地址
 *
 * @author lc. 2019-05-20 17:32
 * @since 0.6.2
 */
public class RequestInterceptor implements Interceptor {

    /** API 基础地址（可动态配置） */
    private static String sApiBaseUrl = null;

    /**
     * 设置 API 基础地址
     *
     * @param baseUrl API 基础地址，为空时使用默认值
     */
    public static void setApiBaseUrl(String baseUrl) {
        sApiBaseUrl = baseUrl;
    }

    /**
     * 获取当前配置的 API 基础地址
     *
     * @return API 基础地址，若未配置则返回 null
     */
    public static String getApiBaseUrl() {
        return sApiBaseUrl;
    }

    /**
     * 重置为默认配置（从 BuildConfig 读取）
     */
    public static void resetToDefault() {
        sApiBaseUrl = null;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        okhttp3.Request originalRequest = chain.request();
        String url = originalRequest.url().toString();

        // 如果设置了自定义 API 地址，进行替换
        if (!TextUtils.isEmpty(sApiBaseUrl)) {
            // 提取原 URL 的路径部分
            String originalHost = originalRequest.url().host();
            if (url.startsWith("https://" + originalHost)) {
                url = url.replace("https://" + originalHost, sApiBaseUrl);
            } else if (url.startsWith("http://" + originalHost)) {
                url = url.replace("http://" + originalHost, sApiBaseUrl);
            }
        }

        Request request = originalRequest.newBuilder()
                .url(url)
                .addHeader("Platform", "android")
                .addHeader("Platform-Version", String.valueOf(Build.VERSION.SDK_INT))
                .addHeader("Device-Id", AndroidUtils.generateDeviceId(MineApp.getAppContext()))
                .addHeader("Device-Name", Build.MODEL)
                .addHeader("Client-Version", String.valueOf(BuildConfig.VERSION_CODE))
                .addHeader("Client-Version-Name", BuildConfig.VERSION_NAME)
                .build();
        return chain.proceed(request);
    }
}

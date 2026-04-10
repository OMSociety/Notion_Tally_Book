package com.coderpage.base.utils;

import timber.log.Timber;

/**
 * 日志工具类
 *
 * 内部代理到 Timber 日志框架，保留对外接口不变。
 * MineApp.onCreate() 中已初始化 Timber.DebugTree。
 *
 * @author abner-l
 * @since 0.1.0
 */
public class LogUtils {

    public static void d(String tag, String msg) {
        Timber.tag(tag).d(msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
        Timber.tag(tag).d(tr, msg);
    }

    public static void i(String tag, String msg) {
        Timber.tag(tag).i(msg);
    }

    public static void i(String tag, String msg, Throwable tr) {
        Timber.tag(tag).i(tr, msg);
    }

    public static void w(String tag, String msg) {
        Timber.tag(tag).w(msg);
    }

    public static void w(String tag, String msg, Throwable tr) {
        Timber.tag(tag).w(tr, msg);
    }

    public static void e(String tag, String msg) {
        Timber.tag(tag).e(msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        Timber.tag(tag).e(tr, msg);
    }
}

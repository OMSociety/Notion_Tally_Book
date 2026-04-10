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
    



    // ============ 兼容旧版 API ============
    
    /**
     * 兼容旧版 LOGI 方法
     */
    public static void LOGI(String tag, String msg) {
        i(tag, msg);
    }
    
    /**
     * 兼容旧版 LOGV 方法
     */
    public static void LOGV(String tag, String msg) {
        d(tag, msg);
    }
    
    /**
     * 生成 TAG
     * @param clazz 类
     * @return TAG 字符串
     */
    public static String makeLogTag(Class<?> clazz) {
        return clazz.getSimpleName();
    }

}

    public static void d(String tag, String msg, Throwable tr) {
        Timber.tag(tag).d(tr, msg);
    



    // ============ 兼容旧版 API ============
    
    /**
     * 兼容旧版 LOGI 方法
     */
    public static void LOGI(String tag, String msg) {
        i(tag, msg);
    }
    
    /**
     * 兼容旧版 LOGV 方法
     */
    public static void LOGV(String tag, String msg) {
        d(tag, msg);
    }
    
    /**
     * 生成 TAG
     * @param clazz 类
     * @return TAG 字符串
     */
    public static String makeLogTag(Class<?> clazz) {
        return clazz.getSimpleName();
    }

}

    public static void i(String tag, String msg) {
        Timber.tag(tag).i(msg);
    



    // ============ 兼容旧版 API ============
    
    /**
     * 兼容旧版 LOGI 方法
     */
    public static void LOGI(String tag, String msg) {
        i(tag, msg);
    }
    
    /**
     * 兼容旧版 LOGV 方法
     */
    public static void LOGV(String tag, String msg) {
        d(tag, msg);
    }
    
    /**
     * 生成 TAG
     * @param clazz 类
     * @return TAG 字符串
     */
    public static String makeLogTag(Class<?> clazz) {
        return clazz.getSimpleName();
    }

}

    public static void i(String tag, String msg, Throwable tr) {
        Timber.tag(tag).i(tr, msg);
    



    // ============ 兼容旧版 API ============
    
    /**
     * 兼容旧版 LOGI 方法
     */
    public static void LOGI(String tag, String msg) {
        i(tag, msg);
    }
    
    /**
     * 兼容旧版 LOGV 方法
     */
    public static void LOGV(String tag, String msg) {
        d(tag, msg);
    }
    
    /**
     * 生成 TAG
     * @param clazz 类
     * @return TAG 字符串
     */
    public static String makeLogTag(Class<?> clazz) {
        return clazz.getSimpleName();
    }

}

    public static void w(String tag, String msg) {
        Timber.tag(tag).w(msg);
    



    // ============ 兼容旧版 API ============
    
    /**
     * 兼容旧版 LOGI 方法
     */
    public static void LOGI(String tag, String msg) {
        i(tag, msg);
    }
    
    /**
     * 兼容旧版 LOGV 方法
     */
    public static void LOGV(String tag, String msg) {
        d(tag, msg);
    }
    
    /**
     * 生成 TAG
     * @param clazz 类
     * @return TAG 字符串
     */
    public static String makeLogTag(Class<?> clazz) {
        return clazz.getSimpleName();
    }

}

    public static void w(String tag, String msg, Throwable tr) {
        Timber.tag(tag).w(tr, msg);
    



    // ============ 兼容旧版 API ============
    
    /**
     * 兼容旧版 LOGI 方法
     */
    public static void LOGI(String tag, String msg) {
        i(tag, msg);
    }
    
    /**
     * 兼容旧版 LOGV 方法
     */
    public static void LOGV(String tag, String msg) {
        d(tag, msg);
    }
    
    /**
     * 生成 TAG
     * @param clazz 类
     * @return TAG 字符串
     */
    public static String makeLogTag(Class<?> clazz) {
        return clazz.getSimpleName();
    }

}

    public static void e(String tag, String msg) {
        Timber.tag(tag).e(msg);
    



    // ============ 兼容旧版 API ============
    
    /**
     * 兼容旧版 LOGI 方法
     */
    public static void LOGI(String tag, String msg) {
        i(tag, msg);
    }
    
    /**
     * 兼容旧版 LOGV 方法
     */
    public static void LOGV(String tag, String msg) {
        d(tag, msg);
    }
    
    /**
     * 生成 TAG
     * @param clazz 类
     * @return TAG 字符串
     */
    public static String makeLogTag(Class<?> clazz) {
        return clazz.getSimpleName();
    }

}

    public static void e(String tag, String msg, Throwable tr) {
        Timber.tag(tag).e(tr, msg);
    



    // ============ 兼容旧版 API ============
    
    /**
     * 兼容旧版 LOGI 方法
     */
    public static void LOGI(String tag, String msg) {
        i(tag, msg);
    }
    
    /**
     * 兼容旧版 LOGV 方法
     */
    public static void LOGV(String tag, String msg) {
        d(tag, msg);
    }
    
    /**
     * 生成 TAG
     * @param clazz 类
     * @return TAG 字符串
     */
    public static String makeLogTag(Class<?> clazz) {
        return clazz.getSimpleName();
    }

}




    // ============ 兼容旧版 API ============
    
    /**
     * 兼容旧版 LOGI 方法
     */
    public static void LOGI(String tag, String msg) {
        i(tag, msg);
    }
    
    /**
     * 兼容旧版 LOGV 方法
     */
    public static void LOGV(String tag, String msg) {
        d(tag, msg);
    }
    
    /**
     * 生成 TAG
     * @param clazz 类
     * @return TAG 字符串
     */
    public static String makeLogTag(Class<?> clazz) {
        return clazz.getSimpleName();
    }

}

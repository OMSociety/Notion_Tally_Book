package com.coderpage.mine.utils;

import android.util.Log;

/**
 * 应用日志工具类
 *
 * 封装 Android Log，提供统一的日志输出接口。
 * 支持日志级别过滤、Tag统一管理、可选的日志开关控制。
 *
 * <b>使用示例：</b>
 * <pre>
 * // 基本用法（与 Android Log 用法一致）
 * AppLogger.d("Tag", "Debug message");
 * AppLogger.i("Tag", "Info message");
 * AppLogger.w("Tag", "Warning message");
 * AppLogger.e("Tag", "Error message");
 *
 * // 带异常信息的日志
 * try {
 *     // some code
 * } catch (Exception e) {
 *     AppLogger.e("Tag", "Error occurred", e);
 * }
 *
 * // 临时禁用日志（生产环境建议在 Application 中设置）
 * AppLogger.setEnabled(false);
 * </pre>
 *
 * @author Flan
 * @since 0.7.6
 */
public class AppLogger {

    /** 默认 Tag */
    private static final String DEFAULT_TAG = "MineApp";

    /** 是否启用日志输出（生产环境设为 false） */
    private static boolean sEnabled = true;

    /**
     * 设置是否启用日志
     *
     * @param enabled true 启用，false 禁用
     */
    public static void setEnabled(boolean enabled) {
        sEnabled = enabled;
    }

    /**
     * 检查日志是否启用
     *
     * @return true 表示启用
     */
    public static boolean isEnabled() {
        return sEnabled;
    }

    /**
     * Verbose 日志
     */
    public static void v(String tag, String message) {
        if (sEnabled) {
            Log.v(tag, message);
        }
    }

    /**
     * Verbose 日志（带异常）
     */
    public static void v(String tag, String message, Throwable t) {
        if (sEnabled) {
            Log.v(tag, message, t);
        }
    }

    /**
     * Debug 日志
     */
    public static void d(String tag, String message) {
        if (sEnabled) {
            Log.d(tag, message);
        }
    }

    /**
     * Debug 日志（带异常）
     */
    public static void d(String tag, String message, Throwable t) {
        if (sEnabled) {
            Log.d(tag, message, t);
        }
    }

    /**
     * Info 日志
     */
    public static void i(String tag, String message) {
        if (sEnabled) {
            Log.i(tag, message);
        }
    }

    /**
     * Info 日志（带异常）
     */
    public static void i(String tag, String message, Throwable t) {
        if (sEnabled) {
            Log.i(tag, message, t);
        }
    }

    /**
     * Warning 日志
     */
    public static void w(String tag, String message) {
        if (sEnabled) {
            Log.w(tag, message);
        }
    }

    /**
     * Warning 日志（带异常）
     */
    public static void w(String tag, String message, Throwable t) {
        if (sEnabled) {
            Log.w(tag, message, t);
        }
    }

    /**
     * Error 日志
     */
    public static void e(String tag, String message) {
        if (sEnabled) {
            Log.e(tag, message);
        }
    }

    /**
     * Error 日志（带异常）
     */
    public static void e(String tag, String message, Throwable t) {
        if (sEnabled) {
            Log.e(tag, message, t);
        }
    }

    // ==================== 使用默认 Tag 的便捷方法 ====================

    public static void v(String message) {
        v(DEFAULT_TAG, message);
    }

    public static void d(String message) {
        d(DEFAULT_TAG, message);
    }

    public static void i(String message) {
        i(DEFAULT_TAG, message);
    }

    public static void w(String message) {
        w(DEFAULT_TAG, message);
    }

    public static void e(String message) {
        e(DEFAULT_TAG, message);
    }

    public static void e(String message, Throwable t) {
        e(DEFAULT_TAG, message, t);
    }
}

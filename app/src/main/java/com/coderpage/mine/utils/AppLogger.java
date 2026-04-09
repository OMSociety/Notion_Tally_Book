package com.coderpage.mine.utils;

import android.util.Log;

import com.coderpage.mine.BuildConfig;

/**
 * 应用日志工具类
 *
 * 封装 Android Log，提供统一的日志输出接口。
 * 基于 BuildConfig.DEBUG 自动判断日志级别：
 * <ul>
 *   <li>DEBUG 构建：所有级别日志正常输出</li>
 *   <li>RELEASE 构建：仅输出 w / i / e 级别，v / d 级别静默</li>
 * </ul>
 *
 * 也可通过 {@link #setEnabled(boolean)} 手动覆盖。
 *
 * <b>使用示例：</b>
 * <pre>
 * AppLogger.d("Tag", "Debug message");  // DEBUG 可见，RELEASE 静默
 * AppLogger.e("Tag", "Error message");  // 始终可见
 * AppLogger.setEnabled(false);          // 完全关闭日志
 * </pre>
 *
 * @author Flan
 * @since 0.7.6
 */
public class AppLogger {

    /** 默认 Tag */
    private static final String DEFAULT_TAG = "MineApp";

    /** 是否启用日志输出（默认跟随 BuildConfig.DEBUG） */
    private static boolean sEnabled = BuildConfig.DEBUG;

    /**
     * 设置是否启用日志
     *
     * @param enabled true 启用，false 禁用所有日志
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

    // ==================== Verbose（仅 DEBUG 输出） ====================

    public static void v(String tag, String message) {
        if (sEnabled && BuildConfig.DEBUG) {
            Log.v(tag, message);
        }
    }

    public static void v(String tag, String message, Throwable t) {
        if (sEnabled && BuildConfig.DEBUG) {
            Log.v(tag, message, t);
        }
    }

    // ==================== Debug（仅 DEBUG 输出） ====================

    public static void d(String tag, String message) {
        if (sEnabled && BuildConfig.DEBUG) {
            Log.d(tag, message);
        }
    }

    public static void d(String tag, String message, Throwable t) {
        if (sEnabled && BuildConfig.DEBUG) {
            Log.d(tag, message, t);
        }
    }

    // ==================== Info（始终输出） ====================

    public static void i(String tag, String message) {
        if (sEnabled) {
            Log.i(tag, message);
        }
    }

    public static void i(String tag, String message, Throwable t) {
        if (sEnabled) {
            Log.i(tag, message, t);
        }
    }

    // ==================== Warning（始终输出） ====================

    public static void w(String tag, String message) {
        if (sEnabled) {
            Log.w(tag, message);
        }
    }

    public static void w(String tag, String message, Throwable t) {
        if (sEnabled) {
            Log.w(tag, message, t);
        }
    }

    // ==================== Error（始终输出） ====================

    public static void e(String tag, String message) {
        if (sEnabled) {
            Log.e(tag, message);
        }
    }

    public static void e(String tag, String message, Throwable t) {
        if (sEnabled) {
            Log.e(tag, message, t);
        }
    }

    // ==================== 默认 Tag 便捷方法 ====================

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

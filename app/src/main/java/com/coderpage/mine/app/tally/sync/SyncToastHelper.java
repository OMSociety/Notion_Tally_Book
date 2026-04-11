package com.coderpage.mine.app.tally.sync;

import android.content.Context;
import android.widget.Toast;

/**
 * 同步状态 Toast 帮助类
 * 
 * @author Flandre Scarlet
 */
public class SyncToastHelper {

    /**
     * 显示同步成功 Toast
     */
    public static void showSyncSuccess(Context context, int added, int updated) {
        String message;
        if (added == 0 && updated == 0) {
            message = "同步完成，数据已是最新";
        } else {
            message = "同步成功：新增" + added + "条，更新" + updated + "条";
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示同步失败 Toast
     */
    public static void showSyncError(Context context, String error) {
        String message;
        if (error == null || error.isEmpty()) {
            message = "同步失败，请检查网络连接";
        } else if (error.contains("401")) {
            message = "同步失败：API Key 无效或已过期";
        } else if (error.contains("404")) {
            message = "同步失败：Database ID 不正确";
        } else if (error.contains("network") || error.contains("Network")) {
            message = "同步失败：网络连接异常";
        } else if (error.contains("timeout") || error.contains("Timeout")) {
            message = "同步失败：请求超时，请重试";
        } else if (error.length() > 30) {
            message = "同步失败：" + error.substring(0, 30) + "...";
        } else {
            message = "同步失败：" + error;
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * 显示同步中 Toast
     */
    public static void showSyncing(Context context) {
        Toast.makeText(context, "正在同步...", Toast.LENGTH_SHORT).show();
    }
}

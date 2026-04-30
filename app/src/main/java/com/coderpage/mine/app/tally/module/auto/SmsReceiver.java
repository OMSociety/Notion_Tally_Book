// 文件路径: app/src/main/java/com/coderpage/mine/app/tally/module/auto/SmsReceiver.java
package com.coderpage.mine.app.tally.module.auto;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.coderpage.base.utils.UIUtils;
import com.coderpage.mine.MineApp;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.RecordType;
import com.coderpage.mine.app.tally.eventbus.EventRecordAdd;
import com.coderpage.mine.app.tally.module.edit.record.RecordRepository;
import com.coderpage.mine.app.tally.module.home.HomeActivity;
import com.coderpage.mine.app.tally.module.setting.SettingWorkerConstant;
import com.coderpage.mine.app.tally.persistence.model.CategoryModel;
import com.coderpage.mine.app.tally.persistence.model.Record;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.persistence.database.MineDatabase;
import com.coderpage.mine.utils.AndroidUtils;

import com.coderpage.concurrency.MineExecutors;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    private TallyDatabase mDataBase;

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();
        MineExecutors.ioExecutor().execute(() -> {
            try {
                mDataBase = TallyDatabase.getInstance();

                // 检查是否启用了短信识别功能
                if (!isSmsRecognitionEnabled(context)) {
                    Log.d(TAG, "短信识别功能未开启");
                    return;
                }

                // 获取短信内容
                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    return;
                }

                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus == null || pdus.length == 0) {
                    return;
                }

                // 解析短信
                for (Object pdu : pdus) {
                    SmsMessage smsMessage;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        String format = intent.getStringExtra("format");
                        smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format != null ? format : "3gpp");
                    } else {
                        smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                    }
                    if (smsMessage != null) {
                        String sender = smsMessage.getDisplayOriginatingAddress();
                        String messageBody = smsMessage.getMessageBody();

                        Log.d(TAG, "发件人: " + sender + ", 内容: " + messageBody);

                        // 检查是否在检测名单中
                        if (isInDetectionList(context, sender)) {
                            // 提示正在处理
                            UIUtils.showToastShort(context, "短信记账处理中...");
                            // 解析金额
                            AiIdentifyAmount amount = extractAmount(messageBody);
                            if (amount != null) {
                                // 保存记录
                                saveRecordDirectly(amount);
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish();
            }
        });
    }

    /**
     * 检查是否启用了短信识别功能
     */
    private boolean isSmsRecognitionEnabled(Context context) {
        try {
            com.coderpage.mine.persistence.entity.KeyValue keyValue =
                    MineDatabase.getInstance().keyValueDao()
                            .query(SettingWorkerConstant.KEY_SMS_RECOGNITION_ENABLED);

            if (keyValue != null && keyValue.getValue() != null) {
                return Boolean.parseBoolean(keyValue.getValue());
            }
        } catch (Exception e) {
            Log.e(TAG, "获取短信识别开关状态失败", e);
        }
        return false; // 默认关闭
    }

    /**
     * 检查发送方是否在检测名单中
     */
    private boolean isInDetectionList(Context context, String sender) {
        try {
            com.coderpage.mine.persistence.entity.KeyValue keyValue =
                    MineDatabase.getInstance().keyValueDao()
                            .query(SettingWorkerConstant.KEY_DETECTION_LIST);

            if (keyValue != null && keyValue.getValue() != null && !keyValue.getValue().isEmpty()) {
                String[] detectionList = keyValue.getValue().split(",|，");
                for (String number : detectionList) {
                    if (sender.contains(number.trim())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取检测名单失败", e);
        }
        return false;
    }

    /**
     * 从短信内容中提取金额
     */
    private AiIdentifyAmount extractAmount(String messageBody) {
        //删除关键词
        List<String> delKeywords = Arrays.asList("支付机构","快捷支付协议","支付协议","代付协议","支付宝","代发工资客户","活动补贴","活动返现");
        //收入类
        List<String> incomeKeywords = Arrays.asList("工资", "奖金", "薪资", "汇入", "转入", "到账", "收入", "进账", "退款", "存入", "收款", "余额增加");
        //支出类
        List<String> expenseKeywords = Arrays.asList("消费", "支出", "支付", "付款", "扣款", "扣收", "刷卡消费", "取现", "转账", "转出", "汇出", "提现", "手续费", "年费", "管理费", "利息支出", "扣账", "余额减少", "还款");

        //创建返回值
        AiIdentifyAmount amount = new AiIdentifyAmount(false, "other", 0);

        //处理短信内容, 短信内容去除删除关键词
        for (String delKeyword : delKeywords) {
            messageBody = messageBody.replace(delKeyword, "");
        }
        // 判断是否为收入
        boolean isIncome = false;
        for (String keyword : incomeKeywords) {
            int index = messageBody.indexOf(keyword);
            if (index != -1) {
                isIncome = true;
                break;
            }
        }

        // 判断是否为支出
        boolean isExpense = false;
        for (String keyword : expenseKeywords) {
            int index = messageBody.indexOf(keyword);
            if (index != -1) {
                isExpense = true;
                break;
            }
        }

        // 如果既不是收入也不是支出，或者同时是收入和支出（有歧义），直接返回
        if ((!isIncome && !isExpense) || (isIncome && isExpense)) {
            return amount;
        }

        // 设置收支类型
        List<String> keywords = null;
        if (isIncome) {
            amount.setExpenseOrIncome("income");
            amount.setBill(true);
            keywords = incomeKeywords;
        } else if (isExpense) {
            amount.setExpenseOrIncome("expense");
            amount.setBill(true);
            keywords = expenseKeywords;
        }

        // 提取金额（使用正则表达式匹配金额）
        // 提取金额（使用正则表达式匹配金额）
        // 1. 截取关键字后面的所有短信内容
        String contentAfterKeyword = messageBody;
        for (String keyword : keywords) {
            int index = messageBody.indexOf(keyword);
            if (index != -1) {
                contentAfterKeyword = messageBody.substring(index + keyword.length());
                break;
            }
        }

        // 2. 使用正则表达式匹配所有可能的金额
        Pattern pattern = Pattern.compile("[+-]?[0-9][0-9,.]*\\.?[0-9]*");
        Matcher matcher = pattern.matcher(contentAfterKeyword);

        // 查找所有匹配项
        List<String> matchedAmounts = new ArrayList<>();
        while (matcher.find()) {
            matchedAmounts.add(matcher.group());
        }

        // 如果有匹配项，选择第一个
        if (!matchedAmounts.isEmpty()) {
            String matchedAmount = matchedAmounts.get(0);

            // 3. 处理金额数值，删除逗号
            String processedAmount = matchedAmount.replace(",", "");

            // 检查小数点数量
            int dotCount = processedAmount.length() - processedAmount.replace(".", "").length();

            if (dotCount > 1) {
                // 如果有多个点，直接返回0
                amount.setMoney(0);
            } else {
                try {
                    double money = Double.parseDouble(processedAmount);
                    amount.setMoney(Math.abs(money)); // 使用绝对值
                } catch (NumberFormatException e) {
                    amount.setMoney(0);
                }
            }
        }

        return amount;
    }

    /**
     * 直接保存记录数据，不依赖界面状态
     *
     * @param amount 识别出的金额信息
     */
    private void saveRecordDirectly(AiIdentifyAmount amount) {
        Context context = MineApp.getAppContext();
        //如果不是账单，则返回
        if (!Boolean.TRUE.equals(amount.getBill())) return;

        //如果识别不出来是支出或者是收入, 不记录
        String expenseOrIncome = amount.getExpenseOrIncome();
        if (amount.getExpenseOrIncome().equals("other")) return;

        //如果是0元也不记录
        if (amount.getMoney() == 0) return;

        //确定分类
        RecordType type = "expense".equals(expenseOrIncome) ? RecordType.EXPENSE : RecordType.INCOME;
        CategoryModel defaultCategory = null;
        if (type == RecordType.EXPENSE) {
            List<CategoryModel> categoryList = mDataBase.categoryDao().allExpenseCategory();
            for (CategoryModel categoryModel : categoryList) {
                if (categoryModel.getName().equals(context.getString(R.string.tyOther))) {
                    defaultCategory = categoryModel;
                }
            }
        } else if (type == RecordType.INCOME) {
            List<CategoryModel> categoryList = mDataBase.categoryDao().allIncomeCategory();
            for (CategoryModel categoryModel : categoryList) {
                if (categoryModel.getName().equals(context.getString(R.string.tyOther))) {
                    defaultCategory = categoryModel;
                }
            }
        }
        if (defaultCategory == null) return;


        //创建记录
        Record record = new Record();
        record.setType(type == RecordType.EXPENSE ? Record.TYPE_EXPENSE : Record.TYPE_INCOME);

        // 创建新的记录
        record.setSyncId(AndroidUtils.generateUUID());
        record.setAmount(Math.abs(amount.getMoney()));
        record.setTime(System.currentTimeMillis());
        record.setDesc("短信识别记录");

        // 设置默认分类
        record.setCategoryIcon(defaultCategory.getIcon());
        record.setCategoryName(defaultCategory.getName());
        record.setCategoryUniqueName(defaultCategory.getUniqueName());

        // 保存记录到数据库
        RecordRepository repository = new RecordRepository();
        repository.saveRecord(record, result -> {
            if (result.isOk()) {
                record.setId(result.data());
                EventBus.getDefault().post(new EventRecordAdd(record));
                Log.d(TAG, "记录保存成功: " + record.getId());
                // 添加成功提示
                UIUtils.showToastShort(context, "短信记账成功: " +
                        (type == RecordType.EXPENSE ? "支出" : "收入") +
                        Math.abs(amount.getMoney()) + "元");

                // 发送通知到通知中心
                sendNotification(context, type, amount);
            } else {
                Log.e(TAG, "记录保存失败: " + result.error());
            }
        });
    }

    /**
     * 发送通知到通知中心
     *
     * @param context 上下文
     * @param type 记录类型
     * @param amount 金额信息
     */
    private void sendNotification(Context context, RecordType type, AiIdentifyAmount amount) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 创建通知渠道 (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "sms_record_channel",
                    "短信记账通知",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("短信自动记账通知");
            notificationManager.createNotificationChannel(channel);
        }

        // 构建通知内容
        String title = "短信自动记账成功";
        String content = (type == RecordType.EXPENSE ? "支出" : "收入") +
                Math.abs(amount.getMoney()) + "元";

        // 创建点击通知后打开的意图
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // 构建通知
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, "sms_record_channel");
        } else {
            builder = new Notification.Builder(context);
        }

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // 发送通知
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}

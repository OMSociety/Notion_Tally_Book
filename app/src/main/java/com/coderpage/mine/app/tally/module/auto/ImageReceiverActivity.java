package com.coderpage.mine.app.tally.module.auto;

import static java.lang.Math.abs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.coderpage.base.utils.UIUtils;
import com.coderpage.mine.MineApp;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.ai.AiApiConfig;
import com.coderpage.mine.app.tally.ai.AiRecognizer;
import com.coderpage.mine.app.tally.ai.AiRecognizerFactory;
import com.coderpage.mine.app.tally.ai.RecognitionResult;
import com.coderpage.mine.app.tally.common.RecordType;
import com.coderpage.mine.app.tally.eventbus.EventRecordAdd;
import com.coderpage.mine.app.tally.module.edit.record.RecordRepository;
import com.coderpage.mine.app.tally.module.home.HomeActivity;
import com.coderpage.mine.app.tally.persistence.model.CategoryModel;
import com.coderpage.mine.app.tally.persistence.model.Record;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.utils.AndroidUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageReceiverActivity extends AppCompatActivity {
    private static final String TAG = "ImageReceiverActivity";

    private TallyDatabase mDataBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataBase = TallyDatabase.getInstance();

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("image/")) {
            Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (imageUri != null) {
                try {
                    processAndPrintImage(imageUri);
                } catch (Exception e) {
                    Log.e(TAG, "处理图片时出错", e);
                    throw new RuntimeException(e);
                }
            }
        }
        finish();
    }

    private void processAndPrintImage(Uri imageUri) throws Exception {
        Context context = MineApp.getAppContext();
        
        // 检查 API 配置
        AiApiConfig config = AiApiConfig.load(context);
        if (!config.isValid()) {
            UIUtils.showToastShort(context, "请先在设置中配置 AI API");
            return;
        }

        // 转换为 Base64
        String base64Image = uriToBase64(imageUri);
        if (base64Image == null) {
            UIUtils.showToastShort(context, "图片处理失败");
            return;
        }

        // 创建识别器
        AiRecognizer recognizer = AiRecognizerFactory.create(config);

        // 异步调用
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                RecognitionResult result = recognizer.recognize(base64Image);

                if (result.success) {
                    saveRecordDirectly(result);
                } else {
                    String title = "AI 自动记账失败";
                    String content = result.errorMessage;
                    sendNotification(context, title, content);
                }

            } catch (Exception e) {
                Log.e(TAG, "AI 请求失败", e);
                String title = "AI 自动记账失败";
                String content = "错误: " + e.getMessage();
                sendNotification(context, title, content);
            } finally {
                runOnUiThread(() -> finish());
            }
        });
    }

    // 将Uri转换为Base64编码
    private String uriToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);

            // 先解码图片尺寸，不加载到内存中
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            inputStream.close();
            inputStream = getContentResolver().openInputStream(uri);

            // 设置缩放比例为3（缩小一半）
            options.inJustDecodeBounds = false;
            options.inSampleSize = 3;

            // 解码图片并缩放
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // 将Bitmap转换为Base64
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] bytes = baos.toByteArray();
            bitmap.recycle();

            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "图片转换Base64失败", e);
            return null;
        }
    }

    /**
     * 直接保存记录数据，不依赖界面状态
     */
    private void saveRecordDirectly(RecognitionResult result) {
        Context context = MineApp.getAppContext();
        
        // 如果不是账单，则返回
        if (!result.isBill) return;

        // 如果识别不出来是支出还是收入，不记录
        String expenseOrIncome = result.type;
        if (expenseOrIncome == null || expenseOrIncome.equals("other")) return;

        // 确定分类
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

        // 创建记录
        Record record = new Record();
        record.setType(type == RecordType.EXPENSE ? Record.TYPE_EXPENSE : Record.TYPE_INCOME);

        record.setSyncId(AndroidUtils.generateUUID());
        record.setAmount(abs(result.amount));
        record.setTime(System.currentTimeMillis());
        record.setDesc("AI 识别记账");

        record.setCategoryIcon(defaultCategory.getIcon());
        record.setCategoryName(defaultCategory.getName());
        record.setCategoryUniqueName(defaultCategory.getUniqueName());

        // 保存记录到数据库
        RecordRepository repository = new RecordRepository();
        repository.saveRecord(record, result1 -> {
            if (result1.isOk()) {
                record.setId(result1.data());
                EventBus.getDefault().post(new EventRecordAdd(record));
                Log.d(TAG, "记录保存成功: " + record.getId());

                String title = "AI 自动记账成功:";
                String content = (type == RecordType.EXPENSE ? "支出" : "收入") + Math.abs(result.amount) + "元";
                runOnUiThread(() -> {
                    Toast.makeText(this, title + content, Toast.LENGTH_LONG).show();
                });
            } else {
                Log.e(TAG, "记录保存失败: " + result1.error());
            }
        });
    }

    /**
     * 发送通知到通知中心
     */
    private void sendNotification(Context context, String title, String content) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 创建通知渠道 (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "ai_record_channel",
                    "AI 记账通知",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("AI 自动记账通知");
            notificationManager.createNotificationChannel(channel);
        }

        // 创建点击通知后打开的意图
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // 构建通知
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, "ai_record_channel");
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

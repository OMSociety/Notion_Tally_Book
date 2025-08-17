package com.coderpage.mine.app.tally.module.auto;

import static java.lang.Math.abs;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.fastjson.JSONObject;
import com.coderpage.mine.MineApp;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.common.RecordType;
import com.coderpage.mine.app.tally.eventbus.EventRecordAdd;
import com.coderpage.mine.app.tally.module.edit.record.RecordRepository;
import com.coderpage.mine.app.tally.module.setting.SettingWorkerConst;
import com.coderpage.mine.app.tally.persistence.model.CategoryModel;
import com.coderpage.mine.app.tally.persistence.model.Record;
import com.coderpage.mine.app.tally.persistence.sql.TallyDatabase;
import com.coderpage.mine.persistence.database.MineDatabase;
import com.coderpage.mine.utils.AndroidUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageReceiverActivity extends AppCompatActivity {
    private static final String TAG = "ImageReceiverActivity";

    private TallyDatabase mDataBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataBase = TallyDatabase.getInstance();

        // 检查是否是通过分享接收到的图片
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("image/")) {
            Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (imageUri != null) {
                // 处理接收到的图片
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
        // 在这里实现您的"打印"逻辑
        // 可能是保存到记事本、显示在应用中等操作
        String base64Image = uriToBase64(imageUri);
        String imageData = "data:image/png;base64," + base64Image;

        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("image", imageData),
//                        Collections.singletonMap("text", "快速识别图片中的账单信息，不要深度分析。回答三个问题: 1.是否涉及支付或收入? 2.是支出还是收入? 3.金额是多少?"),
//                        Collections.singletonMap("text", "回答示例: {\"bill\":true,\"expenseOrIncome\":\"expense\",\"money\":23}"),
//                        Collections.singletonMap("text", "严格按照json格式输出,不需要额外表述: bill[true,false], expenseOrIncome[expense,income,other], money[数值]")
                        Collections.singletonMap("text", "回答三个问题: 1.是否涉及支付或收入? 2.是支出还是收入? 3.金额是多少?"),
                        Collections.singletonMap("text", "严格按照以下JSON格式输出，不要包含任何额外文字:\n" +
                                "{\"bill\":true/false,\"expenseOrIncome\":\"expense/income/other\",\"money\":数值}\n" +
                                "示例: {\"bill\":true,\"expenseOrIncome\":\"expense\",\"money\":23.5}"),
                        Collections.singletonMap("text", "重要: 只返回JSON，不要添加任何解释或额外文本")

                )).build();

        // 从设置中获取 API Key 和模型名称
        String apiKey = getApiKeyFromSettings();
        String model = getAiModelFromSettings();

        if (apiKey ==  null){
            runOnUiThread(() -> {
                // 显示错误提示
                android.widget.Toast.makeText(this, "请先在设置中配置 AI API Key",
                        android.widget.Toast.LENGTH_LONG).show();
            });
            finish();
            return;
        }

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(apiKey)
                // 此处以qwen-vl-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model(model)
                .message(userMessage)
                .build();

        // 异步调用
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                MultiModalConversationResult result = conv.call(param);

                AiIdentifyAmount amount = getAiIdentifyAmount(result);

                saveRecordDirectly(amount);

            } catch (Exception e) {
                Log.e(TAG, "ai请求失败", e);
                runOnUiThread(() -> {
                    // 显示错误提示
                    android.widget.Toast.makeText(this, "ai请求失败: " + e.getMessage(),
                            android.widget.Toast.LENGTH_LONG).show();
                });
            } finally {
                runOnUiThread(() -> finish());
            }
        });
    }

    // 将Uri转换为Base64编码
    private String uriToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);

            // 首先解码图片尺寸，不加载到内存中
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            // 重置输入流
            inputStream.close();
            inputStream = getContentResolver().openInputStream(uri);

            // 设置缩放比例为2（缩小一半）
            options.inJustDecodeBounds = false;
            options.inSampleSize = 2;

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

    private AiIdentifyAmount getAiIdentifyAmount(MultiModalConversationResult result) {
        //提取数据
        List<MultiModalConversationOutput.Choice> choices = result.getOutput().getChoices();
        MultiModalConversationOutput.Choice choice = choices.get(0);
        MultiModalMessage message = choice.getMessage();
        List<Map<String, Object>> content = message.getContent();
        Map<String, Object> stringObjectMap1 = content.get(0);
        String valueStr = stringObjectMap1.values().iterator().next().toString();

        Pattern pattern = Pattern.compile("\\{[^{}]*+(?:\\{[^{}]*+\\}[^{}]*+)*+\\}");
        Matcher matcher = pattern.matcher(valueStr);
        if (matcher.find()) {
            //找到了
            String DataString = matcher.group();
            return JSONObject.parseObject(DataString, AiIdentifyAmount.class);
        } else {
            AiIdentifyAmount aiIdentifyAmount = new AiIdentifyAmount();

            //没有找到, 那就硬解字符串
            String DataString = result.toString();

            //解出来了
            Pattern pattern1 = Pattern.compile("bill\\s*[:\\s]*\\s*(true|false)");
            Pattern pattern2 = Pattern.compile("expenseOrIncome\\s*[:\\s]*\\s*(expense|income|other)");
            Pattern pattern3 = Pattern.compile("\"money\"\\s*[:\\s]*([+-]?\\d+(?:\\.\\d+)?)");

            Matcher matcher1 = pattern1.matcher(DataString);
            if (matcher1.find()) {
                Boolean booleanValue = Boolean.valueOf(matcher.group());
                if (booleanValue == false) {
                    return getZeroAmount();
                } else {
                    aiIdentifyAmount.setBill(true);
                }
            }

            Matcher matcher2 = pattern2.matcher(DataString);
            if (matcher2.find()) {
                String expenseOrIncome = matcher.group();
                if (expenseOrIncome.equals("other")) {
                    return getZeroAmount();
                } else {
                    aiIdentifyAmount.setExpenseOrIncome(expenseOrIncome);
                }
            }

            Matcher matcher3 = pattern3.matcher(DataString);
            if (matcher3.find()) {
                aiIdentifyAmount.setMoney(Double.valueOf(matcher.group()));
            }
            return aiIdentifyAmount;
        }
    }

    private AiIdentifyAmount getZeroAmount() {
        AiIdentifyAmount aiIdentifyAmount = new AiIdentifyAmount();
        aiIdentifyAmount.setBill(false);
        aiIdentifyAmount.setExpenseOrIncome("other");
        aiIdentifyAmount.setMoney(0D);
        return aiIdentifyAmount;
    }

    /**
     * 直接保存记录数据，不依赖界面状态
     *
     * @param amount 识别出的金额信息
     */
    private void saveRecordDirectly(AiIdentifyAmount amount) {
        Context context = MineApp.getAppContext();
        //如果不是账单，则返回
        if (amount.getBill() == false) return;

        //如果识别不出来是支出或者是收入, 不记录
        String expenseOrIncome = amount.getExpenseOrIncome();
        if (expenseOrIncome.equals("other")) return;

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
        record.setAmount(abs(amount.getMoney()));
        record.setTime(System.currentTimeMillis());
        record.setDesc("AI识别记录");

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
            } else {
                Log.e(TAG, "记录保存失败: " + result.error());
            }
        });
    }

    /**
     * 从数据库获取 API Key
     *
     * @return API Key 字符串
     */
    private String getApiKeyFromSettings() {
        try {
            // 查询数据库中的 API Key 设置
            com.coderpage.mine.persistence.entity.KeyValue keyValue =
                    MineDatabase.getInstance().keyValueDao().query(SettingWorkerConst.KEY_API_KEY);

            // 如果没有设置，则使用默认值
            if (keyValue != null && keyValue.getValue() != null && !keyValue.getValue().isEmpty()) {
                return keyValue.getValue();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取 API Key 失败", e);
        }
        // 返回默认的 API Key
        return null;
    }

    /**
     * 从数据库获取 AI 模型名称
     *
     * @return 模型名称字符串
     */
    private String getAiModelFromSettings() {
        try {
            // 查询数据库中的 AI 模型设置
            com.coderpage.mine.persistence.entity.KeyValue keyValue =
                    MineDatabase.getInstance().keyValueDao().query(SettingWorkerConst.KEY_AI_MODEL);

            // 如果没有设置，则使用默认值
            if (keyValue != null && keyValue.getValue() != null && !keyValue.getValue().isEmpty()) {
                return keyValue.getValue();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取 AI 模型名称失败", e);
        }
        // 返回默认的模型名称
        return "qwen2.5-vl-32b-instruct";
    }
}

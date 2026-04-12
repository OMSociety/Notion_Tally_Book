package com.coderpage.mine.app.tally.ai;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class OpenAiRecognizerTest {

    @Test
    public void parseResponse_shouldHandleNoisyJsonContent() throws Exception {
        OpenAiRecognizer recognizer = new OpenAiRecognizer(buildConfig());
        Method parseResponse = OpenAiRecognizer.class.getDeclaredMethod("parseResponse", String.class);
        parseResponse.setAccessible(true);

        String response = "{\"choices\":[{\"message\":{\"content\":\"识别结果如下：\\n{\\\"bill\\\":true,\\\"expenseOrIncome\\\":\\\"expense\\\",\\\"money\\\":23.5}\\n谢谢\"}}]}";
        RecognitionResult result = (RecognitionResult) parseResponse.invoke(recognizer, response);

        Assert.assertTrue(result.success);
        Assert.assertTrue(result.isBill);
        Assert.assertEquals("expense", result.type);
        Assert.assertEquals(23.5, result.amount, 0.0001);
    }

    @Test
    public void parseResponse_shouldReturnErrorWhenResponseHasNoChoices() throws Exception {
        OpenAiRecognizer recognizer = new OpenAiRecognizer(buildConfig());
        Method parseResponse = OpenAiRecognizer.class.getDeclaredMethod("parseResponse", String.class);
        parseResponse.setAccessible(true);

        RecognitionResult result = (RecognitionResult) parseResponse.invoke(recognizer, "{}");

        Assert.assertFalse(result.success);
        Assert.assertNotNull(result.errorMessage);
    }

    private AiApiConfig buildConfig() {
        AiApiConfig config = new AiApiConfig();
        config.setProvider(AiApiConfig.PROVIDER_OPENAI);
        config.setApiUrl("https://api.openai.com/v1");
        config.setApiKey("key");
        config.setModel("gpt-4o");
        return config;
    }
}

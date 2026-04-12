package com.coderpage.mine.app.tally.ai;

import org.junit.Assert;
import org.junit.Test;

public class AiApiConfigTest {

    @Test
    public void isValid_shouldReturnFalseWhenValueIsBlank() {
        AiApiConfig config = new AiApiConfig();
        config.setApiUrl("  ");
        config.setApiKey("key");
        config.setModel("model");

        Assert.assertFalse(config.isValid());
    }

    @Test
    public void isValid_shouldReturnTrueWhenAllRequiredFieldsPresent() {
        AiApiConfig config = new AiApiConfig();
        config.setApiUrl("https://api.openai.com/v1");
        config.setApiKey("key");
        config.setModel("gpt-4o");

        Assert.assertTrue(config.isValid());
    }

    @Test
    public void getTemplate_shouldFallbackToCustomTemplateForUnknownProvider() {
        AiApiConfig.ProviderTemplate template = AiApiConfig.getTemplate("unknown");
        Assert.assertEquals(AiApiConfig.PROVIDER_CUSTOM, template.provider);
    }
}

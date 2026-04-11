package com.coderpage.mine.app.tally.ai;

/**
 * AI 识别器工厂
 * 
 * @author Flandre Scarlet
 */
public class AiRecognizerFactory {

    /**
     * 创建识别器
     * 
     * @param config API 配置
     * @return 识别器实例
     */
    public static AiRecognizer create(AiApiConfig config) {
        return new OpenAiRecognizer(config);
    }

    /**
     * 创建测试用识别器（不验证配置）
     */
    public static AiRecognizer createTest(AiApiConfig config) {
        return new OpenAiRecognizer(config);
    }
}

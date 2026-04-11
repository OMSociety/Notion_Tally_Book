package com.coderpage.mine.app.tally.ai;

/**
 * AI 识别器接口
 * 
 * @author Flandre Scarlet
 */
public interface AiRecognizer {

    /**
     * 识别图片中的账单信息
     * 
     * @param base64Image Base64 编码的图片
     * @return 识别结果
     */
    RecognitionResult recognize(String base64Image);

    /**
     * 测试 API 连接
     * 
     * @return 识别结果（success 表示连接是否成功）
     */
    RecognitionResult testConnection();

    /**
     * 验证配置是否有效
     * 
     * @return 是否有效
     */
    boolean isValid();
}

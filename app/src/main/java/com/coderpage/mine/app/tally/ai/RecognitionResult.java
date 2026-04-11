package com.coderpage.mine.app.tally.ai;

/**
 * AI 识别结果
 * 
 * @author Flandre Scarlet
 */
public class RecognitionResult {
    
    public boolean success;
    public boolean isBill;      // 是否是账单
    public String type;         // "expense" 或 "income"
    public double amount;       // 金额
    public String rawResponse;   // 原始响应
    public String errorMessage;  // 错误信息
    
    public static RecognitionResult success(boolean isBill, String type, double amount) {
        RecognitionResult result = new RecognitionResult();
        result.success = true;
        result.isBill = isBill;
        result.type = type;
        result.amount = amount;
        return result;
    }
    
    public static RecognitionResult error(String message) {
        RecognitionResult result = new RecognitionResult();
        result.success = false;
        result.errorMessage = message;
        return result;
    }
}

package com.coderpage.mine.app.tally.module.auto;

public class AiIdentifyAmount {
    /**
     * 是否涉及到金额支付或者支持 true:是账单 false:不是账单
     */
    private Boolean bill;

    /**
     * 账单类型 expense:支出 income:收入 other:未知
     */
    private String expenseOrIncome;

    /**
     * 金额
     */
    private double money;


    public AiIdentifyAmount() {
    }

    public AiIdentifyAmount(boolean bill, String expenseOrIncome, double money) {
        this.bill = bill;
        this.expenseOrIncome = expenseOrIncome;
        this.money = money;
    }

    public Boolean getBill() {
        return bill;
    }

    public void setBill(Boolean bill) {
        this.bill = bill;
    }

    public String getExpenseOrIncome() {
        return expenseOrIncome;
    }

    public void setExpenseOrIncome(String expenseOrIncome) {
        this.expenseOrIncome = expenseOrIncome;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }
}

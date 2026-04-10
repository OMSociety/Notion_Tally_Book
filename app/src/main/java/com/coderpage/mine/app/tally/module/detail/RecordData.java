package com.coderpage.mine.app.tally.module.detail;

/**
 * @author lc. 2018-07-21 12:15
 * @since 0.6.0
 */
public class RecordData {

    private long recordId;
    private String amount;
    private String categoryIcon;
    private String categoryName;
    private String desc;
    private String time;
    private int type;

    public static final int TYPE_EXPENSE = 0;
    public static final int TYPE_INCOME = 1;

    public long getRecordId() {
        return recordId;
    }

    public void setRecordId(long recordId) {
        this.recordId = recordId;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}

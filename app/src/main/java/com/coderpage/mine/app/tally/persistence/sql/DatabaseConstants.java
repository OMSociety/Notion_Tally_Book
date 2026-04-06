package com.coderpage.mine.app.tally.persistence.sql;

/**
 * 数据库常量
 * 
 * 统一管理所有数据表名称和列名常量，
 * 避免在多处硬编码字符串，减少拼写错误风险。
 * 
 * @author abner-l
 * @since 0.7.5
 */
public class DatabaseConstants {

    private DatabaseConstants() {
        // 私有构造器，防止实例化
    }

    // ==================== 表名常量 ====================

    /** 记账记录表 */
    public static final String TABLE_RECORD = "record";

    /** 分类表 */
    public static final String TABLE_CATEGORY = "category";

    /** 同步历史表 */
    public static final String TABLE_SYNC_HISTORY = "sync_history";

    // ==================== record 表列名常量 ====================

    public static final String COLUMN_RECORD_ID = "record_id";
    public static final String COLUMN_RECORD_ACCOUNT_ID = "record_account_id";
    public static final String COLUMN_RECORD_TIME = "record_time";
    public static final String COLUMN_RECORD_AMOUNT = "record_amount";
    public static final String COLUMN_RECORD_CATEGORY_UNIQUE_NAME = "record_category_unique_name";
    public static final String COLUMN_RECORD_DESC = "record_desc";
    public static final String COLUMN_RECORD_SYNC_ID = "record_sync_id";
    public static final String COLUMN_RECORD_SYNC_STATUS = "record_sync_status";
    public static final String COLUMN_RECORD_DELETE = "record_delete";
    public static final String COLUMN_RECORD_TYPE = "record_type";

    // ==================== category 表列名常量 ====================

    public static final String COLUMN_CATEGORY_ID = "category_id";
    public static final String COLUMN_CATEGORY_TYPE = "category_type";
    public static final String COLUMN_CATEGORY_UNIQUE_NAME = "category_unique_name";
    public static final String COLUMN_CATEGORY_ICON = "category_icon";
    public static final String COLUMN_CATEGORY_NAME = "category_name";
    public static final String COLUMN_CATEGORY_ORDER = "category_order";
    public static final String COLUMN_CATEGORY_DELETE = "category_delete";

    // ==================== sync_history 表列名常量 ====================

    public static final String COLUMN_SYNC_ID = "sync_id";
    public static final String COLUMN_SYNC_TYPE = "sync_type";
    public static final String COLUMN_SYNC_STATUS = "sync_status";
    public static final String COLUMN_SYNC_TIME = "sync_time";
    public static final String COLUMN_SYNC_MESSAGE = "sync_message";
}

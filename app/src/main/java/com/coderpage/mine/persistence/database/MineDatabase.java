package com.coderpage.mine.persistence.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.coderpage.mine.MineApp;
import com.coderpage.mine.persistence.dao.KeyValueDao;
import com.coderpage.mine.persistence.entity.KeyValue;

/**
 * @author lc. 2019-03-30 08:49
 * @since 0.6.0
 */

@Database(entities = {KeyValue.class}, version = 60, exportSchema = false)
public abstract class MineDatabase extends RoomDatabase {
    /** sqlite db name */
    private static final String DATABASE_NAME = "sql_mine";

    /** db version of app version 0.6.0 */
    private static final int VERSION_0_6_0 = 60;

    private static volatile MineDatabase sInstance = null;

    /**
     * key-value 数据表操作
     *
     * @return key-value 数据表操作
     */
    public abstract KeyValueDao keyValueDao();

    public static MineDatabase getInstance() {
        if (sInstance == null) {
            synchronized (MineDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(
                            MineApp.getAppContext(),
                            MineDatabase.class, DATABASE_NAME)
                            .build();
                }
            }
        }
        return sInstance;
    }
}

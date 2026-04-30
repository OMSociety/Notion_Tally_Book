package com.coderpage.mine.app.tally.persistence.sql;

import androidx.room.TypeConverter;

import java.math.BigDecimal;

/**
 * Room TypeConverter for BigDecimal ↔ Double
 */
public class Converters {

    @TypeConverter
    public static BigDecimal fromDouble(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    @TypeConverter
    public static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}

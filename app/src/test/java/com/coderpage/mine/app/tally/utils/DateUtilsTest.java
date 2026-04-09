package com.coderpage.mine.app.tally.utils;

import android.util.Pair;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * DateUtils 工具类单元测试
 *
 * @author test
 * @since 0.8.0
 */
public class DateUtilsTest {

    @Test
    public void testDayMillisecondsConstant() {
        assertEquals(86400000, DateUtils.DAY_MILLISECONDS);
    }

    @Test
    public void testMonthDateRange2024January() {
        Pair<Long, Long> range = DateUtils.monthDateRange(2024, 1);
        assertNotNull(range);
        
        // 2024年1月1日 00:00:00
        long startExpected = 1704038400000L;
        // 2024年1月31日 23:59:59.999
        long endExpected = 1706745599999L;
        
        assertEquals(startExpected, range.first.longValue());
        assertEquals(endExpected, range.second.longValue());
    }

    @Test
    public void testMonthDateRange2024FebruaryLeapYear() {
        Pair<Long, Long> range = DateUtils.monthDateRange(2024, 2);
        assertNotNull(range);
        
        // 2024是闰年，2月有29天
        long startExpected = 1706649600000L;
        // 2024年2月29日 23:59:59.999
        long endExpected = 1709255999999L;
        
        assertEquals(startExpected, range.first.longValue());
        assertEquals(endExpected, range.second.longValue());
    }

    @Test
    public void testMonthDateRange2023FebruaryNonLeapYear() {
        Pair<Long, Long> range = DateUtils.monthDateRange(2023, 2);
        assertNotNull(range);
        
        // 2023不是闰年，2月有28天
        long startExpected = 1677600000000L;
        // 2023年2月28日 23:59:59.999
        long endExpected = 1680206399999L;
        
        assertEquals(startExpected, range.first.longValue());
        assertEquals(endExpected, range.second.longValue());
    }

    @Test
    public void testMonthDateRangeDecember() {
        Pair<Long, Long> range = DateUtils.monthDateRange(2024, 12);
        assertNotNull(range);
        
        // 12月有31天
        long startExpected = 1733011200000L;
        long endExpected = 1735603199999L;
        
        assertEquals(startExpected, range.first.longValue());
        assertEquals(endExpected, range.second.longValue());
    }

    @Test
    public void testDayDateRange() {
        Pair<Long, Long> range = DateUtils.dayDateRange(2024, 6, 15);
        assertNotNull(range);
        
        // 2024年6月15日 00:00:00.000
        long startExpected = 1718390400000L;
        // 2024年6月15日 23:59:59.999
        long endExpected = 1718476799999L;
        
        assertEquals(startExpected, range.first.longValue());
        assertEquals(endExpected, range.second.longValue());
    }

    @Test
    public void testDayDateRangeFirstDayOfMonth() {
        Pair<Long, Long> range = DateUtils.dayDateRange(2024, 1, 1);
        assertNotNull(range);
        
        assertEquals(1704067200000L, range.first.longValue());
        assertEquals(1704153599999L, range.second.longValue());
    }

    @Test
    public void testDayDateRangeLastDayOfMonth() {
        Pair<Long, Long> range = DateUtils.dayDateRange(2024, 1, 31);
        assertNotNull(range);
        
        assertEquals(1706649600000L, range.first.longValue());
        assertEquals(1706735999999L, range.second.longValue());
    }

    @Test
    public void testYearDateRange() {
        Pair<Long, Long> range = DateUtils.yearDateRange(2024);
        assertNotNull(range);
        
        // 2024年1月1日 00:00:00.000
        long startExpected = 1704067200000L;
        // 2024年12月31日 23:59:59.999
        long endExpected = 1735689599999L;
        
        assertEquals(startExpected, range.first.longValue());
        assertEquals(endExpected, range.second.longValue());
    }

    @Test
    public void testYearDateRangeLeapYear() {
        // 2024是闰年
        Pair<Long, Long> range = DateUtils.yearDateRange(2024);
        assertNotNull(range);
        
        // 闰年有366天
        long daysInYear = (range.second - range.first) / DateUtils.DAY_MILLISECONDS;
        assertEquals(366, daysInYear);
    }

    @Test
    public void testYearDateRangeNonLeapYear() {
        // 2023不是闰年
        Pair<Long, Long> range = DateUtils.yearDateRange(2023);
        assertNotNull(range);
        
        // 非闰年有365天
        long daysInYear = (range.second - range.first) / DateUtils.DAY_MILLISECONDS;
        assertEquals(365, daysInYear);
    }

    @Test
    public void testCurrentMonthStartUnixTime() {
        long startTime = DateUtils.currentMonthStartUnixTime();
        
        // 验证是当月1日
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(startTime);
        
        assertEquals(1, cal.get(java.util.Calendar.DAY_OF_MONTH));
        assertEquals(0, cal.get(java.util.Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(java.util.Calendar.MINUTE));
        assertEquals(0, cal.get(java.util.Calendar.SECOND));
        assertEquals(0, cal.get(java.util.Calendar.MILLISECOND));
    }

    @Test
    public void testTodayStartUnixTime() {
        long startTime = DateUtils.todayStartUnixTime();
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(startTime);
        
        assertEquals(0, cal.get(java.util.Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(java.util.Calendar.MINUTE));
        assertEquals(0, cal.get(java.util.Calendar.SECOND));
        assertEquals(0, cal.get(java.util.Calendar.MILLISECOND));
    }

    @Test
    public void testTodayEndUnixTime() {
        long endTime = DateUtils.todayEndUnixTime();
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(endTime);
        
        assertEquals(23, cal.get(java.util.Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(java.util.Calendar.MINUTE));
        assertEquals(59, cal.get(java.util.Calendar.SECOND));
        assertEquals(999, cal.get(java.util.Calendar.MILLISECOND));
    }

    @Test
    public void testMonthDateRangeIntegrity() {
        // 验证月份时间范围的一致性
        for (int month = 1; month <= 12; month++) {
            Pair<Long, Long> range = DateUtils.monthDateRange(2024, month);
            
            long days = (range.second - range.first) / DateUtils.DAY_MILLISECONDS + 1;
            int expectedDays = TimeUtils.getDaysTotalOfMonth(2024, month);
            
            assertEquals("Month " + month + " day count mismatch", 
                        expectedDays, (int) days);
        }
    }
}

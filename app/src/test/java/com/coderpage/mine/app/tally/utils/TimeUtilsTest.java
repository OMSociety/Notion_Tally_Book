package com.coderpage.mine.app.tally.utils;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

/**
 * TimeUtils 工具类单元测试
 *
 * @author test
 * @since 0.8.0
 */
public class TimeUtilsTest {

    @Test
    public void testDayMillisecondsConstant() {
        assertEquals(86400000, TimeUtils.DAY_MILLSECONDS);
    }

    @Test
    public void testGetDaysTotalOfMonthJanuary() {
        assertEquals(31, TimeUtils.getDaysTotalOfMonth(2024, 1));
        assertEquals(31, TimeUtils.getDaysTotalOfMonth(2023, 1));
    }

    @Test
    public void testGetDaysTotalOfMonthFebruary() {
        // 闰年 2月
        assertEquals(29, TimeUtils.getDaysTotalOfMonth(2024, 2));
        assertEquals(29, TimeUtils.getDaysTotalOfMonth(2000, 2));
        assertEquals(29, TimeUtils.getDaysTotalOfMonth(1996, 2));
        
        // 非闰年 2月
        assertEquals(28, TimeUtils.getDaysTotalOfMonth(2023, 2));
        assertEquals(28, TimeUtils.getDaysTotalOfMonth(1900, 2));
        assertEquals(28, TimeUtils.getDaysTotalOfMonth(2100, 2));
    }

    @Test
    public void testGetDaysTotalOfMonthLeapYearRule() {
        // 闰年规则: 能被4整除但不能被100整除，或者能被400整除
        // 2000是闰年(能被400整除)
        assertEquals(29, TimeUtils.getDaysTotalOfMonth(2000, 2));
        // 1900不是闰年(能被100整除但不能被400整除)
        assertEquals(28, TimeUtils.getDaysTotalOfMonth(1900, 2));
        // 2004是闰年(能被4整除且不能被100整除)
        assertEquals(29, TimeUtils.getDaysTotalOfMonth(2004, 2));
        // 2100不是闰年
        assertEquals(28, TimeUtils.getDaysTotalOfMonth(2100, 2));
    }

    @Test
    public void testGetDaysTotalOfMonth31DayMonths() {
        int[] months31 = {1, 3, 5, 7, 8, 10, 12};
        for (int month : months31) {
            assertEquals(31, TimeUtils.getDaysTotalOfMonth(2024, month));
        }
    }

    @Test
    public void testGetDaysTotalOfMonth30DayMonths() {
        int[] months30 = {4, 6, 9, 11};
        for (int month : months30) {
            assertEquals(30, TimeUtils.getDaysTotalOfMonth(2024, month));
        }
    }

    @Test
    public void testGetDaysTotalOfMonthDecember() {
        assertEquals(31, TimeUtils.getDaysTotalOfMonth(12, 12));
    }

    @Test
    public void testGetDaysTotalOfMonthAllMonths() {
        // 验证所有月份天数正确
        int[] expectedDays = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        for (int month = 1; month <= 12; month++) {
            assertEquals(expectedDays[month - 1], TimeUtils.getDaysTotalOfMonth(2024, month));
        }
    }

    @Test
    public void testGetDaysTotalOfMonthCenturyYears() {
        // 1900年不是闰年(世纪年但不能被400整除)
        assertEquals(28, TimeUtils.getDaysTotalOfMonth(1900, 2));
        // 2000年是闰年(能被400整除)
        assertEquals(29, TimeUtils.getDaysTotalOfMonth(2000, 2));
        // 2100年不是闰年
        assertEquals(28, TimeUtils.getDaysTotalOfMonth(2100, 2));
    }
}

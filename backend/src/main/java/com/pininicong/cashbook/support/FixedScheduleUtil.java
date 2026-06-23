package com.pininicong.cashbook.support;

import com.pininicong.cashbook.domain.CbFixedItem;
import com.pininicong.cashbook.domain.FixedHolidayAdjust;
import com.pininicong.cashbook.domain.FixedPeriodType;
import com.pininicong.cashbook.domain.FixedScheduleType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

public final class FixedScheduleUtil {

    private FixedScheduleUtil() {}

    public static boolean matchesDate(CbFixedItem item, LocalDate date) {
        if (item == null || date == null) {
            return false;
        }
        if (!isWithinPeriod(item, date)) {
            return false;
        }
        FixedScheduleType schedule = item.getScheduleType();
        if (schedule == FixedScheduleType.DAILY) {
            return true;
        }
        LocalDate occurrence = resolveOccurrenceDate(item, YearMonth.from(date));
        return occurrence != null && occurrence.equals(date);
    }

    public static LocalDate resolveOccurrenceDate(CbFixedItem item, YearMonth ym) {
        LocalDate base;
        if (Boolean.TRUE.equals(item.getLastDayOfMonth())) {
            base = ym.atEndOfMonth();
        } else {
            Integer day = item.getDayOfMonth();
            if (day == null || day < 1) {
                return null;
            }
            int clamped = Math.min(day, ym.lengthOfMonth());
            base = ym.atDay(clamped);
        }
        FixedHolidayAdjust adjust = item.getHolidayAdjust();
        if (adjust == null || adjust == FixedHolidayAdjust.NONE || !isHoliday(base)) {
            return base;
        }
        return adjustForHoliday(base, adjust);
    }

    public static String formatScheduleLabel(CbFixedItem item) {
        if (item.getScheduleType() == FixedScheduleType.DAILY) {
            return "매일";
        }
        if (Boolean.TRUE.equals(item.getLastDayOfMonth())) {
            return "말일";
        }
        Integer day = item.getDayOfMonth();
        return day != null ? String.valueOf(day) : "";
    }

    private static boolean isWithinPeriod(CbFixedItem item, LocalDate date) {
        if (item.getPeriodType() != FixedPeriodType.RANGE) {
            return true;
        }
        if (item.getPeriodStart() != null && date.isBefore(item.getPeriodStart())) {
            return false;
        }
        if (item.getPeriodEnd() != null && date.isAfter(item.getPeriodEnd())) {
            return false;
        }
        return true;
    }

    private static boolean isHoliday(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    private static LocalDate adjustForHoliday(LocalDate date, FixedHolidayAdjust adjust) {
        LocalDate cursor = date;
        if (adjust == FixedHolidayAdjust.PREVIOUS) {
            while (isHoliday(cursor)) {
                cursor = cursor.minusDays(1);
            }
            return cursor;
        }
        while (isHoliday(cursor)) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }
}

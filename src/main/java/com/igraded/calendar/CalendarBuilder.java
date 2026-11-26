package com.igraded.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the month grid. Plain Java with no framework dependencies, so it is trivial to unit test.
 */
public class CalendarBuilder {

    private static final int DAYS_IN_WEEK = 7;

    public CalendarMonth build(YearMonth month, LocalDate today) {
        LocalDate first = month.atDay(1);

        // Weeks start on Sunday. DayOfWeek.getValue() is Mon=1 ... Sun=7, so "% 7" gives Sun=0 ... Sat=6.
        int leadingDays = first.getDayOfWeek().getValue() % DAYS_IN_WEEK;
        LocalDate gridStart = first.minusDays(leadingDays);

        int weekCount = (int) Math.ceil((leadingDays + month.lengthOfMonth()) / (double) DAYS_IN_WEEK);

        List<List<CalendarDay>> weeks = new ArrayList<>(weekCount);
        for (int w = 0; w < weekCount; w++) {
            List<CalendarDay> week = new ArrayList<>(DAYS_IN_WEEK);
            for (int d = 0; d < DAYS_IN_WEEK; d++) {
                LocalDate date = gridStart.plusDays((long) w * DAYS_IN_WEEK + d);
                DayOfWeek dow = date.getDayOfWeek();
                week.add(new CalendarDay(
                        date,
                        YearMonth.from(date).equals(month),
                        date.equals(today),
                        dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY));
            }
            weeks.add(List.copyOf(week));
        }

        return new CalendarMonth(month, List.copyOf(weeks), month.minusMonths(1), month.plusMonths(1),
                month.equals(YearMonth.from(today)));
    }
}

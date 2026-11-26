package com.igraded.calendar;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * A full month, ready to render: complete weeks (Sunday to Saturday) so the grid is always rectangular.
 *
 * @param month    the month being shown
 * @param weeks    4 to 6 rows of 7 days each
 * @param previous the month before, for the "back" button
 * @param next     the month after, for the "forward" button
 * @param current  true when this is the month that contains today
 */
public record CalendarMonth(YearMonth month, List<List<CalendarDay>> weeks,
                            YearMonth previous, YearMonth next, boolean current) {

    public String title() {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
    }

    public static List<String> weekdayLabels() {
        return List.of("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat");
    }
}

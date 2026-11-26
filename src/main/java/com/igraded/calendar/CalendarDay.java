package com.igraded.calendar;

import java.time.LocalDate;

/**
 * One cell in the month grid.
 *
 * @param date       the actual date this cell represents
 * @param inMonth    false for the leading/trailing days that belong to the neighbouring months
 * @param today      true for exactly one cell: the current date (this one gets circled)
 * @param weekend    Saturday or Sunday
 */
public record CalendarDay(LocalDate date, boolean inMonth, boolean today, boolean weekend) {

    public int dayOfMonth() {
        return date.getDayOfMonth();
    }

    /** Full readable date, used for screen readers and tooltips. */
    public String label() {
        return date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                + ", " + date.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                + " " + date.getDayOfMonth() + ", " + date.getYear();
    }
}

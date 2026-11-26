package com.igraded.news;

import java.util.Locale;

/** The kinds of announcement. Each gets its own colour on the home page banner. */
public enum AnnouncementKind {
    NEWS("News"),
    EVENT("Event"),
    MEETING("Meeting"),
    URGENT("Urgent");

    private final String label;

    AnnouncementKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Used in a CSS class name: "slide--meeting". */
    public String css() {
        return name().toLowerCase(Locale.ROOT);
    }
}

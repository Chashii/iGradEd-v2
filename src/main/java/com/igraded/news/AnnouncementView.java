package com.igraded.news;

/** One slide of the home page banner, already formatted. type is the CSS name: news, event, meeting or urgent. */
public record AnnouncementView(String type, String label, String title, String body, String date) {
}

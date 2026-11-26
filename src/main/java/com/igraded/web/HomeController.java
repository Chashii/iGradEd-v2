package com.igraded.web;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.igraded.calendar.CalendarBuilder;
import com.igraded.calendar.CalendarMonth;
import com.igraded.news.AnnouncementService;
import com.igraded.security.AppUserDetails;
import com.igraded.todo.TodoService;
import com.igraded.todo.TodoView;

@Controller
public class HomeController {

    private static final Map<String, String> SECTION_TITLES = Map.of(
            "grades", "Grades",
            "attendance", "Attendance",
            "files", "Files");

    private final CalendarBuilder calendarBuilder = new CalendarBuilder();
    private final AnnouncementService announcements;
    private final TodoService todos;
    private final ZoneId zone;

    public HomeController(AnnouncementService announcements,
                          TodoService todos,
                          @Value("${igraded.timezone:Asia/Manila}") String timezone) {
        this.announcements = announcements;
        this.todos = todos;
        this.zone = ZoneId.of(timezone);
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal AppUserDetails me,
                        @RequestParam(required = false) Integer year,
                        @RequestParam(required = false) Integer month,
                        Model model) {
        LocalDate today = LocalDate.now(zone);
        YearMonth shown = parseMonth(year, month, YearMonth.from(today));

        model.addAttribute("active", "home");
        model.addAttribute("calendar", calendarBuilder.build(shown, today));
        model.addAttribute("weekdays", CalendarMonth.weekdayLabels());
        model.addAttribute("todayText",
                today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)));
        // What this person sees depends on who they are: their department's announcements and their own tasks
        model.addAttribute("announcements", announcements.forHome(me.getId()));

        List<TodoView> myTodos = todos.forUser(me.getId());
        long open = myTodos.stream().filter(t -> !t.done()).count();
        model.addAttribute("todos", myTodos);
        model.addAttribute("todoSummary", myTodos.isEmpty() ? "No tasks yet"
                : open == 0 ? "All caught up"
                : open + (open == 1 ? " task to do" : " tasks to do"));
        return "index";
    }

    /** Placeholder pages so the sidebar links work until these features are built. */
    @GetMapping("/{section:grades|attendance|files}")
    public String section(@PathVariable String section, Model model) {
        model.addAttribute("active", section);
        model.addAttribute("sectionTitle", SECTION_TITLES.get(section));
        return "section";
    }

    private static YearMonth parseMonth(Integer year, Integer month, YearMonth fallback) {
        if (year == null || month == null) {
            return fallback;
        }
        try {
            if (year < 1900 || year > 2200) {
                return fallback;
            }
            return YearMonth.of(year, month);
        } catch (DateTimeException e) {
            return fallback;
        }
    }
}

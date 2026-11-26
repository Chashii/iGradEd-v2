package com.igraded.news;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.igraded.department.Department;
import com.igraded.department.DepartmentRepository;
import com.igraded.security.Permissions;
import com.igraded.user.AppUser;
import com.igraded.user.UserRepository;
import com.igraded.user.UserService;

/**
 * Announcements: what each person sees on the home page, and what admins may post, edit and remove.
 * Powers are always checked against the person's current role and department in the database.
 */
@Service
public class AnnouncementService {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);
    private static final int MAX_SLIDES = 8;

    /** One line of the management table. */
    public record Row(Long id, String kind, String kindCss, String title, String audience,
                      String dates, String status) {
    }

    /** One choice in the "Who sees it" list. value is "school" or a department id. */
    public record Audience(String value, String label) {
    }

    private final AnnouncementRepository announcements;
    private final UserRepository users;
    private final UserService userService;
    private final DepartmentRepository departments;
    private final ZoneId zone;

    public AnnouncementService(AnnouncementRepository announcements,
                               UserRepository users,
                               UserService userService,
                               DepartmentRepository departments,
                               @Value("${igraded.timezone:Asia/Manila}") String timezone) {
        this.announcements = announcements;
        this.users = users;
        this.userService = userService;
        this.departments = departments;
        this.zone = ZoneId.of(timezone);
    }

    /** The slides on this person's home page. Urgent ones first, then newest. */
    public List<AnnouncementView> forHome(Long userId) {
        Long departmentId = users.findById(userId).map(AppUser::getDepartmentId).orElse(null);
        LocalDate today = LocalDate.now(zone);

        return announcements.visibleTo(today, departmentId).stream()
                .sorted(Comparator
                        .comparing((Announcement a) -> a.getKind() != AnnouncementKind.URGENT)
                        .thenComparing(Announcement::getPublishedOn, Comparator.reverseOrder())
                        .thenComparing(Announcement::getId, Comparator.reverseOrder()))
                .limit(MAX_SLIDES)
                .map(this::toView)
                .toList();
    }

    /** Announcements this admin is allowed to edit or delete. */
    public List<Row> manageList(Long actorId) {
        AppUser actor = userService.requireStaff(actorId);
        Map<Long, String> names = departments.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
        LocalDate today = LocalDate.now(zone);

        List<Row> rows = new ArrayList<>();
        for (Announcement a : announcements.findAllByOrderByPublishedOnDescIdDesc()) {
            if (!canManage(actor, a)) {
                continue;
            }
            rows.add(new Row(a.getId(), a.getKind().getLabel(), a.getKind().css(), a.getTitle(),
                    a.getDepartmentId() == null ? "Whole school" : names.getOrDefault(a.getDepartmentId(), "A department"),
                    datesText(a), statusOf(a, today)));
        }
        return rows;
    }

    /** The "Who sees it" choices for this admin. */
    public List<Audience> audiences(Long actorId) {
        AppUser actor = userService.requireStaff(actorId);
        List<Audience> out = new ArrayList<>();
        out.add(new Audience("school", "Whole school"));
        for (Department d : departments.findAllByOrderByNameAsc()) {
            if (Permissions.canPostTo(actor.getRole(), actor.getDepartmentId(), d.getId())) {
                out.add(new Audience(String.valueOf(d.getId()), d.getName() + " department only"));
            }
        }
        return out;
    }

    /** Fills the form with an existing announcement for editing. */
    public AnnouncementForm formFor(Long actorId, Long id) {
        AppUser actor = userService.requireStaff(actorId);
        Announcement a = announcements.findById(id).orElseThrow(NoSuchElementException::new);
        if (!canManage(actor, a)) {
            throw new AccessDeniedException("Not your announcement");
        }
        AnnouncementForm form = new AnnouncementForm();
        form.setKind(a.getKind());
        form.setTitle(a.getTitle());
        form.setBody(a.getBody());
        form.setEventOn(a.getEventOn());
        form.setPublishedOn(a.getPublishedOn());
        form.setExpiresOn(a.getExpiresOn());
        form.setAudience(a.getDepartmentId() == null ? "school" : String.valueOf(a.getDepartmentId()));
        return form;
    }

    /**
     * Creates an announcement (id null) or changes one.
     *
     * @throws IllegalArgumentException with a message to show, if the form is not acceptable
     * @throws AccessDeniedException    if this admin may not do this
     */
    public void save(Long actorId, Long id, AnnouncementForm form) {
        AppUser actor = userService.requireStaff(actorId);

        Long targetDepartment = parseAudience(form.getAudience());
        if (!Permissions.canPostTo(actor.getRole(), actor.getDepartmentId(), targetDepartment)) {
            throw new AccessDeniedException("Not allowed to post to that audience");
        }
        if (targetDepartment != null && !departments.existsById(targetDepartment)) {
            throw new IllegalArgumentException("That department no longer exists.");
        }

        LocalDate publishedOn = form.getPublishedOn() == null ? LocalDate.now(zone) : form.getPublishedOn();
        if (form.getExpiresOn() != null && form.getExpiresOn().isBefore(publishedOn)) {
            throw new IllegalArgumentException("The last day to show it can't be before the first day.");
        }

        String title = form.getTitle().trim();
        String body = form.getBody().trim();

        if (id == null) {
            announcements.save(new Announcement(form.getKind(), title, body, publishedOn,
                    form.getExpiresOn(), form.getEventOn(), targetDepartment, actor.getId()));
            return;
        }

        Announcement existing = announcements.findById(id).orElseThrow(NoSuchElementException::new);
        if (!canManage(actor, existing)) {
            throw new AccessDeniedException("Not your announcement");
        }
        existing.update(form.getKind(), title, body, publishedOn, form.getExpiresOn(),
                form.getEventOn(), targetDepartment);
        announcements.save(existing);
    }

    public void delete(Long actorId, Long id) {
        AppUser actor = userService.requireStaff(actorId);
        Announcement a = announcements.findById(id).orElseThrow(NoSuchElementException::new);
        if (!canManage(actor, a)) {
            throw new AccessDeniedException("Not your announcement");
        }
        announcements.delete(a);
    }

    private boolean canManage(AppUser actor, Announcement a) {
        return Permissions.canManageAnnouncement(actor.getRole(), actor.getId(), actor.getDepartmentId(),
                a.getDepartmentId(), a.getCreatedBy());
    }

    /** "school" or blank = whole school (null). Otherwise a department id. */
    private static Long parseAudience(String audience) {
        if (audience == null || audience.isBlank() || audience.equals("school")) {
            return null;
        }
        try {
            return Long.parseLong(audience.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Choose who should see it.");
        }
    }

    private AnnouncementView toView(Announcement a) {
        String date = a.getEventOn() != null
                ? "On " + a.getEventOn().format(LONG)
                : a.getPublishedOn().format(SHORT);
        return new AnnouncementView(a.getKind().css(), a.getKind().getLabel(), a.getTitle(), a.getBody(), date);
    }

    private static String datesText(Announcement a) {
        StringBuilder sb = new StringBuilder("Shown from ").append(a.getPublishedOn().format(SHORT));
        if (a.getExpiresOn() != null) {
            sb.append(" to ").append(a.getExpiresOn().format(SHORT));
        }
        if (a.getEventOn() != null) {
            sb.append(" · Happens ").append(a.getEventOn().format(SHORT));
        }
        return sb.toString();
    }

    private static String statusOf(Announcement a, LocalDate today) {
        if (today.isBefore(a.getPublishedOn())) {
            return "Scheduled";
        }
        if (a.getExpiresOn() != null && today.isAfter(a.getExpiresOn())) {
            return "Expired";
        }
        return "Live";
    }
}

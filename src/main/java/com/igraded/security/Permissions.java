package com.igraded.security;

import com.igraded.user.Role;

/**
 * Who may do what. All the role rules live here, in one place, as plain functions with no framework code,
 * so they can be read and tested on their own. The services call these before every action.
 *
 * TEACHER     - basic use. Never appears in any rule below that grants a power.
 * ADMIN       - a department head. Gives tasks to teachers in their own department, and posts announcements
 *               for the whole school or for their own department.
 * SUPER_ADMIN - the developers. Can do everything.
 *
 * "Department id" arguments may be null: a person without a department, or an announcement for the whole school.
 */
public final class Permissions {

    private Permissions() {
    }

    public static boolean isStaff(Role role) {
        return role == Role.ADMIN || role == Role.SUPER_ADMIN;
    }

    /** Post an announcement to a department (id) or to the whole school (null). */
    public static boolean canPostTo(Role role, Long actorDepartmentId, Long targetDepartmentId) {
        if (role == Role.SUPER_ADMIN) {
            return true;
        }
        if (role == Role.ADMIN) {
            return targetDepartmentId == null
                    || (actorDepartmentId != null && actorDepartmentId.equals(targetDepartmentId));
        }
        return false;
    }

    /** Edit or delete an existing announcement. */
    public static boolean canManageAnnouncement(Role role, Long actorId, Long actorDepartmentId,
                                                Long announcementDepartmentId, Long announcementCreatedBy) {
        if (role == Role.SUPER_ADMIN) {
            return true;
        }
        if (role == Role.ADMIN) {
            boolean wroteIt = announcementCreatedBy != null && announcementCreatedBy.equals(actorId);
            boolean itsForMyDepartment = announcementDepartmentId != null
                    && announcementDepartmentId.equals(actorDepartmentId);
            return wroteIt || itsForMyDepartment;
        }
        return false;
    }

    /** Whether a person (with this department) sees an announcement on their home page. */
    public static boolean canSeeAnnouncement(Long userDepartmentId, Long announcementDepartmentId) {
        return announcementDepartmentId == null || announcementDepartmentId.equals(userDepartmentId);
    }

    /** "All teachers in the school" as the audience of a task. */
    public static boolean canAssignToAll(Role role) {
        return role == Role.SUPER_ADMIN;
    }

    /** Give a task to the teachers of a department. Also used for one teacher, with that teacher's department. */
    public static boolean canAssignToDepartment(Role role, Long actorDepartmentId, Long targetDepartmentId) {
        if (role == Role.SUPER_ADMIN) {
            return true;
        }
        return role == Role.ADMIN && actorDepartmentId != null && actorDepartmentId.equals(targetDepartmentId);
    }

    /** Take back a task that was given to a teacher. */
    public static boolean canUnassign(Role role, Long actorId, Long assignedBy) {
        if (role == Role.SUPER_ADMIN) {
            return true;
        }
        return role == Role.ADMIN && assignedBy != null && assignedBy.equals(actorId);
    }
}

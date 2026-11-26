package com.igraded.user;

import java.time.LocalDate;

/** One line of the Users page, with the department name already looked up. */
public record UserRow(Long id, String fullName, String email, Role role,
                      Long departmentId, String departmentName, LocalDate createdOn) {
}

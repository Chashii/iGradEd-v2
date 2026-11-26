package com.igraded.user;

/**
 * The three kinds of account.
 *
 * TEACHER     - the regular user (called "customer" in the first requirements). Anyone can sign up as one.
 * ADMIN       - can see the list of users.
 * SUPER_ADMIN - everything an admin can do, plus changing other users' roles.
 *
 * Spring Security turns each of these into the authority "ROLE_" + name.
 */
public enum Role {
    TEACHER("Teacher"),
    ADMIN("Admin"),
    SUPER_ADMIN("Super Admin");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

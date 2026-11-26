package com.igraded.security;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.igraded.user.AppUser;
import com.igraded.user.Role;

/**
 * The logged-in user as Spring Security sees it. It copies plain values out of the database entity so it
 * is safe to keep in the session. Controllers can receive it with @AuthenticationPrincipal.
 */
public class AppUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String fullName;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;
    private final LocalDate createdOn;

    public AppUserDetails(AppUser user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
        this.createdOn = user.getCreatedOn();
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public LocalDate getCreatedOn() { return createdOn; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    /** The email is the login name. */
    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isEnabled() { return enabled; }
}

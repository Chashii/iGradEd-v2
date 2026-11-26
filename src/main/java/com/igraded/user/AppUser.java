package com.igraded.user;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One account. The table is called "users" because "user" is a reserved word in some databases. */
@Entity
@Table(name = "users")
public class AppUser {

    /** 2 GB. Matches the default in the users table. */
    public static final long DEFAULT_QUOTA_BYTES = 2L * 1024 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String fullName;

    /** Always stored trimmed and lower-case. */
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** BCrypt hash. The real password is never stored. */
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private LocalDate createdOn = LocalDate.now();

    /** How much Locker space this account may use. */
    @Column(nullable = false)
    private long storageQuotaBytes = DEFAULT_QUOTA_BYTES;

    /** The department this person belongs to, or null. A department head is an ADMIN in a department. */
    @Column
    private Long departmentId;

    protected AppUser() {
        // required by JPA
    }

    public AppUser(String fullName, String email, String passwordHash, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public LocalDate getCreatedOn() { return createdOn; }
    public long getStorageQuotaBytes() { return storageQuotaBytes; }

    public Long getDepartmentId() { return departmentId; }

    public void setRole(Role role) { this.role = role; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}

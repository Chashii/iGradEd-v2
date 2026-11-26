package com.igraded.user;

import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first Super Admin when the app starts and none exists yet. Nobody can sign up as an
 * admin, so without this there would be no way to ever get one.
 *
 * The password comes from the IGRADED_SUPERADMIN_PASSWORD environment variable. If it is not set, a random
 * one is generated and printed once in the console, so no password ever has to be written in the code.
 */
@Component
public class SuperAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);
    private static final String ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UserRepository users;
    private final UserService userService;
    private final String email;
    private final String configuredPassword;

    public SuperAdminSeeder(UserRepository users,
                            UserService userService,
                            @Value("${igraded.superadmin.email}") String email,
                            @Value("${igraded.superadmin.password:}") String configuredPassword) {
        this.users = users;
        this.userService = userService;
        this.email = email;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(String... args) {
        if (users.existsByRole(Role.SUPER_ADMIN)) {
            return;
        }
        if (userService.emailTaken(email)) {
            // Never promote an existing account automatically
            log.warn("No Super Admin exists, but {} is already registered as another account. "
                    + "Set IGRADED_SUPERADMIN_EMAIL to a different address and restart.", email);
            return;
        }

        boolean generated = configuredPassword == null || configuredPassword.isBlank();
        String password = generated ? randomPassword() : configuredPassword;
        userService.create("Super Admin", email, password, Role.SUPER_ADMIN);

        if (generated) {
            log.warn("""

                    ============================================================
                     First Super Admin created. Save this now, it is shown once.
                       Email:    {}
                       Password: {}
                    ============================================================
                    """, email, password);
        } else {
            log.info("First Super Admin created: {}", email);
        }
    }

    private static String randomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}

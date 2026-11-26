package com.igraded.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.igraded.user.UserRepository;
import com.igraded.user.UserService;

/** Tells Spring Security how to find an account by the email typed on the login form. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByEmail(UserService.normalizeEmail(username))
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown account"));
    }
}

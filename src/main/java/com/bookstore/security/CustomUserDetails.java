package com.bookstore.security;

import com.bookstore.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter between our {@link User} entity and Spring Security's
 * {@link UserDetails} contract.
 *
 * Spring Security never touches the User entity directly — it always goes
 * through this wrapper. Keeping them separate means we can change either
 * side without breaking the other.
 *
 * The authority is "ROLE_" + role.name() (e.g. "ROLE_ADMIN"), which is
 * what Spring Security expects for @PreAuthorize("hasRole('ADMIN')") and
 * hasRole() in SecurityConfig to work correctly.
 */
public class CustomUserDetails implements UserDetails {

    @Getter
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Getter
    private final Long id = null; // resolved lazily via getUser().getId()

    public Long getUserId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /** Spring Security uses "username" as the primary key; we use email. */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()            { return true; }
}

package com.bookstore.repository;

import com.bookstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Used by both {@code AuthServiceImpl} (login: load the user by the
     * email they submitted, then verify their password against the hash)
     * and {@code CustomUserDetailsService} (Spring Security loads the
     * principal by email/username on every authenticated request).
     */
    Optional<User> findByEmail(String email);

    /**
     * Used at registration time to return a clean 409 Conflict instead of
     * letting the request fall through to the unique-constraint violation
     * on insert. Both layers matter: this is the fast, friendly check; the
     * DB constraint (see {@code User} entity) is the actual safety net
     * against a race between two near-simultaneous registrations for the
     * same email.
     */
    boolean existsByEmail(String email);
}

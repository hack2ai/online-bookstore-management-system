package com.bookstore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A registered account — either a {@link Role#CUSTOMER} or {@link Role#ADMIN}.
 *
 * <p>There is deliberately no separate "Admin" or "Customer" entity/table:
 * the spec's single Users table (with a {@code role} column) is the right call here,
 * since both roles share every other field and splitting them would just mean
 * joining back together everywhere. {@link Role} distinguishes them, and
 * Spring Security authorities are derived from it in
 * {@code security.CustomUserDetailsService} rather than stored redundantly.
 *
 * <p>{@code email} carries a unique constraint at the database level
 * (not just {@code @Email} validation) so that two concurrent registration
 * requests for the same address cannot both succeed — see
 * {@code GlobalExceptionHandler} for how the resulting
 * {@code DataIntegrityViolationException} is translated into a clean 409 response.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password") // never let the hashed password leak into logs via toString()
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Email
    @Column(nullable = false, length = 150)
    private String email;

    /**
     * BCrypt hash, never the raw password. Populated exclusively through
     * {@code AuthServiceImpl} (registration) so it always passes through
     * {@code PasswordEncoder.encode(...)} first.
     */
    @NotBlank
    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.CUSTOMER;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

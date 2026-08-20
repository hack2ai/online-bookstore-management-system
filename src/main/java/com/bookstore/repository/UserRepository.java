package com.bookstore.repository;

import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);

    @Query("""
            select u from User u
            where u.role = :role
              and (:keyword is null
                   or lower(u.name) like lower(concat('%', :keyword, '%'))
                   or lower(u.email) like lower(concat('%', :keyword, '%')))
            """)
    Page<User> searchByRole(@Param("role") Role role, @Param("keyword") String keyword, Pageable pageable);
}

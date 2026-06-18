package com.bookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Online Bookstore Management System.
 *
 * <p>This Spring Boot application exposes:
 * <ul>
 *   <li>A JSON REST API under {@code /api/**} secured with JWT bearer tokens, and</li>
 *   <li>A server-rendered Thymeleaf UI under {@code /} secured with a traditional
 *       session/cookie login, for customers and admins respectively.</li>
 * </ul>
 *
 * See {@code SecurityConfig} for how both authentication schemes coexist on
 * separate filter chains.
 */
@SpringBootApplication
public class BookStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookStoreApplication.class, args);
    }
}

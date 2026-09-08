package com.bookstore.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthorizationAnnotationTest {

    private static final String ADMIN_EXPRESSION = "hasRole('ADMIN')";

    @Test
    void adminControllersRequireAdminRole() {
        List<Class<?>> adminControllers = List.of(
                AdminAnalyticsController.class,
                AdminAnalyticsPageController.class,
                AdminBookPageController.class,
                AdminCategoryPageController.class,
                AdminCustomerController.class,
                AdminDashboardController.class,
                AdminInventoryController.class,
                AdminOrderPageController.class
        );

        adminControllers.forEach(controller -> {
            PreAuthorize annotation = controller.getAnnotation(PreAuthorize.class);
            assertThat(annotation)
                    .as("%s must declare an admin-only authorization rule", controller.getSimpleName())
                    .isNotNull();
            assertThat(annotation.value())
                    .as("%s authorization expression", controller.getSimpleName())
                    .isEqualTo(ADMIN_EXPRESSION);
        });
    }
}

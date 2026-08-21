package com.bookstore.service;

import com.bookstore.entity.User;

public interface CustomerAccountService {
    User getUser(Long userId);
    void updateProfile(Long userId, String name, String phone);
    void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword);
}

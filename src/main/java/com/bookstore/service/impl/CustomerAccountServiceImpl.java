package com.bookstore.service.impl;

import com.bookstore.entity.User;
import com.bookstore.exception.BadRequestException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.CustomerAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerAccountServiceImpl implements CustomerAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, String name, String phone) {
        User user = getUser(userId);
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.length() < 2 || normalizedName.length() > 100) {
            throw new BadRequestException("Name must be between 2 and 100 characters.");
        }
        String normalizedPhone = phone == null ? null : phone.trim();
        if (normalizedPhone != null && normalizedPhone.isBlank()) normalizedPhone = null;
        if (normalizedPhone != null && normalizedPhone.length() > 20) {
            throw new BadRequestException("Phone number must not exceed 20 characters.");
        }
        user.setName(normalizedName);
        user.setPhone(normalizedPhone);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        User user = getUser(userId);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 72) {
            throw new BadRequestException("New password must be between 8 and 72 characters.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("New password and confirmation do not match.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("New password must be different from your current password.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}

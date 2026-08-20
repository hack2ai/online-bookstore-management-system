package com.bookstore.service;

import com.bookstore.dto.request.LoginRequest;
import com.bookstore.dto.request.RefreshTokenRequest;
import com.bookstore.dto.request.RegisterRequest;
import com.bookstore.dto.response.AuthResponse;
import com.bookstore.entity.RefreshToken;
import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import com.bookstore.exception.BadRequestException;
import com.bookstore.exception.DuplicateResourceException;
import com.bookstore.repository.RefreshTokenRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.security.JwtUtil;
import com.bookstore.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuthenticationManager authenticationManager;
    @Mock Authentication authentication;

    private AuthServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepository, refreshTokenRepository, passwordEncoder, jwtUtil, authenticationManager);
        user = User.builder().id(7L).name("Groot").email("groot@example.com").password("hash").role(Role.CUSTOMER).build();
    }

    @Test
    void registerNormalizesEmailAndAlwaysCreatesCustomer() {
        RegisterRequest request = RegisterRequest.builder()
                .name(" Groot ").email(" Groot@Example.COM ").password("Secret123!").build();
        when(userRepository.existsByEmail("groot@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn("access");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = service.register(request);

        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(response.getEmail()).isEqualTo("groot@example.com");
        verify(passwordEncoder).encode("Secret123!");
        verify(userRepository).existsByEmail("groot@example.com");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Groot").email("groot@example.com").password("Secret123!").build();
        when(userRepository.existsByEmail("groot@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void refreshRevokesOldTokenAndIssuesReplacement() {
        RefreshToken stored = RefreshToken.builder()
                .id(4L).user(user).tokenHash("hash").expiresAt(LocalDateTime.now().plusHours(1)).build();
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString())).thenReturn(Optional.of(stored));
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn("access");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = service.refresh(new RefreshTokenRequest("raw-refresh"));

        assertThat(stored.isRevoked()).isTrue();
        assertThat(response.getAccessToken()).isEqualTo("access");
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void expiredRefreshTokenIsRejectedAndRevoked() {
        RefreshToken stored = RefreshToken.builder()
                .id(4L).user(user).tokenHash("hash").expiresAt(LocalDateTime.now().minusMinutes(1)).build();
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest("raw-refresh")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void logoutRevokesActiveRefreshToken() {
        RefreshToken stored = RefreshToken.builder()
                .id(4L).user(user).tokenHash("hash").expiresAt(LocalDateTime.now().plusHours(1)).build();
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString())).thenReturn(Optional.of(stored));

        service.logout(new RefreshTokenRequest("raw-refresh"));

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).findByTokenHashAndRevokedAtIsNull(anyString());
    }

    @Test
    void loginUsesNormalizedEmailAndReturnsAuthenticatedUser() {
        LoginRequest request = LoginRequest.builder().email(" Groot@Example.COM ").password("Secret123!").build();
        CustomUserDetails details = new CustomUserDetails(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(details);
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn("access");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = service.login(request);

        assertThat(response.getEmail()).isEqualTo("groot@example.com");
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);
        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("groot@example.com");
    }
}

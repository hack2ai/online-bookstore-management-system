package com.bookstore.dto.response;

import com.bookstore.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Opaque refresh token. It can be exchanged for a new access token and is
     * stored only as a SHA-256 hash on the server.
     */
    private String refreshToken;

    private Long userId;
    private String name;
    private String email;
    private Role role;
}

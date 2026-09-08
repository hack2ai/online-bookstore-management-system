package com.bookstore.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT utility using JJWT 0.12.x.
 *
 * Tokens contain:
 *   - sub  : the user's email (the Spring Security username)
 *   - role : the user's role string (CUSTOMER / ADMIN)
 *   - userId : the authenticated user's database id
 *   - iat  : issued-at timestamp
 *   - exp  : expiry timestamp
 *   - iss  : application issuer
 *
 * JWT_SECRET should be a strong Base64-encoded secret in shared/staging/
 * production environments. The development fallback remains supported for
 * local startup only.
 */
@Component
@Slf4j
public class JwtUtil {

    private final SecretKey signingKey;
    private final long jwtExpirationMs;
    private final String issuer;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs,
            @Value("${app.jwt.issuer}") String issuer) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must not be empty.");
        }
        if (jwtExpirationMs <= 0) {
            throw new IllegalStateException("JWT expiration must be greater than zero.");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("JWT issuer must not be empty.");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            // Local development fallback: accept a plain-text secret.
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        try {
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT secret is too short. Use a strong secret of at least 256 bits.", e);
        }

        this.jwtExpirationMs = jwtExpirationMs;
        this.issuer = issuer;
    }

    // -------------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------------

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails instanceof CustomUserDetails cud) {
            extraClaims.put("role", cud.getUser().getRole().name());
            extraClaims.put("userId", cud.getUser().getId());
        }
        return buildToken(extraClaims, userDetails);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    // -------------------------------------------------------------------------
    // Token validation
    // -------------------------------------------------------------------------

    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            return false;
        }
        try {
            final String email = extractUsername(token);
            return userDetails.getUsername().equals(email) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates token signature, issuer, structure and expiry-related claims.
     * Returns false so the security filter can continue and Spring Security can
     * decide whether the request should receive a 401/403 response.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired");
        } catch (SignatureException e) {
            log.warn("JWT signature invalid — possible token tampering");
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token");
        } catch (JwtException e) {
            log.warn("JWT validation failed");
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is empty or invalid");
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Claims extraction
    // -------------------------------------------------------------------------

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

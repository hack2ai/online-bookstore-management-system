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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT utility using JJWT 0.12.x (the API changed significantly from 0.11.x —
 * notably: parserBuilder() → parser(), and the key is now a SecretKey, not
 * a raw byte[]).
 *
 * Tokens contain:
 *   - sub  : the user's email (the Spring Security "username")
 *   - role : the user's role string (CUSTOMER / ADMIN), so clients don't
 *            need a separate /me call just to know what UI to show
 *   - iat  : issued-at
 *   - exp  : expiry (default 24 h, overridable via JWT_EXPIRATION_MS env var)
 *
 * The secret is expected as a Base64-encoded string so it survives env-var
 * round-trips. The dev default in application.properties is a plain ASCII
 * string — JJWT will accept it, but it will log a warning about key length.
 * In production, set JWT_SECRET to a proper 256-bit Base64 secret.
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

        // Support both plain strings and Base64-encoded secrets.
        // In production, JWT_SECRET should be at least 32 random bytes,
        // Base64-encoded. The dev default is a plain string — fine for
        // local dev, insecure for anywhere real.
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            // Not valid Base64 → treat as raw UTF-8 bytes (dev default case)
            keyBytes = secret.getBytes();
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
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
                .subject(userDetails.getUsername())   // email
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
        final String email = extractUsername(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates token signature and structure; logs the specific failure reason
     * so we can distinguish between expired tokens (normal), tampered tokens
     * (security alert), and malformed tokens (client bug).
     *
     * Returns false rather than throwing, so the filter chain can continue
     * and let Spring Security return a proper 401 rather than a 500.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT signature invalid — possible token tampering: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
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
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

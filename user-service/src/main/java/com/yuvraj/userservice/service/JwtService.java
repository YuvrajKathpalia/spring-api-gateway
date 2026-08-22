package com.yuvraj.userservice.service;

import com.yuvraj.userservice.config.JwtProperties;
import com.yuvraj.userservice.entity.UserEntity;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Issues signed JWTs. This service is the token ISSUER; the gateway is the verifier.
 *
 * <p>Claims embedded in every token:
 * <ul>
 *   <li>{@code sub}   — the userId</li>
 *   <li>{@code email} — the user's email</li>
 *   <li>{@code roles} — the user's role(s)</li>
 * </ul>
 * Signed with HS256 using the shared secret from {@link JwtProperties}.
 */
@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        // HMAC-SHA256 key built from the raw secret bytes. The gateway rebuilds the
        // same key from the same secret to verify signatures.
        this.signingKey = new SecretKeySpec(
                properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * Builds a signed JWT for the given user.
     */
    public String issueToken(UserEntity user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getExpiration());

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("roles", user.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /** Token lifetime in seconds — surfaced to the client in the login response. */
    public long expiresInSeconds() {
        return properties.getExpiration().toSeconds();
    }
}

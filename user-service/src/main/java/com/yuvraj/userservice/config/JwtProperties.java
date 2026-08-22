package com.yuvraj.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Externalized JWT settings, bound from the {@code jwt.*} config keys.
 *
 * <p>The {@code secret} is shared with the gateway (the verifier) via the
 * {@code JWT_SECRET} environment variable — this service SIGNS tokens, the
 * gateway VERIFIES them, so both must agree on the same secret.
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HMAC-SHA256 signing secret. Injected via JWT_SECRET; never hardcode a real value. */
    private String secret;

    /** How long an issued token stays valid. */
    private Duration expiration = Duration.ofHours(24);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }
}

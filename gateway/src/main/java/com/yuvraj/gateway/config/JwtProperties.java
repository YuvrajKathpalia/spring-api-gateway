package com.yuvraj.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The gateway's copy of the JWT secret. Must match the user-service secret
 * (both read {@code JWT_SECRET}) — user-service signs, the gateway verifies.
 */
@ConfigurationProperties(prefix = "gateway.jwt")
public class JwtProperties {

    /** HMAC-SHA256 secret, injected via JWT_SECRET. */
    private String secret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}

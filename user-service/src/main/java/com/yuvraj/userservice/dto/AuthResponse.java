package com.yuvraj.userservice.dto;

/**
 * Returned by {@code POST /auth/login}. The {@code token} is a signed JWT the
 * client then sends as {@code Authorization: Bearer <token>} on protected calls.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds
) {
    public static AuthResponse bearer(String token, long expiresInSeconds) {
        return new AuthResponse(token, "Bearer", expiresInSeconds);
    }
}

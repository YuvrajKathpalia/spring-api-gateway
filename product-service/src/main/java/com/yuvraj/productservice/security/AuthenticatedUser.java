package com.yuvraj.productservice.security;

/**
 * The caller's identity as established by the gateway and passed down via headers.
 * This service never sees or parses the JWT — it trusts these values.
 */
public record AuthenticatedUser(Long userId, String email, String roles) {
}

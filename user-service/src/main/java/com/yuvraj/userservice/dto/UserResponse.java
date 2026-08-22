package com.yuvraj.userservice.dto;

import com.yuvraj.userservice.entity.UserEntity;

import java.time.Instant;

/**
 * Safe outward view of a user — never exposes the password hash.
 */
public record UserResponse(
        Long id,
        String email,
        String role,
        Instant createdAt
) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}

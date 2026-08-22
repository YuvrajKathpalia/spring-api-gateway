package com.yuvraj.userservice.controller;

import com.yuvraj.userservice.dto.UserResponse;
import com.yuvraj.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Protected user endpoints. Requests reach here only after the gateway has verified
 * the JWT and injected the caller's identity as headers.
 *
 * <p>This service TRUSTS the {@code X-User-Id} header — it never parses a JWT itself.
 * The gateway is the single security boundary.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> profile(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }
}

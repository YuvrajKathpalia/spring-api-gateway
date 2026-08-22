package com.yuvraj.userservice.service;

import com.yuvraj.userservice.dto.AuthResponse;
import com.yuvraj.userservice.dto.LoginRequest;
import com.yuvraj.userservice.dto.RegisterRequest;
import com.yuvraj.userservice.dto.UserResponse;
import com.yuvraj.userservice.entity.UserEntity;
import com.yuvraj.userservice.exception.EmailAlreadyExistsException;
import com.yuvraj.userservice.exception.InvalidCredentialsException;
import com.yuvraj.userservice.exception.UserNotFoundException;
import com.yuvraj.userservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core user use-cases: register, authenticate, and fetch profile.
 * Controllers stay thin; all business rules live here.
 */
@Service
public class UserService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user with a BCrypt-hashed password.
     *
     * @throws EmailAlreadyExistsException if the email is already taken
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        String hash = passwordEncoder.encode(request.password());
        UserEntity saved = userRepository.save(new UserEntity(request.email(), hash, DEFAULT_ROLE));
        return UserResponse.from(saved);
    }

    /**
     * Verifies credentials and issues a JWT on success.
     *
     * @throws InvalidCredentialsException if the email is unknown or password mismatches
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.issueToken(user);
        return AuthResponse.bearer(token, jwtService.expiresInSeconds());
    }

    /**
     * Loads a user's profile by id. The id is resolved by the gateway from the JWT
     * and forwarded as the {@code X-User-Id} header.
     *
     * @throws UserNotFoundException if no such user exists
     */
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return UserResponse.from(user);
    }
}

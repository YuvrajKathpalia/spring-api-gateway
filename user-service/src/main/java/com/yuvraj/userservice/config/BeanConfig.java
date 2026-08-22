package com.yuvraj.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Infrastructure beans that don't belong to a specific domain service.
 */
@Configuration
public class BeanConfig {

    /**
     * BCrypt encoder for hashing passwords at registration and matching them at login.
     * Pulled from spring-security-crypto only — we are NOT enabling the security filter chain.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

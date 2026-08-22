package com.yuvraj.userservice.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("No user found with id " + userId);
    }
}

package com.yuvraj.userservice.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        // Deliberately vague — do not reveal whether the email or the password was wrong.
        super("Invalid email or password");
    }
}

package com.example.jwtauth.error;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(message);
    }
}

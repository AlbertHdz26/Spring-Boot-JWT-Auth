package com.example.jwtauth.user.dto;

public record UserProfileResponse(
        String id,
        String email,
        String role) {
}

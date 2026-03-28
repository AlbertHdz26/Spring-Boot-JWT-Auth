package com.example.jwtauth.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserSummary user) {

    public record UserSummary(
            String id,
            String email,
            String role) {
    }
}

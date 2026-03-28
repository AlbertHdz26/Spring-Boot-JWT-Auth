package com.example.jwtauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app.admin-bootstrap")
public record AdminBootstrapProperties(
        boolean enabled,
        @NotBlank String email,
        @NotBlank String password) {
}

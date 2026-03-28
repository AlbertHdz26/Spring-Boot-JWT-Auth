package com.example.jwtauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin-bootstrap")
public record AdminBootstrapProperties(
        boolean enabled,
        String email,
        String password) {
}

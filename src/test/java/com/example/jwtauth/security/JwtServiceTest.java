package com.example.jwtauth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jwtauth.config.JwtProperties;
import com.example.jwtauth.user.Role;
import com.example.jwtauth.user.User;
import java.time.Duration;
import java.lang.reflect.Constructor;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(
                "01234567890123456789012345678901",
                Duration.ofMinutes(15),
                Duration.ofDays(7)));
    }

    @Test
    void generateAccessTokenShouldEncodeSubjectAndRole() {
        User user = newUser();
        user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setEmail("user@example.com");
        user.setRole(Role.USER);

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractSubject(token)).isEqualTo("user@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValidShouldRejectInvalidToken() {
        assertThat(jwtService.isTokenValid("bad-token")).isFalse();
    }

    private User newUser() {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            return BeanUtils.instantiateClass(constructor);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create User test fixture", exception);
        }
    }
}

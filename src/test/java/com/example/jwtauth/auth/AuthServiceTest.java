package com.example.jwtauth.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.example.jwtauth.auth.dto.AuthResponse;
import com.example.jwtauth.auth.dto.LoginRequest;
import com.example.jwtauth.auth.dto.LogoutRequest;
import com.example.jwtauth.auth.dto.RefreshTokenRequest;
import com.example.jwtauth.auth.dto.RegisterRequest;
import com.example.jwtauth.config.JwtProperties;
import com.example.jwtauth.error.ConflictException;
import com.example.jwtauth.error.UnauthorizedException;
import com.example.jwtauth.security.JwtService;
import com.example.jwtauth.token.RefreshToken;
import com.example.jwtauth.token.RefreshTokenRepository;
import com.example.jwtauth.token.RefreshTokenService;
import com.example.jwtauth.user.Role;
import com.example.jwtauth.user.User;
import com.example.jwtauth.user.UserRepository;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(new JwtProperties(
                SECRET,
                Duration.ofMinutes(15),
                Duration.ofDays(7)));
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, new JwtProperties(
                SECRET,
                Duration.ofMinutes(15),
                Duration.ofDays(7)));
        authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void registerShouldCreateUserAndIssueTokens() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0, User.class);
            user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return user;
        });
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(new RegisterRequest("user@example.com", "Password123!"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("user@example.com");
        assertThat(response.user().role()).isEqualTo("USER");
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(passwordEncoder.matches("Password123!", userCaptor.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("user@example.com", "Password123!")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginShouldIssueTokensForValidCredentials() {
        User user = newUser("user@example.com", "Password123!", Role.USER);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "Password123!"));

        assertThat(response.user().email()).isEqualTo("user@example.com");
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void loginShouldRejectInvalidCredentials() {
        User user = newUser("user@example.com", "Password123!", Role.USER);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void refreshShouldRotateRefreshToken() {
        User user = newUser("user@example.com", "Password123!", Role.USER);
        RefreshToken existingToken = instantiateRefreshToken();
        existingToken.setToken("existing-token");
        existingToken.setUser(user);
        existingToken.setRevoked(false);
        existingToken.setExpiresAt(java.time.OffsetDateTime.now().plusHours(1));

        when(refreshTokenRepository.findByToken("existing-token")).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.refresh(new RefreshTokenRequest("existing-token"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(existingToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getAllValues()).hasSize(2);
        assertThat(refreshTokenCaptor.getAllValues().get(0).isRevoked()).isTrue();
        assertThat(refreshTokenCaptor.getAllValues().get(1).isRevoked()).isFalse();
    }

    @Test
    void logoutShouldRevokeRefreshToken() {
        User user = newUser("user@example.com", "Password123!", Role.USER);
        RefreshToken existingToken = instantiateRefreshToken();
        existingToken.setToken("existing-token");
        existingToken.setUser(user);
        existingToken.setRevoked(false);
        existingToken.setExpiresAt(java.time.OffsetDateTime.now().plusHours(1));

        when(refreshTokenRepository.findByToken("existing-token")).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.logout(new LogoutRequest("existing-token"));

        assertThat(existingToken.isRevoked()).isTrue();
    }

    @Test
    void logoutShouldRejectUnknownToken() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(new LogoutRequest("missing-token")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid refresh token");
    }

    private static User newUser(String email, String rawPassword, Role role) {
        User user = instantiateUser();
        user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setEmail(email);
        user.setPasswordHash(new BCryptPasswordEncoder().encode(rawPassword));
        user.setRole(role);
        return user;
    }

    private static User instantiateUser() {
        try {
            return BeanUtils.instantiateClass(User.class.getDeclaredConstructor());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create User test fixture", exception);
        }
    }

    private static RefreshToken instantiateRefreshToken() {
        try {
            return BeanUtils.instantiateClass(RefreshToken.class.getDeclaredConstructor());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create RefreshToken test fixture", exception);
        }
    }
}

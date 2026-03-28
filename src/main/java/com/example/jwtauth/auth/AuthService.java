package com.example.jwtauth.auth;

import com.example.jwtauth.auth.dto.AuthResponse;
import com.example.jwtauth.auth.dto.LoginRequest;
import com.example.jwtauth.auth.dto.LogoutRequest;
import com.example.jwtauth.auth.dto.RefreshTokenRequest;
import com.example.jwtauth.auth.dto.RegisterRequest;
import com.example.jwtauth.error.ConflictException;
import com.example.jwtauth.error.UnauthorizedException;
import com.example.jwtauth.security.JwtService;
import com.example.jwtauth.token.RefreshToken;
import com.example.jwtauth.token.RefreshTokenService;
import com.example.jwtauth.user.Role;
import com.example.jwtauth.user.User;
import com.example.jwtauth.user.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }

        User user = newUser();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);
        return issueTokens(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken currentRefreshToken = refreshTokenService.validateActiveToken(request.refreshToken());
        User user = currentRefreshToken.getUser();
        refreshTokenService.revoke(currentRefreshToken);
        return issueTokens(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createToken(user);
        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                TOKEN_TYPE,
                new AuthResponse.UserSummary(
                        user.getId().toString(),
                        user.getEmail(),
                        user.getRole().name()));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static User newUser() {
        try {
            return BeanUtils.instantiateClass(User.class.getDeclaredConstructor());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create User instance", exception);
        }
    }
}

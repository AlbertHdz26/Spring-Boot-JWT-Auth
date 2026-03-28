package com.example.jwtauth.config;

import com.example.jwtauth.user.Role;
import com.example.jwtauth.user.User;
import com.example.jwtauth.user.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(
            AdminBootstrapProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }

        String email = normalizeEmail(properties.email());
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User admin = newAdminUser();
        admin.setId(UUID.randomUUID());
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(properties.password()));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static User newAdminUser() {
        try {
            return BeanUtils.instantiateClass(User.class.getDeclaredConstructor());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create admin user instance", exception);
        }
    }
}

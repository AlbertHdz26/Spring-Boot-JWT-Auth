# Spring Boot JWT Auth PostgreSQL Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot 3 / Java 21 REST API with JWT authentication, refresh-token rotation, PostgreSQL persistence, Flyway migrations, and basic `USER` / `ADMIN` authorization.

**Architecture:** The application is a layered monolith with focused packages for auth, users, security, persistence, and error handling. PostgreSQL is provisioned locally with a dedicated app user, the schema is created by Flyway, and Spring Security enforces stateless JWT-based access control while refresh tokens remain persisted and revocable.

**Tech Stack:** Java 21, Spring Boot, Spring Web, Spring Security, Spring Data JPA, PostgreSQL, Flyway, Bean Validation, JUnit 5, Mockito, Spring Boot Test

---

## File Structure

Expected files and responsibilities:

- Create: `pom.xml`
  Maven project definition and dependencies.
- Create: `src/main/java/com/example/jwtauth/JwtAuthApplication.java`
  Spring Boot entry point.
- Create: `src/main/java/com/example/jwtauth/config/JwtProperties.java`
  Externalized JWT configuration properties.
- Create: `src/main/java/com/example/jwtauth/config/AdminBootstrapProperties.java`
  Externalized admin bootstrap settings.
- Create: `src/main/java/com/example/jwtauth/config/AdminBootstrapRunner.java`
  Creates the initial admin user in non-production profiles.
- Create: `src/main/java/com/example/jwtauth/user/Role.java`
  Role enum.
- Create: `src/main/java/com/example/jwtauth/user/User.java`
  User JPA entity.
- Create: `src/main/java/com/example/jwtauth/token/RefreshToken.java`
  Refresh token JPA entity.
- Create: `src/main/java/com/example/jwtauth/user/UserRepository.java`
  User repository.
- Create: `src/main/java/com/example/jwtauth/token/RefreshTokenRepository.java`
  Refresh token repository.
- Create: `src/main/java/com/example/jwtauth/auth/dto/RegisterRequest.java`
  Public registration request DTO.
- Create: `src/main/java/com/example/jwtauth/auth/dto/LoginRequest.java`
  Login request DTO.
- Create: `src/main/java/com/example/jwtauth/auth/dto/RefreshTokenRequest.java`
  Refresh request DTO.
- Create: `src/main/java/com/example/jwtauth/auth/dto/LogoutRequest.java`
  Logout request DTO.
- Create: `src/main/java/com/example/jwtauth/auth/dto/AuthResponse.java`
  Token response DTO.
- Create: `src/main/java/com/example/jwtauth/user/dto/UserProfileResponse.java`
  Current-user response DTO.
- Create: `src/main/java/com/example/jwtauth/error/ErrorResponse.java`
  Standard API error response.
- Create: `src/main/java/com/example/jwtauth/error/GlobalExceptionHandler.java`
  Centralized exception translation.
- Create: `src/main/java/com/example/jwtauth/error/ApiException.java`
  Base business exception.
- Create: `src/main/java/com/example/jwtauth/error/ConflictException.java`
  Duplicate-resource exception.
- Create: `src/main/java/com/example/jwtauth/error/UnauthorizedException.java`
  Authentication/invalid-token exception.
- Create: `src/main/java/com/example/jwtauth/security/JwtService.java`
  JWT generation and validation logic.
- Create: `src/main/java/com/example/jwtauth/security/JwtAuthenticationFilter.java`
  Parses bearer tokens and populates `SecurityContext`.
- Create: `src/main/java/com/example/jwtauth/security/SecurityConfig.java`
  Security filter chain and password encoder.
- Create: `src/main/java/com/example/jwtauth/security/AppUserDetailsService.java`
  Loads users for authentication and JWT processing.
- Create: `src/main/java/com/example/jwtauth/auth/AuthService.java`
  Registration, login, refresh, logout orchestration.
- Create: `src/main/java/com/example/jwtauth/token/RefreshTokenService.java`
  Refresh token creation, lookup, validation, rotation, revocation.
- Create: `src/main/java/com/example/jwtauth/user/UserService.java`
  Current user lookup.
- Create: `src/main/java/com/example/jwtauth/auth/AuthController.java`
  Auth endpoints.
- Create: `src/main/java/com/example/jwtauth/user/UserController.java`
  `/users/me` endpoint.
- Create: `src/main/java/com/example/jwtauth/admin/AdminController.java`
  Role-protected admin test endpoint.
- Create: `src/main/resources/application.yml`
  Base Spring, database, Flyway, and JWT configuration.
- Create: `src/main/resources/application-dev.yml`
  Local development overrides.
- Create: `src/main/resources/application-test.yml`
  Test overrides.
- Create: `src/main/resources/db/migration/V1__create_users_and_refresh_tokens.sql`
  Initial schema migration.
- Create: `src/test/java/com/example/jwtauth/auth/AuthServiceTest.java`
  Unit tests for auth business logic.
- Create: `src/test/java/com/example/jwtauth/security/JwtServiceTest.java`
  Unit tests for JWT creation/validation.
- Create: `src/test/java/com/example/jwtauth/AuthIntegrationTest.java`
  End-to-end auth and authorization tests.
- Create: `src/test/resources/application-test.yml`
  Test configuration if split from main resources.

### Task 1: Scaffold the Spring Boot project

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/example/jwtauth/JwtAuthApplication.java`

- [ ] **Step 1: Write the failing project bootstrap expectation**

Define the expected Maven shape before implementation:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>jwt-auth</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <properties>
    <java.version>21</java.version>
    <spring-boot.version>3.5.0</spring-boot.version>
  </properties>
</project>
```

- [ ] **Step 2: Run bootstrap command to verify the project is not ready yet**

Run: `./mvnw test`
Expected: FAIL because the Maven wrapper and project files do not exist yet.

- [ ] **Step 3: Write the minimal project files**

Create `pom.xml` with only the required dependencies:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

Create the application entry point:

```java
package com.example.jwtauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JwtAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(JwtAuthApplication.class, args);
    }
}
```

- [ ] **Step 4: Run the project tests to verify bootstrap compiles**

Run: `mvn test`
Expected: PASS with zero or no meaningful tests executed, but the project builds.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/com/example/jwtauth/JwtAuthApplication.java
git commit -m "build: scaffold spring boot jwt auth project"
```

### Task 2: Provision local PostgreSQL

**Files:**
- No repository file changes required for the initial DB/user creation

- [ ] **Step 1: Verify PostgreSQL tooling is available**

Run: `psql --version`
Expected: PASS and print a PostgreSQL client version.

- [ ] **Step 2: Verify the app database does not already exist or inspect current state**

Run: `psql -d postgres -c "\\l"`
Expected: PASS and list databases; confirm whether `jwt_auth_db` exists.

- [ ] **Step 3: Create the dedicated PostgreSQL role**

Run:

```bash
psql -d postgres -c "CREATE ROLE jwt_app WITH LOGIN PASSWORD 'change-me-dev-password';"
```

Expected: `CREATE ROLE` or a clear error that the role already exists.

- [ ] **Step 4: Create the application database owned by the app role**

Run:

```bash
psql -d postgres -c "CREATE DATABASE jwt_auth_db OWNER jwt_app;"
```

Expected: `CREATE DATABASE` or a clear error that it already exists.

- [ ] **Step 5: Verify the role can connect to the database**

Run:

```bash
PGPASSWORD=change-me-dev-password psql -h localhost -U jwt_app -d jwt_auth_db -c "SELECT current_database(), current_user;"
```

Expected: one row with `jwt_auth_db` and `jwt_app`.

- [ ] **Step 6: Commit**

```bash
git commit --allow-empty -m "chore: provision local postgresql database"
```

### Task 3: Define application configuration

**Files:**
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-dev.yml`
- Create: `src/main/resources/application-test.yml`
- Create: `src/main/java/com/example/jwtauth/config/JwtProperties.java`
- Create: `src/main/java/com/example/jwtauth/config/AdminBootstrapProperties.java`

- [ ] **Step 1: Write the failing configuration binding test case**

Target properties to support:

```yaml
app:
  jwt:
    secret: 01234567890123456789012345678901
    access-token-expiration: PT15M
    refresh-token-expiration: P7D
  admin-bootstrap:
    enabled: true
    email: admin@example.com
    password: ChangeMe123!
```

- [ ] **Step 2: Run the app startup to verify properties classes do not exist yet**

Run: `mvn -Dspring-boot.run.profiles=dev spring-boot:run`
Expected: FAIL because the properties classes and config files are not implemented yet.

- [ ] **Step 3: Write minimal externalized configuration**

`application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/jwt_auth_db
    username: jwt_app
    password: ${DB_PASSWORD:change-me-dev-password}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
  jackson:
    serialization:
      write-dates-as-timestamps: false

app:
  jwt:
    secret: ${JWT_SECRET:01234567890123456789012345678901}
    access-token-expiration: PT15M
    refresh-token-expiration: P7D
  admin-bootstrap:
    enabled: false
    email: ${ADMIN_EMAIL:admin@example.com}
    password: ${ADMIN_PASSWORD:ChangeMe123!}
```

`application-dev.yml`:

```yaml
spring:
  config:
    activate:
      on-profile: dev

app:
  admin-bootstrap:
    enabled: true
```

`application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
  sql:
    init:
      mode: never

app:
  jwt:
    secret: 01234567890123456789012345678901
    access-token-expiration: PT15M
    refresh-token-expiration: P7D
  admin-bootstrap:
    enabled: false
    email: admin@example.com
    password: ChangeMe123!
```

`JwtProperties.java`:

```java
package com.example.jwtauth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration) {
}
```

`AdminBootstrapProperties.java`:

```java
package com.example.jwtauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin-bootstrap")
public record AdminBootstrapProperties(
        boolean enabled,
        String email,
        String password) {
}
```

- [ ] **Step 4: Run the build to verify configuration compiles**

Run: `mvn test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources src/main/java/com/example/jwtauth/config
git commit -m "config: externalize jwt and datasource settings"
```

### Task 4: Create the database schema with Flyway

**Files:**
- Create: `src/main/resources/db/migration/V1__create_users_and_refresh_tokens.sql`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Write the failing schema expectation**

Expected tables and constraints:

```sql
users(id uuid primary key, email unique not null, password_hash not null, role not null);
refresh_tokens(id uuid primary key, token unique not null, user_id references users(id));
```

- [ ] **Step 2: Run Flyway-backed startup before creating the migration**

Run: `mvn -Dspring-boot.run.profiles=dev spring-boot:run`
Expected: FAIL during startup because Hibernate validation finds missing tables.

- [ ] **Step 3: Write the initial migration**

`V1__create_users_and_refresh_tokens.sql`:

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

- [ ] **Step 4: Start the application and verify Flyway creates the schema**

Run: `mvn -Dspring-boot.run.profiles=dev spring-boot:run`
Expected: PASS; logs show Flyway migration `V1` applied and the app starts.

- [ ] **Step 5: Verify the tables in PostgreSQL**

Run:

```bash
PGPASSWORD=change-me-dev-password psql -h localhost -U jwt_app -d jwt_auth_db -c "\dt"
```

Expected: `users` and `refresh_tokens` appear.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/db/migration/V1__create_users_and_refresh_tokens.sql src/main/resources/application.yml
git commit -m "db: add initial flyway schema for auth tables"
```

### Task 5: Implement the domain model and repositories

**Files:**
- Create: `src/main/java/com/example/jwtauth/user/Role.java`
- Create: `src/main/java/com/example/jwtauth/user/User.java`
- Create: `src/main/java/com/example/jwtauth/token/RefreshToken.java`
- Create: `src/main/java/com/example/jwtauth/user/UserRepository.java`
- Create: `src/main/java/com/example/jwtauth/token/RefreshTokenRepository.java`

- [ ] **Step 1: Write the failing repository expectations**

Expected repository methods:

```java
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);
Optional<RefreshToken> findByToken(String token);
List<RefreshToken> findAllByUserId(UUID userId);
```

- [ ] **Step 2: Run the build to verify the domain types are missing**

Run: `mvn test`
Expected: FAIL because entities and repositories do not exist.

- [ ] **Step 3: Write the minimal entities and repositories**

`Role.java`:

```java
package com.example.jwtauth.user;

public enum Role {
    USER,
    ADMIN
}
```

`User.java`:

```java
package com.example.jwtauth.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
```

`RefreshToken.java`:

```java
package com.example.jwtauth.token;

import com.example.jwtauth.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = OffsetDateTime.now();
    }
}
```

- [ ] **Step 4: Run the build to verify entity mapping passes**

Run: `mvn test`
Expected: PASS or at least compile success if no tests exist yet.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/jwtauth/user src/main/java/com/example/jwtauth/token
git commit -m "feat: add auth domain entities and repositories"
```

### Task 6: Implement JWT generation and validation

**Files:**
- Create: `src/main/java/com/example/jwtauth/security/JwtService.java`
- Create: `src/test/java/com/example/jwtauth/security/JwtServiceTest.java`

- [ ] **Step 1: Write the failing JWT tests**

```java
@Test
void shouldGenerateTokenContainingSubjectAndRole() {
    String token = jwtService.generateAccessToken(user);
    assertThat(jwtService.extractSubject(token)).isEqualTo("user@example.com");
    assertThat(jwtService.extractRole(token)).isEqualTo("USER");
}

@Test
void shouldRejectInvalidToken() {
    assertThat(jwtService.isTokenValid("bad-token")).isFalse();
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `mvn -Dtest=JwtServiceTest test`
Expected: FAIL because `JwtService` does not exist yet.

- [ ] **Step 3: Write the minimal JWT service**

Core implementation shape:

```java
public String generateAccessToken(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
            .subject(user.getEmail())
            .claim("role", user.getRole().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(jwtProperties.accessTokenExpiration())))
            .signWith(signingKey())
            .compact();
}

public boolean isTokenValid(String token) {
    try {
        parseClaims(token);
        return true;
    } catch (JwtException | IllegalArgumentException exception) {
        return false;
    }
}
```

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `mvn -Dtest=JwtServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/jwtauth/security/JwtService.java src/test/java/com/example/jwtauth/security/JwtServiceTest.java
git commit -m "feat: add jwt generation and validation service"
```

### Task 7: Implement refresh-token lifecycle

**Files:**
- Create: `src/main/java/com/example/jwtauth/token/RefreshTokenService.java`
- Create: `src/test/java/com/example/jwtauth/auth/AuthServiceTest.java`

- [ ] **Step 1: Write the failing refresh-token behavior tests**

```java
@Test
void shouldCreatePersistedRefreshToken() {
    RefreshToken refreshToken = refreshTokenService.createToken(user);
    assertThat(refreshToken.getToken()).isNotBlank();
    assertThat(refreshToken.isRevoked()).isFalse();
}

@Test
void shouldRejectRevokedToken() {
    assertThatThrownBy(() -> refreshTokenService.validateActiveToken("revoked-token"))
            .isInstanceOf(UnauthorizedException.class);
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `mvn -Dtest=AuthServiceTest test`
Expected: FAIL because the token service and exception types are missing.

- [ ] **Step 3: Write the minimal refresh-token service**

Core implementation shape:

```java
public RefreshToken createToken(User user) {
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken(UUID.randomUUID() + "." + UUID.randomUUID());
    refreshToken.setUser(user);
    refreshToken.setExpiresAt(OffsetDateTime.now().plus(jwtProperties.refreshTokenExpiration()));
    refreshToken.setRevoked(false);
    return refreshTokenRepository.save(refreshToken);
}

public RefreshToken validateActiveToken(String rawToken) {
    RefreshToken refreshToken = refreshTokenRepository.findByToken(rawToken)
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
    if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
        throw new UnauthorizedException("Refresh token is expired or revoked");
    }
    return refreshToken;
}
```

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `mvn -Dtest=AuthServiceTest test`
Expected: PASS for the new refresh-token unit tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/jwtauth/token/RefreshTokenService.java src/test/java/com/example/jwtauth/auth/AuthServiceTest.java
git commit -m "feat: add refresh token lifecycle service"
```

### Task 8: Implement auth service, DTOs, and exceptions

**Files:**
- Create: `src/main/java/com/example/jwtauth/auth/AuthService.java`
- Create: `src/main/java/com/example/jwtauth/auth/dto/RegisterRequest.java`
- Create: `src/main/java/com/example/jwtauth/auth/dto/LoginRequest.java`
- Create: `src/main/java/com/example/jwtauth/auth/dto/RefreshTokenRequest.java`
- Create: `src/main/java/com/example/jwtauth/auth/dto/LogoutRequest.java`
- Create: `src/main/java/com/example/jwtauth/auth/dto/AuthResponse.java`
- Create: `src/main/java/com/example/jwtauth/error/ApiException.java`
- Create: `src/main/java/com/example/jwtauth/error/ConflictException.java`
- Create: `src/main/java/com/example/jwtauth/error/UnauthorizedException.java`

- [ ] **Step 1: Write the failing auth service tests**

```java
@Test
void shouldRegisterUserWithDefaultRole() {
    AuthResponse response = authService.register(new RegisterRequest("user@example.com", "Password123!"));
    assertThat(response.user().role()).isEqualTo("USER");
}

@Test
void shouldRotateRefreshTokenOnRefresh() {
    AuthResponse response = authService.refresh(new RefreshTokenRequest(existingToken));
    assertThat(response.refreshToken()).isNotEqualTo(existingToken);
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `mvn -Dtest=AuthServiceTest test`
Expected: FAIL because `AuthService` and DTOs are incomplete.

- [ ] **Step 3: Write the minimal auth service and DTOs**

Service shape:

```java
public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
        throw new ConflictException("Email already registered");
    }
    User user = new User();
    user.setEmail(request.email().trim().toLowerCase());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(Role.USER);
    User savedUser = userRepository.save(user);
    return issueTokens(savedUser);
}

public AuthResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.email().trim().toLowerCase())
            .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
        throw new UnauthorizedException("Invalid credentials");
    }
    return issueTokens(user);
}

public AuthResponse refresh(RefreshTokenRequest request) {
    RefreshToken currentToken = refreshTokenService.validateActiveToken(request.refreshToken());
    refreshTokenService.revoke(currentToken);
    return issueTokens(currentToken.getUser());
}
```

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `mvn -Dtest=AuthServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/jwtauth/auth src/main/java/com/example/jwtauth/error
git commit -m "feat: add auth service and request response contracts"
```

### Task 9: Implement Spring Security integration

**Files:**
- Create: `src/main/java/com/example/jwtauth/security/AppUserDetailsService.java`
- Create: `src/main/java/com/example/jwtauth/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/example/jwtauth/security/SecurityConfig.java`

- [ ] **Step 1: Write the failing security integration expectation**

Security rules to enforce:

```java
requestMatchers("/api/v1/auth/**").permitAll();
requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
anyRequest().authenticated();
```

- [ ] **Step 2: Run the build or integration tests to verify security wiring is missing**

Run: `mvn test`
Expected: FAIL because the security components are not implemented.

- [ ] **Step 3: Write the minimal security integration**

`SecurityConfig` core:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated())
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
}
```

`JwtAuthenticationFilter` core:

```java
String header = request.getHeader(HttpHeaders.AUTHORIZATION);
if (header == null || !header.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);
    return;
}
String token = header.substring(7);
if (!jwtService.isTokenValid(token)) {
    filterChain.doFilter(request, response);
    return;
}
String email = jwtService.extractSubject(token);
UserDetails userDetails = userDetailsService.loadUserByUsername(email);
UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
SecurityContextHolder.getContext().setAuthentication(authentication);
```

- [ ] **Step 4: Run the full test suite to verify the security layer compiles**

Run: `mvn test`
Expected: PASS for unit tests; integration tests may still be pending until controllers exist.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/jwtauth/security
git commit -m "feat: wire jwt authentication into spring security"
```

### Task 10: Implement controllers and current-user flow

**Files:**
- Create: `src/main/java/com/example/jwtauth/auth/AuthController.java`
- Create: `src/main/java/com/example/jwtauth/user/UserController.java`
- Create: `src/main/java/com/example/jwtauth/admin/AdminController.java`
- Create: `src/main/java/com/example/jwtauth/user/UserService.java`
- Create: `src/main/java/com/example/jwtauth/user/dto/UserProfileResponse.java`

- [ ] **Step 1: Write the failing integration test cases**

```java
@Test
void registerShouldReturnCreatedTokens() throws Exception { }

@Test
void meShouldRequireAuthentication() throws Exception { }

@Test
void adminPingShouldRequireAdminRole() throws Exception { }
```

- [ ] **Step 2: Run the integration test class to verify it fails**

Run: `mvn -Dtest=AuthIntegrationTest test`
Expected: FAIL because the controllers are missing.

- [ ] **Step 3: Write the minimal controllers and user service**

Controller shapes:

```java
@PostMapping("/register")
@ResponseStatus(HttpStatus.CREATED)
public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
}

@GetMapping("/me")
public UserProfileResponse me(Authentication authentication) {
    return userService.getCurrentUser(authentication.getName());
}

@GetMapping("/ping")
public Map<String, String> ping() {
    return Map.of("message", "admin ok");
}
```

- [ ] **Step 4: Run the integration tests to verify endpoints behave correctly**

Run: `mvn -Dtest=AuthIntegrationTest test`
Expected: PASS for register, login, refresh, `/users/me`, and admin authorization checks.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/jwtauth/auth/AuthController.java src/main/java/com/example/jwtauth/user src/main/java/com/example/jwtauth/admin
git commit -m "feat: expose auth, user, and admin endpoints"
```

### Task 11: Implement API error handling

**Files:**
- Create: `src/main/java/com/example/jwtauth/error/ErrorResponse.java`
- Create: `src/main/java/com/example/jwtauth/error/GlobalExceptionHandler.java`

- [ ] **Step 1: Write the failing error-response assertions**

```java
@Test
void duplicateEmailShouldReturnConflictErrorBody() throws Exception { }

@Test
void invalidPayloadShouldReturnValidationErrorBody() throws Exception { }
```

- [ ] **Step 2: Run the integration test class to verify error handling is incomplete**

Run: `mvn -Dtest=AuthIntegrationTest test`
Expected: FAIL because the expected error body structure is not returned yet.

- [ ] **Step 3: Write the global exception handler**

Core response shape:

```java
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {
}
```

Handler examples:

```java
@ExceptionHandler(ConflictException.class)
ResponseEntity<ErrorResponse> handleConflict(ConflictException exception, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
}

@ExceptionHandler(MethodArgumentNotValidException.class)
ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Request validation failed", request.getRequestURI());
}
```

- [ ] **Step 4: Run the integration tests to verify error contracts pass**

Run: `mvn -Dtest=AuthIntegrationTest test`
Expected: PASS including error-shape assertions.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/jwtauth/error
git commit -m "feat: add consistent api error handling"
```

### Task 12: Bootstrap the admin user in dev profile

**Files:**
- Create: `src/main/java/com/example/jwtauth/config/AdminBootstrapRunner.java`
- Modify: `src/main/resources/application-dev.yml`

- [ ] **Step 1: Write the failing bootstrap expectation**

Expected behavior:

```java
if (properties.enabled() && !userRepository.existsByEmail(properties.email())) {
    // create ADMIN user
}
```

- [ ] **Step 2: Run the application in dev profile to verify the admin user is not created yet**

Run:

```bash
PGPASSWORD=change-me-dev-password mvn -Dspring-boot.run.profiles=dev spring-boot:run
```

Expected: APP starts, but querying `users` shows no bootstrap admin yet.

- [ ] **Step 3: Write the admin bootstrap runner**

Core implementation:

```java
@Component
@Profile("dev")
public class AdminBootstrapRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled() || userRepository.existsByEmail(properties.email().toLowerCase())) {
            return;
        }
        User admin = new User();
        admin.setEmail(properties.email().trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(properties.password()));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }
}
```

- [ ] **Step 4: Start the app and verify the admin user exists**

Run:

```bash
PGPASSWORD=change-me-dev-password mvn -Dspring-boot.run.profiles=dev spring-boot:run
```

Then verify:

```bash
PGPASSWORD=change-me-dev-password psql -h localhost -U jwt_app -d jwt_auth_db -c "SELECT email, role FROM users;"
```

Expected: one row for the configured admin email with role `ADMIN`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/jwtauth/config/AdminBootstrapRunner.java src/main/resources/application-dev.yml
git commit -m "feat: bootstrap dev admin user"
```

### Task 13: Build the integration test suite

**Files:**
- Create: `src/test/java/com/example/jwtauth/AuthIntegrationTest.java`
- Modify: `src/test/resources/application-test.yml`

- [ ] **Step 1: Write the full integration test class**

Core test scenarios:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Test
    void registerLoginRefreshAndMeFlowShouldWork() throws Exception { }

    @Test
    void anonymousUserShouldReceiveUnauthorizedOnMe() throws Exception { }

    @Test
    void nonAdminUserShouldReceiveForbiddenOnAdminEndpoint() throws Exception { }
}
```

- [ ] **Step 2: Run the integration tests to capture remaining failures**

Run: `mvn -Dtest=AuthIntegrationTest test`
Expected: FAIL initially on missing wiring or behavior gaps.

- [ ] **Step 3: Fill the test class with concrete HTTP assertions**

Use `MockMvc` to assert:

```java
mockMvc.perform(post("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"email":"user@example.com","password":"Password123!"}
            """))
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.accessToken").isString())
    .andExpect(jsonPath("$.refreshToken").isString())
    .andExpect(jsonPath("$.user.role").value("USER"));
```

- [ ] **Step 4: Run the full test suite**

Run: `mvn test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/example/jwtauth/AuthIntegrationTest.java src/test/resources/application-test.yml
git commit -m "test: add end-to-end auth integration coverage"
```

### Task 14: Final verification and manual smoke test

**Files:**
- No new files required

- [ ] **Step 1: Run the full verification suite**

Run: `mvn clean test`
Expected: PASS.

- [ ] **Step 2: Start the app against local PostgreSQL**

Run:

```bash
PGPASSWORD=change-me-dev-password mvn -Dspring-boot.run.profiles=dev spring-boot:run
```

Expected: PASS; app starts, Flyway is up to date, admin bootstrap runs once.

- [ ] **Step 3: Run manual HTTP smoke checks**

Run:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"ChangeMe123!"}'
```

Expected: JSON with `accessToken` and `refreshToken`.

Then use the returned access token:

```bash
curl -s http://localhost:8080/api/v1/admin/ping \
  -H "Authorization: Bearer <access-token>"
```

Expected: `{"message":"admin ok"}`.

- [ ] **Step 4: Record any discrepancies and fix them before closing**

Run: `mvn test`
Expected: PASS after any final adjustments.

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: complete jwt auth api with postgresql and flyway"
```

## Self-Review

Spec coverage:

- PostgreSQL local provisioning is covered in Task 2.
- Flyway schema creation is covered in Task 4.
- JWT access-token generation is covered in Task 6.
- Refresh-token persistence and rotation are covered in Tasks 7 and 8.
- Spring Security authorization is covered in Task 9.
- Auth, user, and admin endpoints are covered in Task 10.
- Uniform API errors are covered in Task 11.
- Non-production admin bootstrap is covered in Task 12.
- Unit and integration testing are covered in Tasks 6, 7, 8, 10, 11, 13, and 14.

Placeholder scan:

- No `TODO` or `TBD` placeholders remain.
- Commands, files, code shapes, and expected outcomes are specified per task.

Type consistency:

- `UUID` is used consistently as the primary key type.
- `Role` values remain `USER` and `ADMIN`.
- Auth DTO names match the spec.
- The persistence model and migration names align with the design document.

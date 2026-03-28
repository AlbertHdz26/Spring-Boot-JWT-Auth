package com.example.jwtauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.jwtauth.auth.dto.AuthResponse;
import com.example.jwtauth.token.RefreshTokenRepository;
import com.example.jwtauth.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String REGISTER_ENDPOINT = "/api/v1/auth/register";
    private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
    private static final String REFRESH_ENDPOINT = "/api/v1/auth/refresh";
    private static final String LOGOUT_ENDPOINT = "/api/v1/auth/logout";
    private static final String ME_ENDPOINT = "/api/v1/users/me";
    private static final String ADMIN_PING_ENDPOINT = "/api/v1/admin/ping";
    private static final String HEALTH_ENDPOINT = "/actuator/health";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void registerLoginRefreshAndMeFlowShouldWork() throws Exception {
        AuthResponse registerResponse = register("user@example.com", "Password123!");

        assertThat(registerResponse.tokenType()).isEqualTo("Bearer");
        assertThat(registerResponse.user().email()).isEqualTo("user@example.com");
        assertThat(registerResponse.user().role()).isEqualTo("USER");

        AuthResponse loginResponse = login("user@example.com", "Password123!");

        assertThat(loginResponse.accessToken()).isNotBlank();
        assertThat(loginResponse.refreshToken()).isNotBlank();
        assertThat(loginResponse.user().email()).isEqualTo("user@example.com");

        AuthResponse refreshResponse = refresh(loginResponse.refreshToken());

        assertThat(refreshResponse.accessToken()).isNotBlank();
        assertThat(refreshResponse.refreshToken()).isNotBlank();
        assertThat(refreshResponse.refreshToken()).isNotEqualTo(loginResponse.refreshToken());

        mockMvc.perform(get(ME_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, bearer(refreshResponse.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshResponse.refreshToken())))
                .andExpect(status().isNoContent());
    }

    @Test
    void usersMeShouldReturnUnauthorizedWithoutBearerToken() throws Exception {
        mockMvc.perform(get(ME_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminPingShouldReturnUnauthorizedWithoutBearerToken() throws Exception {
        mockMvc.perform(get(ADMIN_PING_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminPingShouldReturnForbiddenForNonAdminUser() throws Exception {
        AuthResponse registerResponse = register("user@example.com", "Password123!");
        AuthResponse loginResponse = login("user@example.com", "Password123!");

        mockMvc.perform(get(ADMIN_PING_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginResponse.accessToken())))
                .andExpect(status().isForbidden());

        assertThat(registerResponse.user().role()).isEqualTo("USER");
    }

    @Test
    void actuatorHealthShouldBePublic() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void registerShouldReturnConflictForDuplicateEmail() throws Exception {
        register("user@example.com", "Password123!");

        mockMvc.perform(post(REGISTER_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"Password123!"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already registered"))
                .andExpect(jsonPath("$.path").value(REGISTER_ENDPOINT))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void registerShouldReturnValidationErrorForInvalidPayload() throws Exception {
        mockMvc.perform(post(REGISTER_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value(REGISTER_ENDPOINT))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.message", not(isEmptyOrNullString())));
    }

    private AuthResponse register(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(REGISTER_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        return readAuthResponse(result);
    }

    private AuthResponse login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        return readAuthResponse(result);
    }

    private AuthResponse refresh(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post(REFRESH_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        return readAuthResponse(result);
    }

    private AuthResponse readAuthResponse(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}

package com.example.jwtauth.security;

import com.example.jwtauth.config.JwtProperties;
import com.example.jwtauth.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String ROLE_CLAIM = "role";
    private static final String SUBJECT_CLAIM = "sub";
    private static final String ISSUED_AT_CLAIM = "iat";
    private static final String EXPIRATION_CLAIM = "exp";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtProperties jwtProperties;
    private final SecretKeySpec secretKeySpec;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKeySpec = new SecretKeySpec(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(SUBJECT_CLAIM, user.getEmail());
        payload.put(ROLE_CLAIM, user.getRole().name());
        payload.put(ISSUED_AT_CLAIM, now.getEpochSecond());
        payload.put(EXPIRATION_CLAIM, now.plus(jwtProperties.accessTokenExpiration()).getEpochSecond());

        String encodedHeader = encode(HEADER_JSON);
        String encodedPayload = encode(toJson(payload));
        String unsignedToken = encodedHeader + "." + encodedPayload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    public String extractSubject(String token) {
        return parseClaims(token).subject();
    }

    public String extractRole(String token) {
        return parseClaims(token).role();
    }

    public boolean isTokenValid(String token) {
        try {
            TokenClaims claims = parseClaims(token);
            return claims.expiration().isAfter(Instant.now());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private TokenClaims parseClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid JWT signature");
        }

        JsonNode payload = readJson(decodeToString(parts[1]));
        String subject = requiredText(payload, SUBJECT_CLAIM);
        String role = requiredText(payload, ROLE_CLAIM);
        Instant expiration = Instant.ofEpochSecond(requiredLong(payload, EXPIRATION_CLAIM));
        return new TokenClaims(subject, role, expiration);
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));
            return ENCODER.encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }

    private static String encode(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeToString(String value) {
        return new String(DECODER.decode(value), StandardCharsets.UTF_8);
    }

    private static JsonNode readJson(String value) {
        try {
            return OBJECT_MAPPER.readTree(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid JWT payload", exception);
        }
    }

    private static String toJson(Map<String, Object> payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize JWT payload", exception);
        }
    }

    private static String requiredText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull() || !field.isTextual()) {
            throw new IllegalArgumentException("Missing JWT claim: " + fieldName);
        }
        return field.asText();
    }

    private static long requiredLong(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull() || !field.canConvertToLong()) {
            throw new IllegalArgumentException("Missing JWT claim: " + fieldName);
        }
        return field.asLong();
    }

    private record TokenClaims(String subject, String role, Instant expiration) {
    }
}

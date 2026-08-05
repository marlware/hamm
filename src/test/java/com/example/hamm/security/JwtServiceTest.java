package com.example.hamm.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hamm.user.Role;
import com.example.hamm.user.User;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-only-secret-key-not-for-production-use-minimum-256-bits-long", 3_600_000L);
        jwtService = new JwtService(properties);
        user = User.builder()
                .id(UUID.randomUUID())
                .email("jane@example.com")
                .fullName("Jane Doe")
                .passwordHash("hashed")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
    }

    @Test
    void generatesTokenContainingUsername() {
        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo(user.getUsername());
    }

    @Test
    void validatesTokenForMatchingUser() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void rejectsTokenForDifferentUser() {
        String token = jwtService.generateToken(user);
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .fullName("Other")
                .passwordHash("hashed")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void rejectsMalformedToken() {
        assertThat(jwtService.isTokenValid("not-a-real-token", user)).isFalse();
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtProperties shortLived = new JwtProperties(
                "test-only-secret-key-not-for-production-use-minimum-256-bits-long", 1L);
        JwtService shortLivedService = new JwtService(shortLived);
        String token = shortLivedService.generateToken(user);

        Thread.sleep(10);

        assertThat(shortLivedService.isTokenValid(token, user)).isFalse();
    }
}

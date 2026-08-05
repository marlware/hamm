package com.example.hamm;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hamm.account.CreateAccountRequest;
import com.example.hamm.auth.AuthResponse;
import com.example.hamm.auth.RegisterRequest;
import com.example.hamm.transaction.DepositRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * End-to-end check against a real PostgreSQL instance (via Testcontainers) rather
 * than the H2 in-memory database used by the rest of the test suite. The whole
 * class is skipped when a Docker daemon isn't available (e.g. sandboxed dev
 * environments) instead of failing the build; CI runners provide Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class PostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("hamm")
            .withUsername("hamm")
            .withPassword("hamm");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("hamm.jwt.secret",
                () -> "test-only-secret-key-not-for-production-use-minimum-256-bits-long");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerCreateAccountAndDepositAgainstRealPostgres() throws Exception {
        String email = "pg-" + UUID.randomUUID() + "@example.com";
        RegisterRequest register = new RegisterRequest(email, "password123", "PG User");

        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", register, AuthResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = registerResponse.getBody().accessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> accountResponse = restTemplate.exchange(
                "/api/v1/accounts", HttpMethod.POST,
                new HttpEntity<>(new CreateAccountRequest("USD"), headers), String.class);
        assertThat(accountResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String accountId = objectMapper.readTree(accountResponse.getBody()).get("id").asText();

        ResponseEntity<String> depositResponse = restTemplate.exchange(
                "/api/v1/transactions/deposit", HttpMethod.POST,
                new HttpEntity<>(new DepositRequest(UUID.fromString(accountId), new BigDecimal("50.00"), null, null),
                        headers),
                String.class);
        assertThat(depositResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> balanceResponse = restTemplate.exchange(
                "/api/v1/accounts/{id}/balance", HttpMethod.GET, new HttpEntity<>(headers), String.class, accountId);
        assertThat(balanceResponse.getBody()).contains("50.0");
    }
}

package com.example.hamm.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.hamm.account.CreateAccountRequest;
import com.example.hamm.auth.AuthResponse;
import com.example.hamm.auth.LoginRequest;
import com.example.hamm.auth.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner-" + UUID.randomUUID() + "@example.com");
        otherToken = registerAndLogin("other-" + UUID.randomUUID() + "@example.com");
    }

    @Test
    void depositWithdrawAndTransferProduceCorrectBalancesAndLedgerEntries() throws Exception {
        String sourceAccountId = createAccount(ownerToken);
        String destinationAccountId = createAccount(ownerToken);

        deposit(ownerToken, sourceAccountId, "100.00");
        withdraw(ownerToken, sourceAccountId, "20.00");
        transfer(ownerToken, sourceAccountId, destinationAccountId, "30.00");

        mockMvc.perform(get("/api/v1/accounts/{id}/balance", sourceAccountId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00));

        mockMvc.perform(get("/api/v1/accounts/{id}/balance", destinationAccountId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(30.00));

        MvcResult entriesResult = mockMvc.perform(get("/api/v1/accounts/{id}/entries", sourceAccountId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn();

        // deposit (credit) + withdraw (debit) + transfer-out (debit) = 3 entries on the source account
        JsonNode entries = objectMapper.readTree(entriesResult.getResponse().getContentAsString()).get("content");
        assertThat(entries).hasSize(3);
    }

    @Test
    void withdrawMoreThanBalanceIsRejected() throws Exception {
        String accountId = createAccount(ownerToken);
        deposit(ownerToken, accountId, "10.00");

        String body = objectMapper.writeValueAsString(new WithdrawRequest(
                UUID.fromString(accountId), new BigDecimal("999.00"), null, null));

        mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void customerCannotOperateOnAnotherCustomersAccount() throws Exception {
        String accountId = createAccount(ownerToken);

        String body = objectMapper.writeValueAsString(new DepositRequest(
                UUID.fromString(accountId), new BigDecimal("10.00"), null, null));

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateReferenceIsRejected() throws Exception {
        String accountId = createAccount(ownerToken);
        String reference = "DUP-" + UUID.randomUUID();

        String body = objectMapper.writeValueAsString(new DepositRequest(
                UUID.fromString(accountId), new BigDecimal("5.00"), reference, null));

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    private String registerAndLogin(String email) throws Exception {
        RegisterRequest register = new RegisterRequest(email, "password123", "Test User");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return auth.accessToken();
    }

    private String createAccount(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAccountRequest("USD"))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void deposit(String token, String accountId, String amount) throws Exception {
        String body = objectMapper.writeValueAsString(new DepositRequest(
                UUID.fromString(accountId), new BigDecimal(amount), null, null));
        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private void withdraw(String token, String accountId, String amount) throws Exception {
        String body = objectMapper.writeValueAsString(new WithdrawRequest(
                UUID.fromString(accountId), new BigDecimal(amount), null, null));
        mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private void transfer(String token, String sourceId, String destinationId, String amount) throws Exception {
        String body = objectMapper.writeValueAsString(new TransferRequest(
                UUID.fromString(sourceId), UUID.fromString(destinationId), new BigDecimal(amount), null, null));
        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

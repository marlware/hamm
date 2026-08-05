package com.example.hamm.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BalanceResponse(UUID accountId, String accountNumber, BigDecimal balance, String currency, Instant asOf) {

    public static BalanceResponse from(Account account) {
        return new BalanceResponse(account.getId(), account.getAccountNumber(), account.getBalance(),
                account.getCurrency(), Instant.now());
    }
}

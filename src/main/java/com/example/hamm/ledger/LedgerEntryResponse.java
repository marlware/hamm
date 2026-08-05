package com.example.hamm.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID transactionId,
        UUID accountId,
        EntryType entryType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant createdAt
) {

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransaction().getId(),
                entry.getAccount().getId(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getBalanceAfter(),
                entry.getCreatedAt()
        );
    }
}

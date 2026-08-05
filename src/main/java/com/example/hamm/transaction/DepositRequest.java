package com.example.hamm.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequest(
        @NotNull UUID accountId,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than zero") BigDecimal amount,
        @Size(max = 64) String reference,
        @Size(max = 255) String description
) {
}

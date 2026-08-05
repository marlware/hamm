package com.example.hamm.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {

    public AccountNotFoundException(UUID accountId) {
        super(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found: " + accountId);
    }

    public AccountNotFoundException(String accountNumber) {
        super(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found: " + accountNumber);
    }
}

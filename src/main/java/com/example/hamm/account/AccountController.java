package com.example.hamm.account;

import com.example.hamm.ledger.LedgerEntryResponse;
import com.example.hamm.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Accounts")
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request, currentUser));
    }

    @GetMapping
    public ResponseEntity<Page<AccountResponse>> listAccounts(
            @AuthenticationPrincipal User currentUser, Pageable pageable) {
        return ResponseEntity.ok(accountService.listAccounts(currentUser, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(accountService.getAccount(id, currentUser));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable UUID id, @RequestBody UpdateAccountRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(accountService.updateAccount(id, request, currentUser));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(accountService.getBalance(id, currentUser));
    }

    @GetMapping("/{id}/entries")
    public ResponseEntity<Page<LedgerEntryResponse>> getEntries(
            @PathVariable UUID id, @AuthenticationPrincipal User currentUser, Pageable pageable) {
        return ResponseEntity.ok(accountService.getLedgerEntries(id, currentUser, pageable)
                .map(LedgerEntryResponse::from));
    }
}

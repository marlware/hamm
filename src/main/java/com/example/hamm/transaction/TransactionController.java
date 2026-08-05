package com.example.hamm.transaction;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Transactions")
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.deposit(request, currentUser));
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.withdraw(request, currentUser));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transfer(request, currentUser));
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> listTransactions(
            @AuthenticationPrincipal User currentUser, Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactions(currentUser, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(transactionService.getTransaction(id, currentUser));
    }
}

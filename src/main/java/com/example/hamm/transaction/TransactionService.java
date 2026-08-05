package com.example.hamm.transaction;

import com.example.hamm.account.Account;
import com.example.hamm.account.AccountRepository;
import com.example.hamm.account.AccountStatus;
import com.example.hamm.exception.AccountNotFoundException;
import com.example.hamm.exception.DuplicateReferenceException;
import com.example.hamm.exception.InsufficientFundsException;
import com.example.hamm.exception.UnauthorizedException;
import com.example.hamm.exception.ValidationException;
import com.example.hamm.ledger.LedgerService;
import com.example.hamm.user.Role;
import com.example.hamm.user.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final LedgerService ledgerService;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository,
            LedgerService ledgerService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest request, User currentUser) {
        String reference = resolveReference(request.reference());
        Account account = lockAccount(request.accountId());

        assertCanOperate(account, currentUser);
        assertActive(account);

        account.setBalance(account.getBalance().add(request.amount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .reference(reference)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .destinationAccount(account)
                .amount(request.amount())
                .currency(account.getCurrency())
                .description(request.description())
                .createdBy(currentUser)
                .build();
        transaction = transactionRepository.save(transaction);

        ledgerService.recordCredit(transaction, account, request.amount());

        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request, User currentUser) {
        String reference = resolveReference(request.reference());
        Account account = lockAccount(request.accountId());

        assertCanOperate(account, currentUser);
        assertActive(account);
        assertSufficientFunds(account, request.amount());

        account.setBalance(account.getBalance().subtract(request.amount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .reference(reference)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.COMPLETED)
                .sourceAccount(account)
                .amount(request.amount())
                .currency(account.getCurrency())
                .description(request.description())
                .createdBy(currentUser)
                .build();
        transaction = transactionRepository.save(transaction);

        ledgerService.recordDebit(transaction, account, request.amount());

        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request, User currentUser) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new ValidationException("Source and destination accounts must be different");
        }

        String reference = resolveReference(request.reference());
        Account[] locked = lockAccountsInOrder(request.sourceAccountId(), request.destinationAccountId());
        Account source = locked[0];
        Account destination = locked[1];

        assertCanOperate(source, currentUser);
        assertActive(source);
        assertActive(destination);
        assertSufficientFunds(source, request.amount());

        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new ValidationException("Source and destination accounts must share the same currency");
        }

        source.setBalance(source.getBalance().subtract(request.amount()));
        destination.setBalance(destination.getBalance().add(request.amount()));
        accountRepository.save(source);
        accountRepository.save(destination);

        Transaction transaction = Transaction.builder()
                .reference(reference)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .sourceAccount(source)
                .destinationAccount(destination)
                .amount(request.amount())
                .currency(source.getCurrency())
                .description(request.description())
                .createdBy(currentUser)
                .build();
        transaction = transactionRepository.save(transaction);

        ledgerService.recordDebit(transaction, source, request.amount());
        ledgerService.recordCredit(transaction, destination, request.amount());

        return TransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID id, User currentUser) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Transaction not found: " + id));

        boolean isParticipant = transaction.getCreatedBy().getId().equals(currentUser.getId())
                || (transaction.getSourceAccount() != null
                        && transaction.getSourceAccount().getOwner().getId().equals(currentUser.getId()))
                || (transaction.getDestinationAccount() != null
                        && transaction.getDestinationAccount().getOwner().getId().equals(currentUser.getId()));

        if (currentUser.getRole() == Role.CUSTOMER && !isParticipant) {
            throw new UnauthorizedException("You do not have access to this transaction");
        }

        return TransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(User currentUser, Pageable pageable) {
        Page<Transaction> transactions = currentUser.getRole() == Role.CUSTOMER
                ? transactionRepository.findVisibleToUser(currentUser.getId(), pageable)
                : transactionRepository.findAll(pageable);
        return transactions.map(TransactionResponse::from);
    }

    private Account lockAccount(UUID accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    /**
     * Locks both accounts in a globally consistent order (by id) regardless of
     * source/destination direction, to avoid lock-ordering deadlocks between
     * concurrent transfers that touch the same pair of accounts.
     */
    private Account[] lockAccountsInOrder(UUID sourceId, UUID destinationId) {
        List<UUID> ordered = sourceId.compareTo(destinationId) < 0
                ? List.of(sourceId, destinationId)
                : List.of(destinationId, sourceId);

        Account first = lockAccount(ordered.get(0));
        Account second = lockAccount(ordered.get(1));

        Account source = first.getId().equals(sourceId) ? first : second;
        Account destination = first.getId().equals(sourceId) ? second : first;
        return new Account[] {source, destination};
    }

    private void assertCanOperate(Account account, User currentUser) {
        boolean isOwner = account.getOwner().getId().equals(currentUser.getId());
        boolean isPrivileged = currentUser.getRole() == Role.ACCOUNTANT || currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isPrivileged) {
            throw new UnauthorizedException("You do not have access to this account");
        }
    }

    private void assertActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("Account " + account.getAccountNumber() + " is not active");
        }
    }

    private void assertSufficientFunds(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(account.getAccountNumber());
        }
    }

    private String resolveReference(String clientReference) {
        String reference = (clientReference == null || clientReference.isBlank())
                ? "TXN-" + UUID.randomUUID()
                : clientReference;

        if (transactionRepository.existsByReference(reference)) {
            throw new DuplicateReferenceException(reference);
        }
        return reference;
    }
}

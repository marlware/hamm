package com.example.hamm.account;

import com.example.hamm.exception.AccountNotFoundException;
import com.example.hamm.exception.UnauthorizedException;
import com.example.hamm.ledger.LedgerEntry;
import com.example.hamm.ledger.LedgerEntryRepository;
import com.example.hamm.user.Role;
import com.example.hamm.user.User;
import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountService(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, User owner) {
        Account account = Account.builder()
                .accountNumber(generateUniqueAccountNumber())
                .owner(owner)
                .currency(request.currency())
                .build();
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id, User currentUser) {
        Account account = findByIdOrThrow(id);
        assertCanView(account, currentUser);
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> listAccounts(User currentUser, Pageable pageable) {
        Page<Account> accounts = currentUser.getRole() == Role.CUSTOMER
                ? accountRepository.findByOwnerId(currentUser.getId(), pageable)
                : accountRepository.findAll(pageable);
        return accounts.map(AccountResponse::from);
    }

    @Transactional
    public AccountResponse updateAccount(UUID id, UpdateAccountRequest request, User currentUser) {
        Account account = findByIdOrThrow(id);
        assertCanView(account, currentUser);

        if (request.status() != null) {
            account.setStatus(request.status());
        }

        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID id, User currentUser) {
        Account account = findByIdOrThrow(id);
        assertCanView(account, currentUser);
        return BalanceResponse.from(account);
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntry> getLedgerEntries(UUID id, User currentUser, Pageable pageable) {
        Account account = findByIdOrThrow(id);
        assertCanView(account, currentUser);
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(id, pageable);
    }

    private Account findByIdOrThrow(UUID id) {
        return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    private void assertCanView(Account account, User currentUser) {
        boolean isOwner = account.getOwner().getId().equals(currentUser.getId());
        boolean isPrivileged = currentUser.getRole() != Role.CUSTOMER;

        if (!isOwner && !isPrivileged) {
            throw new UnauthorizedException("You do not have access to this account");
        }
    }

    private String generateUniqueAccountNumber() {
        String candidate;
        do {
            candidate = "ACC" + String.format("%012d", Math.abs(RANDOM.nextLong() % 1_000_000_000_000L));
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }
}

package com.example.hamm.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.hamm.exception.AccountNotFoundException;
import com.example.hamm.exception.UnauthorizedException;
import com.example.hamm.ledger.LedgerEntryRepository;
import com.example.hamm.user.Role;
import com.example.hamm.user.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private AccountService accountService;
    private User owner;
    private Account account;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, ledgerEntryRepository);

        owner = User.builder()
                .id(UUID.randomUUID())
                .email("owner@example.com")
                .fullName("Owner")
                .passwordHash("hashed")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC000000000001")
                .owner(owner)
                .currency("USD")
                .build();
    }

    @Test
    void ownerCanViewOwnAccount() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccount(account.getId(), owner);

        assertThat(response.id()).isEqualTo(account.getId());
    }

    @Test
    void otherCustomerCannotViewSomeoneElsesAccount() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        User stranger = User.builder()
                .id(UUID.randomUUID())
                .email("stranger@example.com")
                .fullName("Stranger")
                .passwordHash("hashed")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        assertThatThrownBy(() -> accountService.getAccount(account.getId(), stranger))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void auditorCanViewAnyAccount() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        User auditor = User.builder()
                .id(UUID.randomUUID())
                .email("auditor@example.com")
                .fullName("Auditor")
                .passwordHash("hashed")
                .role(Role.AUDITOR)
                .enabled(true)
                .build();

        AccountResponse response = accountService.getAccount(account.getId(), auditor);

        assertThat(response.id()).isEqualTo(account.getId());
    }

    @Test
    void missingAccountThrowsNotFound() {
        UUID missingId = UUID.randomUUID();
        when(accountRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(missingId, owner))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void createAccountGeneratesUniqueAccountNumber() {
        when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.createAccount(new CreateAccountRequest("USD"), owner);

        assertThat(response.accountNumber()).startsWith("ACC");
        assertThat(response.ownerId()).isEqualTo(owner.getId());
    }
}

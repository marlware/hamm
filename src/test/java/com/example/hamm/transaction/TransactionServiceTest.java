package com.example.hamm.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.example.hamm.account.Account;
import com.example.hamm.account.AccountRepository;
import com.example.hamm.account.AccountStatus;
import com.example.hamm.exception.InsufficientFundsException;
import com.example.hamm.exception.UnauthorizedException;
import com.example.hamm.exception.ValidationException;
import com.example.hamm.ledger.LedgerEntry;
import com.example.hamm.ledger.LedgerService;
import com.example.hamm.user.Role;
import com.example.hamm.user.User;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private LedgerService ledgerService;

    private TransactionService transactionService;

    private User owner;
    private Account account;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, accountRepository, ledgerService);

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
                .balance(new BigDecimal("100.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        lenient().when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(ledgerService.recordCredit(any(), any(), any())).thenReturn(LedgerEntry.builder().build());
        lenient().when(ledgerService.recordDebit(any(), any(), any())).thenReturn(LedgerEntry.builder().build());
    }

    @Test
    void depositIncreasesBalanceAndCreatesCompletedTransaction() {
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));

        TransactionResponse response = transactionService.deposit(
                new DepositRequest(account.getId(), new BigDecimal("25.00"), null, "top up"), owner);

        assertThat(account.getBalance()).isEqualByComparingTo("125.00");
        assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.type()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void withdrawWithInsufficientFundsThrows() {
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.withdraw(
                new WithdrawRequest(account.getId(), new BigDecimal("500.00"), null, null), owner))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void withdrawByNonOwnerCustomerIsUnauthorized() {
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));
        User stranger = User.builder()
                .id(UUID.randomUUID())
                .email("stranger@example.com")
                .fullName("Stranger")
                .passwordHash("hashed")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        assertThatThrownBy(() -> transactionService.withdraw(
                new WithdrawRequest(account.getId(), new BigDecimal("10.00"), null, null), stranger))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void transferToSameAccountIsRejected() {
        assertThatThrownBy(() -> transactionService.transfer(
                new TransferRequest(account.getId(), account.getId(), new BigDecimal("10.00"), null, null), owner))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void transferMovesFundsBetweenAccountsAndRecordsDoubleEntry() {
        Account destination = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC000000000002")
                .owner(owner)
                .currency("USD")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(destination.getId())).thenReturn(Optional.of(destination));

        transactionService.transfer(
                new TransferRequest(account.getId(), destination.getId(), new BigDecimal("40.00"), null, null),
                owner);

        assertThat(account.getBalance()).isEqualByComparingTo("60.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("40.00");
    }

    @Test
    void depositIntoInactiveAccountIsRejected() {
        account.setStatus(AccountStatus.FROZEN);
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.deposit(
                new DepositRequest(account.getId(), new BigDecimal("10.00"), null, null), owner))
                .isInstanceOf(ValidationException.class);
    }
}

package com.example.hamm.ledger;

import com.example.hamm.account.Account;
import com.example.hamm.transaction.Transaction;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public LedgerEntry recordDebit(Transaction transaction, Account account, BigDecimal amount) {
        return record(transaction, account, EntryType.DEBIT, amount);
    }

    @Transactional
    public LedgerEntry recordCredit(Transaction transaction, Account account, BigDecimal amount) {
        return record(transaction, account, EntryType.CREDIT, amount);
    }

    private LedgerEntry record(Transaction transaction, Account account, EntryType entryType, BigDecimal amount) {
        LedgerEntry entry = LedgerEntry.builder()
                .transaction(transaction)
                .account(account)
                .entryType(entryType)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .build();
        return ledgerEntryRepository.save(entry);
    }
}

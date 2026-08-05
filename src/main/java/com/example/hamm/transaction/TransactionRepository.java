package com.example.hamm.transaction;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    boolean existsByReference(String reference);

    Optional<Transaction> findByReference(String reference);

    @Query("""
            select t from Transaction t
            where t.createdBy.id = :userId
              or t.sourceAccount.owner.id = :userId
              or t.destinationAccount.owner.id = :userId
            order by t.createdAt desc
            """)
    Page<Transaction> findVisibleToUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            select t from Transaction t
            where t.sourceAccount.id = :accountId or t.destinationAccount.id = :accountId
            order by t.createdAt desc
            """)
    Page<Transaction> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);
}

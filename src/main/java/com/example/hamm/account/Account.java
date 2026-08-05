package com.example.hamm.account;

import com.example.hamm.common.Auditable;
import com.example.hamm.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_account_number", columnList = "account_number", unique = true)
        }
)
public class Account extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 34)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 3)
    private String currency;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;
}

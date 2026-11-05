package com.wallet.wallet_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(name = "uk_accounts_account_number", columnNames = "accountNumber"))
@Getter
@Setter
@NoArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String accountNumber;

    private String currency;

    private BigDecimal balance;

    private Long userId;

    @PrePersist
    void assignAccountNumber() {
        if(accountNumber == null) {
            accountNumber = "ACC-" + UUID.randomUUID();

        }
    }
}

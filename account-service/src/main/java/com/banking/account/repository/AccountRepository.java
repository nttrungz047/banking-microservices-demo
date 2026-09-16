package com.banking.account.repository;

import com.banking.account.entity.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserId(UUID userId);

    Optional<Account> findByUserIdAndCurrency(UUID userId, String currency);

    boolean existsByUserIdAndCurrency(UUID userId, String currency);
}

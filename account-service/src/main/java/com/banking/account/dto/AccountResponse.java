package com.banking.account.dto;

import com.banking.account.entity.AccountStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID userId,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        long version,
        Instant createdAt
) {
}

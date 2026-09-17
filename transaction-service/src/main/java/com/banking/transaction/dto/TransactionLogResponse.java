package com.banking.transaction.dto;

import com.banking.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionLogResponse(
        UUID id,
        String eventId,
        UUID paymentId,
        UUID accountId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant createdAt
) {
}

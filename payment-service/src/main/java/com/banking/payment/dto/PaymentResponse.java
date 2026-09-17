package com.banking.payment.dto;

import com.banking.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID fromAccountId, UUID toAccountId, BigDecimal amount, PaymentStatus status,
                              String failureReason, Instant createdAt) {
}

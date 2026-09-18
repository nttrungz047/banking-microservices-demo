package com.banking.notification.dto;

import com.banking.notification.entity.NotificationStatus;
import com.banking.notification.entity.NotificationType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String eventId,
        UUID paymentId,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String recipient,
        String subject,
        String content,
        NotificationType type,
        NotificationStatus status,
        Instant createdAt,
        Instant sentAt
) {
}

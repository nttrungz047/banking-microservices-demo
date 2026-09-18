package com.banking.notification.dto;

import com.banking.notification.entity.NotificationType;
import java.math.BigDecimal;
import java.util.UUID;

public record EmailNotificationPayload(
        String eventId,
        UUID paymentId,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String status,
        String reason,
        String recipient,
        String subject,
        String content,
        NotificationType type
) {
}

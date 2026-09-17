package com.banking.account.event;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountFailedEvent(String eventId, UUID paymentId, UUID accountId, BigDecimal amount, String reason) {
}

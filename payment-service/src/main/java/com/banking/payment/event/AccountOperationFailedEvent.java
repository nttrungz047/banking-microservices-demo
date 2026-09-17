package com.banking.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountOperationFailedEvent(String eventId, UUID paymentId, UUID accountId, BigDecimal amount,
                                          String reason) {
}

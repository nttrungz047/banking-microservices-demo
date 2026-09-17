package com.banking.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountOperationRequestedEvent(String eventId, UUID paymentId, UUID accountId, BigDecimal amount,
                                             boolean refund) {
}

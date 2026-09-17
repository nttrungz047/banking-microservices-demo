package com.banking.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResultEvent(String eventId, UUID paymentId, UUID fromAccountId, UUID toAccountId,
                                  BigDecimal amount, String reason) {
}

package com.banking.account.event;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountCreditedEvent(
        String eventId,
        UUID accountId,
        BigDecimal amount,
        BigDecimal balanceAfter
) {
}

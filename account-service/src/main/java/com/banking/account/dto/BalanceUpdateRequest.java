package com.banking.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BalanceUpdateRequest(
        @NotNull(message = "amount is required") @DecimalMin(value = "0.01", inclusive = false, message = "amount must be greater than zero") BigDecimal amount,
        String eventId
) {
}

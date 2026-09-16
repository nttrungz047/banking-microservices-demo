package com.banking.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(
        @NotNull(message = "userId is required") UUID userId,
        @NotNull(message = "initialBalance is required") @DecimalMin(value = "0.00", inclusive = true, message = "initialBalance must be zero or positive") BigDecimal initialBalance,
        @NotBlank(message = "currency is required") @Pattern(regexp = "[A-Za-z]{3}", message = "currency must be a 3-letter ISO code") String currency
) {
}

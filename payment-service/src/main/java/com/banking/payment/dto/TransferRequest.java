package com.banking.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(@NotNull UUID fromAccountId, @NotNull UUID toAccountId,
                              @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}

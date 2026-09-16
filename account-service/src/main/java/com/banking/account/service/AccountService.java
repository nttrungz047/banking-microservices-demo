package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccount(UUID accountId);

    List<AccountResponse> getAccountsByUser(UUID userId);

    AccountResponse debit(UUID accountId, BigDecimal amount);

    AccountResponse credit(UUID accountId, BigDecimal amount);
}

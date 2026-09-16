package com.banking.account.controller;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.BalanceUpdateRequest;
import com.banking.account.service.AccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountService accountService;

    @PutMapping("/{id}/debit")
    public AccountResponse debit(@PathVariable UUID id, @Valid @RequestBody BalanceUpdateRequest request) {
        return accountService.debit(id, request.amount());
    }

    @PutMapping("/{id}/credit")
    public AccountResponse credit(@PathVariable UUID id, @Valid @RequestBody BalanceUpdateRequest request) {
        return accountService.credit(id, request.amount());
    }
}

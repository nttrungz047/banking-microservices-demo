package com.banking.transaction.controller;

import com.banking.transaction.dto.TransactionLogResponse;
import com.banking.transaction.service.TransactionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public List<TransactionLogResponse> getTransactions(@RequestParam(required = false) UUID accountId) {
        return transactionService.getTransactions(accountId);
    }

    @GetMapping("/{id}")
    public TransactionLogResponse getTransactionById(@PathVariable UUID id) {
        return transactionService.getTransactionById(id);
    }
}

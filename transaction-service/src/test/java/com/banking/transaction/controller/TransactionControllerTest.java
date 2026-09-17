package com.banking.transaction.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.transaction.dto.TransactionLogResponse;
import com.banking.transaction.entity.TransactionType;
import com.banking.transaction.exception.GlobalExceptionHandler;
import com.banking.transaction.exception.TransactionNotFoundException;
import com.banking.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getTransactions_withAccountId_returnsList() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID logId = UUID.randomUUID();
        TransactionLogResponse response = new TransactionLogResponse(
                logId, "evt-1", null, accountId, TransactionType.DEBIT,
                new BigDecimal("100.00"), new BigDecimal("900.00"), "Debit", Instant.now()
        );

        when(transactionService.getTransactions(accountId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/transactions")
                        .param("accountId", accountId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(logId.toString()))
                .andExpect(jsonPath("$[0].accountId").value(accountId.toString()))
                .andExpect(jsonPath("$[0].type").value("DEBIT"))
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[0].balanceAfter").value(900.00));

        verify(transactionService).getTransactions(accountId);
    }

    @Test
    void getTransactionById_existingId_returnsTransaction() throws Exception {
        UUID logId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        TransactionLogResponse response = new TransactionLogResponse(
                logId, "evt-1", null, accountId, TransactionType.CREDIT,
                new BigDecimal("50.00"), new BigDecimal("950.00"), "Credit", Instant.now()
        );

        when(transactionService.getTransactionById(logId)).thenReturn(response);

        mockMvc.perform(get("/api/transactions/{id}", logId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(logId.toString()))
                .andExpect(jsonPath("$.type").value("CREDIT"));

        verify(transactionService).getTransactionById(logId);
    }

    @Test
    void getTransactionById_notFound_returns404() throws Exception {
        UUID logId = UUID.randomUUID();
        when(transactionService.getTransactionById(logId))
                .thenThrow(new TransactionNotFoundException("Transaction not found: " + logId));

        mockMvc.perform(get("/api/transactions/{id}", logId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found: " + logId));
    }
}

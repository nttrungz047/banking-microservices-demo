package com.banking.transaction.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.banking.transaction.entity.TransactionType;
import com.banking.transaction.event.AccountOperationCompletedEvent;
import com.banking.transaction.event.AccountOperationFailedEvent;
import com.banking.transaction.event.TransferResultEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaTransactionConsumerTest {

    @Mock
    private TransactionService transactionService;

    private ObjectMapper objectMapper;
    private KafkaTransactionConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new KafkaTransactionConsumer(transactionService, objectMapper);
    }

    @Test
    void onDebitCompleted_callsRecordAccountOperationCompleted() throws Exception {
        AccountOperationCompletedEvent event = new AccountOperationCompletedEvent(
                "evt-1", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), new BigDecimal("900.00")
        );
        String json = objectMapper.writeValueAsString(event);

        consumer.onDebitCompleted(json);

        verify(transactionService).recordAccountOperationCompleted(
                eq(event), eq(TransactionType.DEBIT), eq("account.debit-completed")
        );
    }

    @Test
    void onDebitFailed_callsRecordAccountOperationFailed() throws Exception {
        AccountOperationFailedEvent event = new AccountOperationFailedEvent(
                "evt-2", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "Insufficient funds"
        );
        String json = objectMapper.writeValueAsString(event);

        consumer.onDebitFailed(json);

        verify(transactionService).recordAccountOperationFailed(
                eq(event), eq(TransactionType.DEBIT_FAILED), eq("account.debit-failed")
        );
    }

    @Test
    void onCreditCompleted_callsRecordAccountOperationCompleted() throws Exception {
        AccountOperationCompletedEvent event = new AccountOperationCompletedEvent(
                "evt-3", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), new BigDecimal("1100.00")
        );
        String json = objectMapper.writeValueAsString(event);

        consumer.onCreditCompleted(json);

        verify(transactionService).recordAccountOperationCompleted(
                eq(event), eq(TransactionType.CREDIT), eq("account.credit-completed")
        );
    }

    @Test
    void onCreditFailed_callsRecordAccountOperationFailed() throws Exception {
        AccountOperationFailedEvent event = new AccountOperationFailedEvent(
                "evt-4", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "Account locked"
        );
        String json = objectMapper.writeValueAsString(event);

        consumer.onCreditFailed(json);

        verify(transactionService).recordAccountOperationFailed(
                eq(event), eq(TransactionType.CREDIT_FAILED), eq("account.credit-failed")
        );
    }

    @Test
    void onRefundCompleted_callsRecordAccountOperationCompleted() throws Exception {
        AccountOperationCompletedEvent event = new AccountOperationCompletedEvent(
                "evt-5", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), new BigDecimal("1000.00")
        );
        String json = objectMapper.writeValueAsString(event);

        consumer.onRefundCompleted(json);

        verify(transactionService).recordAccountOperationCompleted(
                eq(event), eq(TransactionType.REFUND), eq("account.refund-completed")
        );
    }

    @Test
    void onRefundFailed_callsRecordAccountOperationFailed() throws Exception {
        AccountOperationFailedEvent event = new AccountOperationFailedEvent(
                "evt-6", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "Refund error"
        );
        String json = objectMapper.writeValueAsString(event);

        consumer.onRefundFailed(json);

        verify(transactionService).recordAccountOperationFailed(
                eq(event), eq(TransactionType.REFUND_FAILED), eq("account.refund-failed")
        );
    }

    @Test
    void onTransferCompleted_callsRecordTransferResult() throws Exception {
        TransferResultEvent event = new TransferResultEvent(
                "evt-7", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), null
        );
        String json = objectMapper.writeValueAsString(event);

        consumer.onTransferCompleted(json);

        verify(transactionService).recordTransferResult(
                eq(event), eq(TransactionType.TRANSFER_COMPLETED), eq("payment.transfer-completed")
        );
    }

    @Test
    void onTransferFailed_callsRecordTransferResult() throws Exception {
        TransferResultEvent event = new TransferResultEvent(
                "evt-8", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "Timeout"
        );
        String json = objectMapper.writeValueAsString(event);

        consumer.onTransferFailed(json);

        verify(transactionService).recordTransferResult(
                eq(event), eq(TransactionType.TRANSFER_FAILED), eq("payment.transfer-failed")
        );
    }
}

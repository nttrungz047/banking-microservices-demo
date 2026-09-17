package com.banking.transaction.service;

import com.banking.transaction.entity.TransactionType;
import com.banking.transaction.event.AccountOperationCompletedEvent;
import com.banking.transaction.event.AccountOperationFailedEvent;
import com.banking.transaction.event.TransferResultEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTransactionConsumer {

    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "account.debit-completed")
    public void onDebitCompleted(String message) {
        AccountOperationCompletedEvent event = parse(message, AccountOperationCompletedEvent.class);
        transactionService.recordAccountOperationCompleted(event, TransactionType.DEBIT, "account.debit-completed");
    }

    @KafkaListener(topics = "account.debit-failed")
    public void onDebitFailed(String message) {
        AccountOperationFailedEvent event = parse(message, AccountOperationFailedEvent.class);
        transactionService.recordAccountOperationFailed(event, TransactionType.DEBIT_FAILED, "account.debit-failed");
    }

    @KafkaListener(topics = "account.credit-completed")
    public void onCreditCompleted(String message) {
        AccountOperationCompletedEvent event = parse(message, AccountOperationCompletedEvent.class);
        transactionService.recordAccountOperationCompleted(event, TransactionType.CREDIT, "account.credit-completed");
    }

    @KafkaListener(topics = "account.credit-failed")
    public void onCreditFailed(String message) {
        AccountOperationFailedEvent event = parse(message, AccountOperationFailedEvent.class);
        transactionService.recordAccountOperationFailed(event, TransactionType.CREDIT_FAILED, "account.credit-failed");
    }

    @KafkaListener(topics = "account.refund-completed")
    public void onRefundCompleted(String message) {
        AccountOperationCompletedEvent event = parse(message, AccountOperationCompletedEvent.class);
        transactionService.recordAccountOperationCompleted(event, TransactionType.REFUND, "account.refund-completed");
    }

    @KafkaListener(topics = "account.refund-failed")
    public void onRefundFailed(String message) {
        AccountOperationFailedEvent event = parse(message, AccountOperationFailedEvent.class);
        transactionService.recordAccountOperationFailed(event, TransactionType.REFUND_FAILED, "account.refund-failed");
    }

    @KafkaListener(topics = "payment.transfer-completed")
    public void onTransferCompleted(String message) {
        TransferResultEvent event = parse(message, TransferResultEvent.class);
        transactionService.recordTransferResult(event, TransactionType.TRANSFER_COMPLETED, "payment.transfer-completed");
    }

    @KafkaListener(topics = "payment.transfer-failed")
    public void onTransferFailed(String message) {
        TransferResultEvent event = parse(message, TransferResultEvent.class);
        transactionService.recordTransferResult(event, TransactionType.TRANSFER_FAILED, "payment.transfer-failed");
    }

    private <T> T parse(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to deserialize Kafka event: " + json, ex);
        }
    }
}

package com.banking.payment.service;

import com.banking.payment.entity.ProcessedEvent;
import com.banking.payment.entity.Payment;
import com.banking.payment.event.*;
import com.banking.payment.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPaymentConsumer {
    private final PaymentServiceImpl paymentService;
    private final ProcessedEventRepository processedEvents;
    private final KafkaPaymentProducer producer;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "account.debit-completed")
    public void debited(String json) {
        AccountOperationCompletedEvent e = read(json, AccountOperationCompletedEvent.class);
        once(e.eventId(), "account.debit-completed", () -> {
            Payment payment = paymentService.find(e.paymentId());
            producer.credit(payment.getId(), payment.getToAccountId(), payment.getAmount());
        });
    }

    @KafkaListener(topics = "account.debit-failed")
    public void debitFailed(String json) {
        AccountOperationFailedEvent e = read(json, AccountOperationFailedEvent.class);
        once(e.eventId(), "account.debit-failed", () -> paymentService.fail(e.paymentId(), e.reason()));
    }

    @KafkaListener(topics = "account.credit-completed")
    public void credited(String json) {
        AccountOperationCompletedEvent e = read(json, AccountOperationCompletedEvent.class);
        once(e.eventId(), "account.credit-completed", () -> paymentService.complete(e.paymentId()));
    }

    @KafkaListener(topics = "account.credit-failed")
    public void creditFailed(String json) {
        AccountOperationFailedEvent e = read(json, AccountOperationFailedEvent.class);
        once(e.eventId(), "account.credit-failed", () -> paymentService.requestRefund(e.paymentId()));
    }

    @KafkaListener(topics = "account.refund-completed")
    public void refunded(String json) {
        AccountOperationCompletedEvent e = read(json, AccountOperationCompletedEvent.class);
        once(e.eventId(), "account.refund-completed", () -> paymentService.fail(e.paymentId(), "Credit failed; source account refunded"));
    }

    @KafkaListener(topics = "account.refund-failed")
    public void refundFailed(String json) {
        AccountOperationFailedEvent e = read(json, AccountOperationFailedEvent.class);
        once(e.eventId(), "account.refund-failed", () -> paymentService.fail(e.paymentId(), "Credit and refund failed: " + e.reason()));
    }

    private void once(String id, String type, Runnable action) {
        if (processedEvents.existsById(id)) {
            log.info("Skipping duplicate {}", id);
            return;
        }
        action.run();
        processedEvents.save(new ProcessedEvent(id, type, Instant.now()));
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid Kafka event payload", ex);
        }
    }
}

package com.banking.payment.service;

import com.banking.payment.event.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPaymentProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void debit(UUID paymentId, UUID accountId, BigDecimal amount) {
        publish("account.debit-requested", new AccountOperationRequestedEvent(id(), paymentId, accountId, amount, false));
    }

    public void credit(UUID paymentId, UUID accountId, BigDecimal amount) {
        publish("account.credit-requested", new AccountOperationRequestedEvent(id(), paymentId, accountId, amount, false));
    }

    public void refund(UUID paymentId, UUID accountId, BigDecimal amount) {
        publish("account.refund-requested", new AccountOperationRequestedEvent(id(), paymentId, accountId, amount, true));
    }

    public void completed(UUID paymentId, UUID from, UUID to, BigDecimal amount) {
        publish("payment.transfer-completed", new TransferResultEvent(id(), paymentId, from, to, amount, null));
    }

    public void failed(UUID paymentId, UUID from, UUID to, BigDecimal amount, String reason) {
        publish("payment.transfer-failed", new TransferResultEvent(id(), paymentId, from, to, amount, reason));
    }

    private String id() {
        return UUID.randomUUID().toString();
    }

    private void publish(String topic, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event instanceof AccountOperationRequestedEvent e ? e.eventId() : ((TransferResultEvent) event).eventId(), json);
            log.info("Published {}", topic);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize Kafka event", ex);
        }
    }
}

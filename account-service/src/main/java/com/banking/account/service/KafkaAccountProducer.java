package com.banking.account.service;

import com.banking.account.event.AccountCreditedEvent;
import com.banking.account.event.AccountDebitedEvent;
import com.banking.account.event.AccountFailedEvent;
import java.math.BigDecimal;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAccountProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishDebitCompleted(String eventId, UUID paymentId, UUID accountId, BigDecimal amount, BigDecimal balanceAfter) {
        publish("account.debit-completed", eventId, new AccountDebitedEvent(eventId, paymentId, accountId, amount, balanceAfter));
        log.info("Published debit completed event {} for account {}", eventId, accountId);
    }

    public void publishCreditCompleted(String eventId, UUID paymentId, UUID accountId, BigDecimal amount, BigDecimal balanceAfter, boolean refund) {
        publish(refund ? "account.refund-completed" : "account.credit-completed", eventId,
                new AccountCreditedEvent(eventId, paymentId, accountId, amount, balanceAfter));
        log.info("Published credit completed event {} for account {}", eventId, accountId);
    }

    public void publishDebitFailed(String eventId, UUID paymentId, UUID accountId, BigDecimal amount, String reason) {
        publish("account.debit-failed", eventId, new AccountFailedEvent(eventId, paymentId, accountId, amount, reason));
    }

    public void publishCreditFailed(String eventId, UUID paymentId, UUID accountId, BigDecimal amount, String reason, boolean refund) {
        publish(refund ? "account.refund-failed" : "account.credit-failed", eventId,
                new AccountFailedEvent(eventId, paymentId, accountId, amount, reason));
    }

    private void publish(String topic, String eventId, Object event) {
        try {
            kafkaTemplate.send(topic, eventId, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize Kafka event " + eventId, ex);
        }
    }
}

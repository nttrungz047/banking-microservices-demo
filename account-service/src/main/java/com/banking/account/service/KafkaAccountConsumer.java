package com.banking.account.service;

import com.banking.account.entity.ProcessedEvent;
import com.banking.account.event.AccountCreditRequestedEvent;
import com.banking.account.event.AccountDebitRequestedEvent;
import com.banking.account.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAccountConsumer {

    private final AccountService accountService;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaAccountProducer kafkaAccountProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "account.debit-requested", groupId = "account-service-group")
    public void onDebitRequested(@Payload String payload) {
        AccountDebitRequestedEvent event = read(payload, AccountDebitRequestedEvent.class);
        if (event == null || event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("Debit event must include a non-empty eventId");
        }
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Skipping already processed debit event {}", event.eventId());
            return;
        }

        try {
            var account = accountService.debit(event.accountId(), event.amount());
            kafkaAccountProducer.publishDebitCompleted(UUID.randomUUID().toString(), event.paymentId(), event.accountId(), event.amount(), account.balance());
        } catch (RuntimeException ex) {
            kafkaAccountProducer.publishDebitFailed(UUID.randomUUID().toString(), event.paymentId(), event.accountId(), event.amount(), ex.getMessage());
        }
        processedEventRepository.save(new ProcessedEvent(event.eventId(), "account.debit-requested", Instant.now()));
        log.info("Processed debit event {}", event.eventId());
    }

    @KafkaListener(topics = {"account.credit-requested", "account.refund-requested"}, groupId = "account-service-group")
    public void onCreditRequested(@Payload String payload) {
        AccountCreditRequestedEvent event = read(payload, AccountCreditRequestedEvent.class);
        if (event == null || event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("Credit event must include a non-empty eventId");
        }
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Skipping already processed credit event {}", event.eventId());
            return;
        }

        try {
            var account = accountService.credit(event.accountId(), event.amount());
            kafkaAccountProducer.publishCreditCompleted(UUID.randomUUID().toString(), event.paymentId(), event.accountId(), event.amount(), account.balance(), event.refund());
        } catch (RuntimeException ex) {
            kafkaAccountProducer.publishCreditFailed(UUID.randomUUID().toString(), event.paymentId(), event.accountId(), event.amount(), ex.getMessage(), event.refund());
        }
        processedEventRepository.save(new ProcessedEvent(event.eventId(), "account.credit-requested", Instant.now()));
        log.info("Processed credit event {}", event.eventId());
    }

    private <T> T read(String payload, Class<T> eventType) {
        try {
            return objectMapper.readValue(payload, eventType);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid Kafka event payload", ex);
        }
    }
}

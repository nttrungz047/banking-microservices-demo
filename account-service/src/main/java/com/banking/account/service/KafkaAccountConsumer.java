package com.banking.account.service;

import com.banking.account.entity.ProcessedEvent;
import com.banking.account.event.AccountCreditRequestedEvent;
import com.banking.account.event.AccountDebitRequestedEvent;
import com.banking.account.repository.ProcessedEventRepository;
import java.time.Instant;
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

    @KafkaListener(topics = "account.debit-requested", groupId = "account-service-group")
    public void onDebitRequested(@Payload AccountDebitRequestedEvent event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("Debit event must include a non-empty eventId");
        }
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Skipping already processed debit event {}", event.eventId());
            return;
        }

        accountService.debit(event.accountId(), event.amount());
        processedEventRepository.save(new ProcessedEvent(event.eventId(), "account.debit-requested", Instant.now()));
        log.info("Processed debit event {}", event.eventId());
    }

    @KafkaListener(topics = "account.credit-requested", groupId = "account-service-group")
    public void onCreditRequested(@Payload AccountCreditRequestedEvent event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("Credit event must include a non-empty eventId");
        }
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Skipping already processed credit event {}", event.eventId());
            return;
        }

        accountService.credit(event.accountId(), event.amount());
        processedEventRepository.save(new ProcessedEvent(event.eventId(), "account.credit-requested", Instant.now()));
        log.info("Processed credit event {}", event.eventId());
    }
}

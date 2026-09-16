package com.banking.account.service;

import com.banking.account.event.AccountCreditedEvent;
import com.banking.account.event.AccountDebitedEvent;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAccountProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishDebitCompleted(String eventId, UUID accountId, BigDecimal amount, BigDecimal balanceAfter) {
        kafkaTemplate.send("account.debit-completed", eventId,
                new AccountDebitedEvent(eventId, accountId, amount, balanceAfter));
        log.info("Published debit completed event {} for account {}", eventId, accountId);
    }

    public void publishCreditCompleted(String eventId, UUID accountId, BigDecimal amount, BigDecimal balanceAfter) {
        kafkaTemplate.send("account.credit-completed", eventId,
                new AccountCreditedEvent(eventId, accountId, amount, balanceAfter));
        log.info("Published credit completed event {} for account {}", eventId, accountId);
    }
}

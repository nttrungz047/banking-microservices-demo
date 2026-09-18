package com.banking.notification.service;

import com.banking.notification.event.TransferResultEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaNotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.transfer-completed")
    public void onTransferCompleted(String message) {
        TransferResultEvent event = parse(message, TransferResultEvent.class);
        notificationService.processTransferCompleted(event, "payment.transfer-completed");
    }

    @KafkaListener(topics = "payment.transfer-failed")
    public void onTransferFailed(String message) {
        TransferResultEvent event = parse(message, TransferResultEvent.class);
        notificationService.processTransferFailed(event, "payment.transfer-failed");
    }

    private <T> T parse(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to deserialize Kafka event: " + json, ex);
        }
    }
}

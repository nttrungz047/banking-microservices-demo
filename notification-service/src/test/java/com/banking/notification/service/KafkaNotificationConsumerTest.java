package com.banking.notification.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.banking.notification.event.TransferResultEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaNotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    private ObjectMapper objectMapper;
    private KafkaNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new KafkaNotificationConsumer(notificationService, objectMapper);
    }

    @Test
    void onTransferCompleted_callsProcessTransferCompleted() throws Exception {
        TransferResultEvent event = new TransferResultEvent(
                "evt-tc-1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), null
        );
        String json = objectMapper.writeValueAsString(event);

        consumer.onTransferCompleted(json);

        verify(notificationService).processTransferCompleted(
                eq(event), eq("payment.transfer-completed")
        );
    }

    @Test
    void onTransferFailed_callsProcessTransferFailed() throws Exception {
        TransferResultEvent event = new TransferResultEvent(
                "evt-tf-1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "Insufficient balance"
        );
        String json = objectMapper.writeValueAsString(event);

        consumer.onTransferFailed(json);

        verify(notificationService).processTransferFailed(
                eq(event), eq("payment.transfer-failed")
        );
    }
}

package com.banking.notification.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.banking.notification.NotificationServiceApplication;
import com.banking.notification.dto.NotificationResponse;
import com.banking.notification.entity.Notification;
import com.banking.notification.entity.NotificationStatus;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.event.TransferResultEvent;
import com.banking.notification.repository.NotificationRepository;
import com.banking.notification.repository.ProcessedEventRepository;
import com.banking.notification.service.KafkaNotificationConsumer;
import com.banking.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = NotificationServiceApplication.class)
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    @Autowired
    private KafkaNotificationConsumer consumer;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void whenTransferCompletedEventReceived_notificationCreatedAndEmailSent() throws Exception {
        String eventId = "integ-evt-comp-" + UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("750.50");

        TransferResultEvent event = new TransferResultEvent(
                eventId, paymentId, fromAccountId, toAccountId, amount, null
        );
        String json = objectMapper.writeValueAsString(event);

        consumer.onTransferCompleted(json);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(eventId, notification.getEventId());
        assertEquals(paymentId, notification.getPaymentId());
        assertEquals(fromAccountId, notification.getFromAccountId());
        assertEquals(toAccountId, notification.getToAccountId());
        assertEquals(amount, notification.getAmount());
        assertEquals(NotificationType.TRANSFER_COMPLETED, notification.getType());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals("account-" + fromAccountId + "@banking.local", notification.getRecipient());
        assertTrue(notification.getContent().contains("750.50"));

        assertTrue(processedEventRepository.existsById(eventId));
        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));

        List<NotificationResponse> responses = notificationService.getNotifications(paymentId);
        assertEquals(1, responses.size());
        assertEquals(paymentId, responses.get(0).paymentId());
    }

    @Test
    void whenTransferFailedEventReceived_failureNotificationCreatedAndEmailSent() throws Exception {
        String eventId = "integ-evt-fail-" + UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("300.00");
        String reason = "Account debit limit exceeded";

        TransferResultEvent event = new TransferResultEvent(
                eventId, paymentId, fromAccountId, toAccountId, amount, reason
        );
        String json = objectMapper.writeValueAsString(event);

        consumer.onTransferFailed(json);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(eventId, notification.getEventId());
        assertEquals(paymentId, notification.getPaymentId());
        assertEquals(NotificationType.TRANSFER_FAILED, notification.getType());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertTrue(notification.getContent().contains(reason));

        assertTrue(processedEventRepository.existsById(eventId));
        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void whenDuplicateEventReplayed_idempotencyPreventsDuplicateNotifications() throws Exception {
        String eventId = "integ-evt-replay-" + UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        TransferResultEvent event = new TransferResultEvent(
                eventId, paymentId, fromAccountId, toAccountId, amount, null
        );
        String json = objectMapper.writeValueAsString(event);

        // First delivery
        consumer.onTransferCompleted(json);
        assertEquals(1, notificationRepository.count());
        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));

        // Replayed delivery
        consumer.onTransferCompleted(json);
        assertEquals(1, notificationRepository.count());
        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}

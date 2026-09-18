package com.banking.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.notification.dto.EmailNotificationPayload;
import com.banking.notification.dto.NotificationResponse;
import com.banking.notification.entity.Notification;
import com.banking.notification.entity.NotificationStatus;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.entity.ProcessedEvent;
import com.banking.notification.event.TransferResultEvent;
import com.banking.notification.exception.NotificationNotFoundException;
import com.banking.notification.mapper.NotificationMapper;
import com.banking.notification.repository.NotificationRepository;
import com.banking.notification.repository.ProcessedEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void processTransferCompleted_success() {
        String eventId = "evt-comp-1";
        UUID paymentId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");
        TransferResultEvent event = new TransferResultEvent(
                eventId, paymentId, fromAccountId, toAccountId, amount, null
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        notificationService.processTransferCompleted(event, "payment.transfer-completed");

        ArgumentCaptor<EmailNotificationPayload> emailCaptor = ArgumentCaptor.forClass(EmailNotificationPayload.class);
        verify(emailService).sendEmail(emailCaptor.capture());
        EmailNotificationPayload capturedEmail = emailCaptor.getValue();
        assertEquals(eventId, capturedEmail.eventId());
        assertEquals(paymentId, capturedEmail.paymentId());
        assertEquals("COMPLETED", capturedEmail.status());
        assertEquals("account-" + fromAccountId + "@banking.local", capturedEmail.recipient());
        assertNull(capturedEmail.reason());

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        Notification savedNotification = notifCaptor.getValue();
        assertEquals(eventId, savedNotification.getEventId());
        assertEquals(paymentId, savedNotification.getPaymentId());
        assertEquals(fromAccountId, savedNotification.getFromAccountId());
        assertEquals(toAccountId, savedNotification.getToAccountId());
        assertEquals(amount, savedNotification.getAmount());
        assertEquals(NotificationType.TRANSFER_COMPLETED, savedNotification.getType());
        assertEquals(NotificationStatus.SENT, savedNotification.getStatus());

        ArgumentCaptor<ProcessedEvent> eventCaptor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(eventCaptor.capture());
        assertEquals(eventId, eventCaptor.getValue().getEventId());
        assertEquals("payment.transfer-completed", eventCaptor.getValue().getEventType());
    }

    @Test
    void processTransferCompleted_duplicateEvent_isSkipped() {
        String eventId = "evt-comp-dup";
        TransferResultEvent event = new TransferResultEvent(
                eventId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), null
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        notificationService.processTransferCompleted(event, "payment.transfer-completed");

        verify(emailService, never()).sendEmail(any());
        verify(notificationRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void processTransferFailed_success() {
        String eventId = "evt-fail-1";
        UUID paymentId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("250.00");
        String reason = "Insufficient funds in source account";
        TransferResultEvent event = new TransferResultEvent(
                eventId, paymentId, fromAccountId, toAccountId, amount, reason
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        notificationService.processTransferFailed(event, "payment.transfer-failed");

        ArgumentCaptor<EmailNotificationPayload> emailCaptor = ArgumentCaptor.forClass(EmailNotificationPayload.class);
        verify(emailService).sendEmail(emailCaptor.capture());
        EmailNotificationPayload capturedEmail = emailCaptor.getValue();
        assertEquals(eventId, capturedEmail.eventId());
        assertEquals(paymentId, capturedEmail.paymentId());
        assertEquals("FAILED", capturedEmail.status());
        assertEquals(reason, capturedEmail.reason());
        assertEquals(NotificationType.TRANSFER_FAILED, capturedEmail.type());

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        Notification savedNotification = notifCaptor.getValue();
        assertEquals(eventId, savedNotification.getEventId());
        assertEquals(NotificationType.TRANSFER_FAILED, savedNotification.getType());
        assertEquals(NotificationStatus.SENT, savedNotification.getStatus());

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void processTransferFailed_duplicateEvent_isSkipped() {
        String eventId = "evt-fail-dup";
        TransferResultEvent event = new TransferResultEvent(
                eventId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "Timeout"
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        notificationService.processTransferFailed(event, "payment.transfer-failed");

        verify(emailService, never()).sendEmail(any());
        verify(notificationRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void getNotifications_withPaymentId_returnsFilteredList() {
        UUID paymentId = UUID.randomUUID();
        List<Notification> list = List.of(
                Notification.builder()
                        .id(UUID.randomUUID())
                        .paymentId(paymentId)
                        .type(NotificationType.TRANSFER_COMPLETED)
                        .build()
        );
        List<NotificationResponse> responses = List.of(
                new NotificationResponse(
                        list.get(0).getId(), "evt-1", paymentId, UUID.randomUUID(), UUID.randomUUID(),
                        new BigDecimal("50.00"), "user@mail.com", "Subject", "Body",
                        NotificationType.TRANSFER_COMPLETED, NotificationStatus.SENT,
                        Instant.now(), Instant.now()
                )
        );

        when(notificationRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId)).thenReturn(list);
        when(notificationMapper.toResponseList(list)).thenReturn(responses);

        List<NotificationResponse> result = notificationService.getNotifications(paymentId);

        assertEquals(1, result.size());
        assertEquals(paymentId, result.get(0).paymentId());
        verify(notificationRepository).findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }

    @Test
    void getNotifications_withoutPaymentId_returnsAll() {
        List<Notification> list = List.of(
                Notification.builder().id(UUID.randomUUID()).build()
        );
        List<NotificationResponse> responses = List.of(
                new NotificationResponse(
                        list.get(0).getId(), "evt-1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        new BigDecimal("50.00"), "user@mail.com", "Subject", "Body",
                        NotificationType.TRANSFER_COMPLETED, NotificationStatus.SENT,
                        Instant.now(), Instant.now()
                )
        );

        when(notificationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(list);
        when(notificationMapper.toResponseList(list)).thenReturn(responses);

        List<NotificationResponse> result = notificationService.getNotifications(null);

        assertEquals(1, result.size());
        verify(notificationRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getNotificationById_found_returnsResponse() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder().id(id).build();
        NotificationResponse response = new NotificationResponse(
                id, "evt-1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50.00"), "user@mail.com", "Subject", "Body",
                NotificationType.TRANSFER_COMPLETED, NotificationStatus.SENT,
                Instant.now(), Instant.now()
        );

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        NotificationResponse result = notificationService.getNotificationById(id);

        assertNotNull(result);
        assertEquals(id, result.id());
    }

    @Test
    void getNotificationById_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.getNotificationById(id));
    }
}

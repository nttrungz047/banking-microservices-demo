package com.banking.notification.service;

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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final NotificationMapper notificationMapper;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UUID paymentId) {
        List<Notification> notifications;
        if (paymentId != null) {
            notifications = notificationRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);
        } else {
            notifications = notificationRepository.findAllByOrderByCreatedAtDesc();
        }
        return notificationMapper.toResponseList(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + id));
        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public void processTransferCompleted(TransferResultEvent event, String topic) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Skipping duplicate event {} for topic {}", event.eventId(), topic);
            return;
        }

        String recipient = resolveRecipient(event.fromAccountId());
        String subject = "Transfer Successful - Payment " + event.paymentId();
        String content = String.format(
                "Transfer of %s from account %s to account %s has completed successfully. Payment ID: %s",
                event.amount(),
                event.fromAccountId(),
                event.toAccountId(),
                event.paymentId()
        );

        EmailNotificationPayload payload = new EmailNotificationPayload(
                event.eventId(),
                event.paymentId(),
                event.fromAccountId(),
                event.toAccountId(),
                event.amount(),
                "COMPLETED",
                null,
                recipient,
                subject,
                content,
                NotificationType.TRANSFER_COMPLETED
        );

        emailService.sendEmail(payload);

        Instant now = Instant.now();
        Notification notification = Notification.builder()
                .eventId(event.eventId())
                .paymentId(event.paymentId())
                .fromAccountId(event.fromAccountId())
                .toAccountId(event.toAccountId())
                .amount(event.amount())
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .type(NotificationType.TRANSFER_COMPLETED)
                .status(NotificationStatus.SENT)
                .createdAt(now)
                .sentAt(now)
                .build();

        notificationRepository.save(notification);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), topic, now));
        log.info("Processed transfer completed notification {} for event {} on topic {}",
                notification.getId(), event.eventId(), topic);
    }

    @Override
    @Transactional
    public void processTransferFailed(TransferResultEvent event, String topic) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Skipping duplicate event {} for topic {}", event.eventId(), topic);
            return;
        }

        String recipient = resolveRecipient(event.fromAccountId());
        String subject = "Transfer Failed - Payment " + event.paymentId();
        String content = String.format(
                "Transfer of %s from account %s to account %s has failed. Reason: %s. Payment ID: %s",
                event.amount(),
                event.fromAccountId(),
                event.toAccountId(),
                event.reason(),
                event.paymentId()
        );

        EmailNotificationPayload payload = new EmailNotificationPayload(
                event.eventId(),
                event.paymentId(),
                event.fromAccountId(),
                event.toAccountId(),
                event.amount(),
                "FAILED",
                event.reason(),
                recipient,
                subject,
                content,
                NotificationType.TRANSFER_FAILED
        );

        emailService.sendEmail(payload);

        Instant now = Instant.now();
        Notification notification = Notification.builder()
                .eventId(event.eventId())
                .paymentId(event.paymentId())
                .fromAccountId(event.fromAccountId())
                .toAccountId(event.toAccountId())
                .amount(event.amount())
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .type(NotificationType.TRANSFER_FAILED)
                .status(NotificationStatus.SENT)
                .createdAt(now)
                .sentAt(now)
                .build();

        notificationRepository.save(notification);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), topic, now));
        log.info("Processed transfer failed notification {} for event {} on topic {}",
                notification.getId(), event.eventId(), topic);
    }

    private String resolveRecipient(UUID accountId) {
        if (accountId != null) {
            return "account-" + accountId + "@banking.local";
        }
        return "customer@banking.local";
    }
}

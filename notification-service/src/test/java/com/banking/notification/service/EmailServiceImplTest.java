package com.banking.notification.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.banking.notification.config.NotificationMailConfig;
import com.banking.notification.dto.EmailNotificationPayload;
import com.banking.notification.entity.NotificationType;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    private NotificationMailConfig mailConfig;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        mailConfig = new NotificationMailConfig();
        mailConfig.setFrom("noreply@banking.local");
        mailConfig.setEnabled(true);
        emailService = new EmailServiceImpl(javaMailSender, mailConfig);
    }

    @Test
    void sendEmail_withEnabledMail_sendsMailMessage() {
        EmailNotificationPayload payload = new EmailNotificationPayload(
                "evt-email-1",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                "COMPLETED",
                null,
                "user@example.com",
                "Transfer Successful",
                "Your transfer has completed.",
                NotificationType.TRANSFER_COMPLETED
        );

        emailService.sendEmail(payload);

        ArgumentCaptor<SimpleMailMessage> msgCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(msgCaptor.capture());
        SimpleMailMessage sentMsg = msgCaptor.getValue();
        assertEquals("noreply@banking.local", sentMsg.getFrom());
        assertNotNull(sentMsg.getTo());
        assertEquals("user@example.com", sentMsg.getTo()[0]);
        assertEquals("Transfer Successful", sentMsg.getSubject());
        assertEquals("Your transfer has completed.", sentMsg.getText());
    }

    @Test
    void sendEmail_whenMailDisabled_doesNotSendSmtp() {
        mailConfig.setEnabled(false);

        EmailNotificationPayload payload = new EmailNotificationPayload(
                "evt-email-2",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                "COMPLETED",
                null,
                "user@example.com",
                "Transfer Successful",
                "Your transfer has completed.",
                NotificationType.TRANSFER_COMPLETED
        );

        emailService.sendEmail(payload);

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_whenMailExceptionOccurs_catchesGracefully() {
        doThrow(new MailSendException("SMTP connection refused"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));

        EmailNotificationPayload payload = new EmailNotificationPayload(
                "evt-email-3",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                "FAILED",
                "Insufficient balance",
                "user@example.com",
                "Transfer Failed",
                "Your transfer has failed.",
                NotificationType.TRANSFER_FAILED
        );

        assertDoesNotThrow(() -> emailService.sendEmail(payload));
    }
}

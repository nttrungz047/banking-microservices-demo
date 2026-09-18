package com.banking.notification.service;

import com.banking.notification.config.NotificationMailConfig;
import com.banking.notification.dto.EmailNotificationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final NotificationMailConfig mailConfig;

    @Override
    public void sendEmail(EmailNotificationPayload payload) {
        log.info(
                "Simulated Email Notification: status={}, eventId={}, paymentId={}, fromAccount={}, toAccount={}, amount={}, recipient={}, subject={}, reason={}",
                payload.status(),
                payload.eventId(),
                payload.paymentId(),
                payload.fromAccountId(),
                payload.toAccountId(),
                payload.amount(),
                payload.recipient(),
                payload.subject(),
                payload.reason()
        );

        if (mailConfig.isEnabled() && javaMailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailConfig.getFrom());
                message.setTo(payload.recipient());
                message.setSubject(payload.subject());
                message.setText(payload.content());
                javaMailSender.send(message);
                log.info("Email successfully sent via SMTP to {}", payload.recipient());
            } catch (Exception ex) {
                log.warn("Failed to deliver email via SMTP to {}: {}", payload.recipient(), ex.getMessage());
            }
        }
    }
}

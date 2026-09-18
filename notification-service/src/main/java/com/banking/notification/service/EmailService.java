package com.banking.notification.service;

import com.banking.notification.dto.EmailNotificationPayload;

public interface EmailService {

    void sendEmail(EmailNotificationPayload payload);
}

package com.banking.notification.service;

import com.banking.notification.dto.NotificationResponse;
import com.banking.notification.event.TransferResultEvent;
import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void processTransferCompleted(TransferResultEvent event, String topic);

    void processTransferFailed(TransferResultEvent event, String topic);

    List<NotificationResponse> getNotifications(UUID paymentId);

    NotificationResponse getNotificationById(UUID id);
}

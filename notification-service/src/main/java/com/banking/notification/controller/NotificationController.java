package com.banking.notification.controller;

import com.banking.notification.dto.NotificationResponse;
import com.banking.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getNotifications(@RequestParam(required = false) UUID paymentId) {
        return notificationService.getNotifications(paymentId);
    }

    @GetMapping("/{id}")
    public NotificationResponse getNotificationById(@PathVariable UUID id) {
        return notificationService.getNotificationById(id);
    }
}

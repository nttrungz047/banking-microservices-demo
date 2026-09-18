package com.banking.notification.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.notification.dto.NotificationResponse;
import com.banking.notification.entity.NotificationStatus;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.exception.GlobalExceptionHandler;
import com.banking.notification.exception.NotificationNotFoundException;
import com.banking.notification.service.NotificationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getNotifications_withPaymentId_returnsList() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        NotificationResponse response = new NotificationResponse(
                notifId,
                "evt-101",
                paymentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "recipient@banking.local",
                "Transfer Successful",
                "Content body",
                NotificationType.TRANSFER_COMPLETED,
                NotificationStatus.SENT,
                Instant.now(),
                Instant.now()
        );

        when(notificationService.getNotifications(paymentId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/notifications")
                        .param("paymentId", paymentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(notifId.toString()))
                .andExpect(jsonPath("$[0].paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$[0].type").value("TRANSFER_COMPLETED"))
                .andExpect(jsonPath("$[0].status").value("SENT"))
                .andExpect(jsonPath("$[0].recipient").value("recipient@banking.local"));

        verify(notificationService).getNotifications(paymentId);
    }

    @Test
    void getNotificationById_existingId_returnsNotification() throws Exception {
        UUID notifId = UUID.randomUUID();
        NotificationResponse response = new NotificationResponse(
                notifId,
                "evt-102",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("50.00"),
                "recipient@banking.local",
                "Transfer Failed",
                "Content body",
                NotificationType.TRANSFER_FAILED,
                NotificationStatus.SENT,
                Instant.now(),
                Instant.now()
        );

        when(notificationService.getNotificationById(notifId)).thenReturn(response);

        mockMvc.perform(get("/api/notifications/{id}", notifId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notifId.toString()))
                .andExpect(jsonPath("$.type").value("TRANSFER_FAILED"))
                .andExpect(jsonPath("$.status").value("SENT"));

        verify(notificationService).getNotificationById(notifId);
    }

    @Test
    void getNotificationById_notFound_returns404() throws Exception {
        UUID notifId = UUID.randomUUID();
        when(notificationService.getNotificationById(notifId))
                .thenThrow(new NotificationNotFoundException("Notification not found: " + notifId));

        mockMvc.perform(get("/api/notifications/{id}", notifId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found: " + notifId));
    }
}

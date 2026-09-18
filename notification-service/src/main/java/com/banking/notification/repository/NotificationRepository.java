package com.banking.notification.repository;

import com.banking.notification.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    List<Notification> findByRecipientOrderByCreatedAtDesc(String recipient);
}

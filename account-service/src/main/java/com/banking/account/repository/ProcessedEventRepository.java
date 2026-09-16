package com.banking.account.repository;

import com.banking.account.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    boolean existsByEventId(String eventId);
}

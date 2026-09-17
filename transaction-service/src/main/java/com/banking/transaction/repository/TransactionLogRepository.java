package com.banking.transaction.repository;

import com.banking.transaction.entity.TransactionLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, UUID> {

    List<TransactionLog> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

    List<TransactionLog> findAllByOrderByCreatedAtAsc();
}

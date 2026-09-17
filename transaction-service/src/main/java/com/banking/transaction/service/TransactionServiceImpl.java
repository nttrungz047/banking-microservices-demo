package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionLogResponse;
import com.banking.transaction.entity.ProcessedEvent;
import com.banking.transaction.entity.TransactionLog;
import com.banking.transaction.entity.TransactionType;
import com.banking.transaction.event.AccountOperationCompletedEvent;
import com.banking.transaction.event.AccountOperationFailedEvent;
import com.banking.transaction.event.TransferResultEvent;
import com.banking.transaction.exception.TransactionNotFoundException;
import com.banking.transaction.mapper.TransactionMapper;
import com.banking.transaction.repository.ProcessedEventRepository;
import com.banking.transaction.repository.TransactionLogRepository;

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
public class TransactionServiceImpl implements TransactionService {

    private final TransactionLogRepository transactionLogRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TransactionLogResponse> getTransactions(UUID accountId) {
        List<TransactionLog> logs;
        if (accountId != null) {
            logs = transactionLogRepository.findByAccountIdOrderByCreatedAtAsc(accountId);
        } else {
            logs = transactionLogRepository.findAllByOrderByCreatedAtAsc();
        }
        return transactionMapper.toResponseList(logs);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionLogResponse getTransactionById(UUID id) {
        TransactionLog log = transactionLogRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + id));
        return transactionMapper.toResponse(log);
    }

    @Override
    @Transactional
    public void recordAccountOperationCompleted(
            AccountOperationCompletedEvent event,
            TransactionType type,
            String topic) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Skipping duplicate event {} for topic {}", event.eventId(), topic);
            return;
        }

        TransactionLog transactionLog = TransactionLog.builder()
                .eventId(event.eventId())
                .paymentId(event.paymentId())
                .accountId(event.accountId())
                .type(type)
                .amount(event.amount())
                .balanceAfter(event.balanceAfter())
                .description(type.name() + " completed for account " + event.accountId())
                .build();

        transactionLogRepository.save(transactionLog);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), topic, Instant.now()));
        log.info("Recorded transaction log {} for event {} on topic {}", transactionLog.getId(), event.eventId(), topic);
    }

    @Override
    @Transactional
    public void recordAccountOperationFailed(
            AccountOperationFailedEvent event,
            TransactionType type,
            String topic) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Skipping duplicate event {} for topic {}", event.eventId(), topic);
            return;
        }

        TransactionLog transactionLog = TransactionLog.builder()
                .eventId(event.eventId())
                .paymentId(event.paymentId())
                .accountId(event.accountId())
                .type(type)
                .amount(event.amount())
                .description(event.reason())
                .build();

        transactionLogRepository.save(transactionLog);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), topic, Instant.now()));
        log.info("Recorded failed transaction log {} for event {} on topic {}", transactionLog.getId(), event.eventId(), topic);
    }

    @Override
    @Transactional
    public void recordTransferResult(
            TransferResultEvent event,
            TransactionType type,
            String topic) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Skipping duplicate event {} for topic {}", event.eventId(), topic);
            return;
        }

        String fromDesc = type == TransactionType.TRANSFER_COMPLETED
                ? "Transfer to account " + event.toAccountId()
                : "Transfer failed: " + event.reason();

        String toDesc = type == TransactionType.TRANSFER_COMPLETED
                ? "Transfer from account " + event.fromAccountId()
                : "Transfer failed: " + event.reason();

        TransactionLog fromLog = TransactionLog.builder()
                .eventId(event.eventId())
                .paymentId(event.paymentId())
                .accountId(event.fromAccountId())
                .type(type)
                .amount(event.amount())
                .description(fromDesc)
                .build();

        TransactionLog toLog = TransactionLog.builder()
                .eventId(event.eventId())
                .paymentId(event.paymentId())
                .accountId(event.toAccountId())
                .type(type)
                .amount(event.amount())
                .description(toDesc)
                .build();

        transactionLogRepository.save(fromLog);
        transactionLogRepository.save(toLog);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), topic, Instant.now()));
        log.info("Recorded transfer result transaction logs for payment {} on topic {}", event.paymentId(), topic);
    }
}

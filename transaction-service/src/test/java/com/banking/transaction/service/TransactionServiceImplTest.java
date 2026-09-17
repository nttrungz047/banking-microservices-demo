package com.banking.transaction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    void getTransactions_withAccountId_returnsFilteredTransactions() {
        UUID accountId = UUID.randomUUID();
        List<TransactionLog> logs = List.of(
                TransactionLog.builder()
                        .id(UUID.randomUUID())
                        .accountId(accountId)
                        .type(TransactionType.DEBIT)
                        .amount(new BigDecimal("100.00"))
                        .build()
        );
        List<TransactionLogResponse> responses = List.of(
                new TransactionLogResponse(
                        logs.get(0).getId(),
                        "event-1",
                        null,
                        accountId,
                        TransactionType.DEBIT,
                        new BigDecimal("100.00"),
                        new BigDecimal("900.00"),
                        "DEBIT",
                        Instant.now()
                )
        );

        when(transactionLogRepository.findByAccountIdOrderByCreatedAtAsc(accountId)).thenReturn(logs);
        when(transactionMapper.toResponseList(logs)).thenReturn(responses);

        List<TransactionLogResponse> result = transactionService.getTransactions(accountId);

        assertEquals(1, result.size());
        assertEquals(accountId, result.get(0).accountId());
        verify(transactionLogRepository).findByAccountIdOrderByCreatedAtAsc(accountId);
    }

    @Test
    void getTransactions_withoutAccountId_returnsAllTransactions() {
        List<TransactionLog> logs = List.of(
                TransactionLog.builder()
                        .id(UUID.randomUUID())
                        .type(TransactionType.DEBIT)
                        .build()
        );
        List<TransactionLogResponse> responses = List.of(
                new TransactionLogResponse(
                        logs.get(0).getId(),
                        "event-1",
                        null,
                        null,
                        TransactionType.DEBIT,
                        new BigDecimal("100.00"),
                        null,
                        "DEBIT",
                        Instant.now()
                )
        );

        when(transactionLogRepository.findAllByOrderByCreatedAtAsc()).thenReturn(logs);
        when(transactionMapper.toResponseList(logs)).thenReturn(responses);

        List<TransactionLogResponse> result = transactionService.getTransactions(null);

        assertEquals(1, result.size());
        verify(transactionLogRepository).findAllByOrderByCreatedAtAsc();
    }

    @Test
    void getTransactionById_existingId_returnsTransaction() {
        UUID id = UUID.randomUUID();
        TransactionLog log = TransactionLog.builder().id(id).type(TransactionType.CREDIT).build();
        TransactionLogResponse response = new TransactionLogResponse(
                id, "event-2", null, UUID.randomUUID(), TransactionType.CREDIT,
                new BigDecimal("50.00"), new BigDecimal("1050.00"), "CREDIT", Instant.now()
        );

        when(transactionLogRepository.findById(id)).thenReturn(Optional.of(log));
        when(transactionMapper.toResponse(log)).thenReturn(response);

        TransactionLogResponse result = transactionService.getTransactionById(id);

        assertNotNull(result);
        assertEquals(id, result.id());
    }

    @Test
    void getTransactionById_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(transactionLogRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.getTransactionById(id));
    }

    @Test
    void recordAccountOperationCompleted_success() {
        String eventId = "evt-123";
        UUID paymentId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        AccountOperationCompletedEvent event = new AccountOperationCompletedEvent(
                eventId, paymentId, accountId, new BigDecimal("100.00"), new BigDecimal("900.00")
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        transactionService.recordAccountOperationCompleted(event, TransactionType.DEBIT, "account.debit-completed");

        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(eventId, savedLog.getEventId());
        assertEquals(paymentId, savedLog.getPaymentId());
        assertEquals(accountId, savedLog.getAccountId());
        assertEquals(TransactionType.DEBIT, savedLog.getType());
        assertEquals(new BigDecimal("100.00"), savedLog.getAmount());
        assertEquals(new BigDecimal("900.00"), savedLog.getBalanceAfter());

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void recordAccountOperationCompleted_duplicateEvent_isSkipped() {
        String eventId = "evt-123";
        AccountOperationCompletedEvent event = new AccountOperationCompletedEvent(
                eventId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), new BigDecimal("900.00")
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        transactionService.recordAccountOperationCompleted(event, TransactionType.DEBIT, "account.debit-completed");

        verify(transactionLogRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void recordAccountOperationFailed_success() {
        String eventId = "evt-456";
        UUID paymentId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        AccountOperationFailedEvent event = new AccountOperationFailedEvent(
                eventId, paymentId, accountId, new BigDecimal("500.00"), "Insufficient balance"
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        transactionService.recordAccountOperationFailed(event, TransactionType.DEBIT_FAILED, "account.debit-failed");

        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(eventId, savedLog.getEventId());
        assertEquals(TransactionType.DEBIT_FAILED, savedLog.getType());
        assertEquals("Insufficient balance", savedLog.getDescription());

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void recordTransferResult_completed_savesBothAccountsLogs() {
        String eventId = "evt-789";
        UUID paymentId = UUID.randomUUID();
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        TransferResultEvent event = new TransferResultEvent(
                eventId, paymentId, from, to, new BigDecimal("250.00"), null
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        transactionService.recordTransferResult(event, TransactionType.TRANSFER_COMPLETED, "payment.transfer-completed");

        verify(transactionLogRepository, times(2)).save(any(TransactionLog.class));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }
}

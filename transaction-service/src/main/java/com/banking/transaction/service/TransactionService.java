package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionLogResponse;
import com.banking.transaction.entity.TransactionType;
import com.banking.transaction.event.AccountOperationCompletedEvent;
import com.banking.transaction.event.AccountOperationFailedEvent;
import com.banking.transaction.event.TransferResultEvent;
import java.util.List;
import java.util.UUID;

public interface TransactionService {

    List<TransactionLogResponse> getTransactions(UUID accountId);

    TransactionLogResponse getTransactionById(UUID id);

    void recordAccountOperationCompleted(AccountOperationCompletedEvent event, TransactionType type, String topic);

    void recordAccountOperationFailed(AccountOperationFailedEvent event, TransactionType type, String topic);

    void recordTransferResult(TransferResultEvent event, TransactionType type, String topic);
}

package com.banking.transaction.mapper;

import com.banking.transaction.dto.TransactionLogResponse;
import com.banking.transaction.entity.TransactionLog;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionLogResponse toResponse(TransactionLog transactionLog);

    List<TransactionLogResponse> toResponseList(List<TransactionLog> transactionLogs);
}

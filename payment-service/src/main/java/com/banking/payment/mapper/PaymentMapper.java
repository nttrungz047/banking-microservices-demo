package com.banking.payment.mapper;

import com.banking.payment.dto.PaymentResponse;
import com.banking.payment.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentResponse toResponse(Payment payment);
}

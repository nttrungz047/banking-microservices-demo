package com.banking.payment.service;

import com.banking.payment.dto.*;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse transfer(TransferRequest request);

    PaymentResponse getPayment(UUID id);
}

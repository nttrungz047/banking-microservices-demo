package com.banking.payment.controller;

import com.banking.payment.dto.*;
import com.banking.payment.service.PaymentService;
import jakarta.validation.Valid;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PaymentResponse transfer(@Valid @RequestBody TransferRequest request) {
        return paymentService.transfer(request);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return paymentService.getPayment(id);
    }
}

package com.banking.payment.service;

import com.banking.payment.dto.*;
import com.banking.payment.entity.*;
import com.banking.payment.exception.PaymentNotFoundException;
import com.banking.payment.mapper.PaymentMapper;
import com.banking.payment.repository.PaymentRepository;

import java.util.UUID;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final KafkaPaymentProducer producer;

    @Transactional
    public PaymentResponse transfer(TransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId()))
            throw new IllegalArgumentException("Source and destination accounts must differ");
        Payment payment = paymentRepository.save(Payment.builder().fromAccountId(request.fromAccountId()).toAccountId(request.toAccountId()).amount(request.amount()).status(PaymentStatus.PENDING).build());
        producer.debit(payment.getId(), payment.getFromAccountId(), payment.getAmount());
        log.info("Started transfer saga {}", payment.getId());
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        return paymentMapper.toResponse(find(id));
    }

    @Transactional
    public Payment find(UUID id) {
        return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
    }

    @Transactional
    public void complete(UUID id) {
        Payment p = find(id);
        if (p.getStatus() == PaymentStatus.PENDING) {
            p.setStatus(PaymentStatus.COMPLETED);
            producer.completed(p.getId(), p.getFromAccountId(), p.getToAccountId(), p.getAmount());
            log.info("Completed transfer saga {}", id);
        }
    }

    @Transactional
    public void fail(UUID id, String reason) {
        Payment p = find(id);
        if (p.getStatus() == PaymentStatus.PENDING) {
            p.setStatus(PaymentStatus.FAILED);
            p.setFailureReason(reason);
            producer.failed(p.getId(), p.getFromAccountId(), p.getToAccountId(), p.getAmount(), reason);
            log.info("Failed transfer saga {}: {}", id, reason);
        }
    }

    @Transactional
    public void requestRefund(UUID id) {
        Payment p = find(id);
        if (p.getStatus() == PaymentStatus.PENDING) producer.refund(p.getId(), p.getFromAccountId(), p.getAmount());
    }
}

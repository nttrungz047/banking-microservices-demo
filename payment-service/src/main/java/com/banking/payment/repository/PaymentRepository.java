package com.banking.payment.repository;

import com.banking.payment.entity.Payment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}

package com.banking.payment.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.banking.payment.dto.*;
import com.banking.payment.entity.*;
import com.banking.payment.mapper.PaymentMapper;
import com.banking.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
 @Mock PaymentRepository payments; @Mock PaymentMapper mapper; @Mock KafkaPaymentProducer producer;
 @InjectMocks PaymentServiceImpl service;
 @Test void startsSagaByPublishingDebitRequest() {
  UUID paymentId=UUID.randomUUID(), from=UUID.randomUUID(), to=UUID.randomUUID();
  Payment payment=Payment.builder().id(paymentId).fromAccountId(from).toAccountId(to).amount(new BigDecimal("25.00")).status(PaymentStatus.PENDING).build();
  when(payments.save(any())).thenReturn(payment); when(mapper.toResponse(payment)).thenReturn(new PaymentResponse(paymentId,from,to,payment.getAmount(),PaymentStatus.PENDING,null,null));
  service.transfer(new TransferRequest(from,to,new BigDecimal("25.00")));
  verify(producer).debit(paymentId,from,new BigDecimal("25.00"));
 }
 @Test void rejectsSameSourceAndDestination() { UUID account=UUID.randomUUID(); assertThrows(IllegalArgumentException.class,()->service.transfer(new TransferRequest(account,account,BigDecimal.ONE))); verifyNoInteractions(payments,producer); }
}

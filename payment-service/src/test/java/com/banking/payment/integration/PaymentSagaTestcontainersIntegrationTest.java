package com.banking.payment.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.banking.payment.PaymentServiceApplication;
import com.banking.payment.dto.PaymentResponse;
import com.banking.payment.dto.TransferRequest;
import com.banking.payment.entity.Payment;
import com.banking.payment.entity.PaymentStatus;
import com.banking.payment.event.AccountOperationCompletedEvent;
import com.banking.payment.repository.PaymentRepository;
import com.banking.payment.repository.ProcessedEventRepository;
import com.banking.payment.service.KafkaPaymentConsumer;
import com.banking.payment.service.KafkaPaymentProducer;
import com.banking.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = PaymentServiceApplication.class)
@ActiveProfiles("test")
@Testcontainers
class PaymentSagaTestcontainersIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payment_test_db")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private KafkaPaymentConsumer kafkaPaymentConsumer;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private KafkaPaymentProducer kafkaPaymentProducer;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    void whenTransferInitiatedAndEventsProcessed_sagaCompletesSuccessfullyInPostgres() throws Exception {
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("250.00");

        TransferRequest request = new TransferRequest(fromAccountId, toAccountId, amount);
        PaymentResponse response = paymentService.transfer(request);

        assertNotNull(response.id());
        assertEquals(PaymentStatus.PENDING, response.status());

        // Verify initial persistence in PostgreSQL container
        Payment initialPayment = paymentRepository.findById(response.id()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, initialPayment.getStatus());
        assertEquals(amount, initialPayment.getAmount());

        // Step 1: Simulate downstream Debited event
        String debitEventId = "tc-evt-debit-" + UUID.randomUUID();
        AccountOperationCompletedEvent debitedEvent = new AccountOperationCompletedEvent(
                debitEventId,
                response.id(),
                fromAccountId,
                amount,
                new BigDecimal("750.00")
        );
        kafkaPaymentConsumer.debited(objectMapper.writeValueAsString(debitedEvent));

        // Step 2: Simulate downstream Credited event
        String creditEventId = "tc-evt-credit-" + UUID.randomUUID();
        AccountOperationCompletedEvent creditedEvent = new AccountOperationCompletedEvent(
                creditEventId,
                response.id(),
                toAccountId,
                amount,
                new BigDecimal("450.00")
        );
        kafkaPaymentConsumer.credited(objectMapper.writeValueAsString(creditedEvent));

        // Verify final state in real Postgres
        Payment completedPayment = paymentRepository.findById(response.id()).orElseThrow();
        assertEquals(PaymentStatus.COMPLETED, completedPayment.getStatus());

        // Verify deduplication table persistence
        assertTrue(processedEventRepository.existsById(debitEventId));
        assertTrue(processedEventRepository.existsById(creditEventId));

        // Step 3: Replay duplicate debit event -> idempotency check
        kafkaPaymentConsumer.debited(objectMapper.writeValueAsString(debitedEvent));
        assertEquals(PaymentStatus.COMPLETED, paymentRepository.findById(response.id()).orElseThrow().getStatus());
    }
}

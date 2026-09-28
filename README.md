# 🏦 Banking Microservices Platform

[![Java 25](https://img.shields.io/badge/Java-25-orange.svg?style=flat&logo=openjdk)](https://openjdk.org/)
[![Spring Boot 4.1.x](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud 2025.1.x](https://img.shields.io/badge/Spring%20Cloud-2025.1.2-blue.svg?style=flat&logo=spring)](https://spring.io/projects/spring-cloud)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-KRaft-black.svg?style=flat&logo=apachekafka)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![Resilience4j](https://img.shields.io/badge/Resilience4j-Circuit%20Breaker-red.svg?style=flat)](https://resilience4j.readme.io/)
[![Zipkin Distributed Tracing](https://img.shields.io/badge/Distributed%20Tracing-Zipkin%20%2B%20Brave-yellow.svg?style=flat)](https://zipkin.io/)
[![CI](https://img.shields.io/badge/CI-GitHub%20Actions-blue.svg?style=flat&logo=githubactions)](.github/workflows/ci.yml)

An enterprise-grade, distributed **Banking Microservices Platform** built with **Java 25**, **Spring Boot 4.1.x**, and **Spring Cloud 2025.1.x**. This project demonstrates end-to-end event-driven architecture, choreographic & orchestrated Saga distributed transaction patterns, optimistic concurrency locking, distributed tracing, circuit breaker fault-tolerance, Kafka idempotency, and comprehensive centralized exception handling.

---

## 🏛️ System Architecture

```mermaid
graph TD
    Client["Client / Postman / Frontend"] -->|HTTP REST / JWT| Gateway["API Gateway (:8080)<br>• JWT Auth Filter<br>• Resilience4j Circuit Breakers<br>• Centralized Error Handler"]

    subgraph Infrastructure ["Infrastructure Layer"]
        Eureka["Eureka Discovery Server (:8761)"]
        Config["Spring Cloud Config Server (:8888)"]
        Zipkin["Zipkin Tracing Server (:9411)"]
        KafkaUI["Kafka UI (:8089)"]
        MailHog["MailHog SMTP (:8025)"]
    end

    subgraph Core Services ["Business Microservices Layer"]
        Auth["Auth Service (:8081)<br>• User Management<br>• JWT Issue & Refresh"]
        Account["Account Service (:8082)<br>• Balance Management<br>• Optimistic Locking (@Version)"]
        Payment["Payment Service (:8083)<br>• Saga Orchestrator<br>• Transfer State Machine"]
        Transaction["Transaction Service (:8084)<br>• Append-only Ledger<br>• Audit History"]
        Notification["Notification Service (:8085)<br>• Customer Alerts<br>• Email Dispatcher"]
    end

    subgraph Data & Messaging ["Storage & Event Bus"]
        KafkaBus[("Apache Kafka Event Bus (:9092, :9094)<br>• Distributed Topics<br>• Observation Tracing<br>• Dead-Letter-Topics (.DLT)")]
        Postgres[("PostgreSQL Instance (:5432)<br>• auth_db<br>• account_db<br>• payment_db<br>• transaction_db<br>• notification_db")]
    end

    Gateway --> Auth
    Gateway --> Account
    Gateway --> Payment
    Gateway --> Transaction
    Gateway --> Notification

    Auth --> Eureka
    Account --> Eureka
    Payment --> Eureka
    Transaction --> Eureka
    Notification --> Eureka
    Gateway --> Eureka

    Account <--> KafkaBus
    Payment <--> KafkaBus
    Transaction <--> KafkaBus
    Notification <--> KafkaBus

    Auth --> Postgres
    Account --> Postgres
    Payment --> Postgres
    Transaction --> Postgres
    Notification --> Postgres
```

---

## 🔄 Distributed Saga Transaction Pattern (Money Transfer Flow)

The money transfer flow is implemented via an **Event-Driven Saga Pattern** with automatic compensating actions (refunds) upon failure, avoiding slow 2PC distributed locking:

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Gateway as API Gateway
    participant Payment as Payment Service
    participant Kafka as Kafka Topics
    participant Account as Account Service
    participant Tx as Transaction Service
    participant Notif as Notification Service

    Client->>Gateway: POST /api/payments/transfer
    Gateway->>Payment: Forward request with X-User-Id
    Payment->>Payment: Save Payment (status: PENDING)
    Payment->>Kafka: Publish `account.debit-requested`
    
    par Async Processing
        Account->>Kafka: Consume `account.debit-requested`
        Account->>Account: Debit fromAccount (Optimistic Lock)
        Account->>Kafka: Publish `account.debit-completed`
        Tx->>Kafka: Consume `account.debit-completed` -> Log DEBIT
    end

    Payment->>Kafka: Consume `account.debit-completed`
    Payment->>Kafka: Publish `account.credit-requested`

    alt Credit Succeeded
        Account->>Kafka: Consume `account.credit-requested`
        Account->>Account: Credit toAccount (Optimistic Lock)
        Account->>Kafka: Publish `account.credit-completed`
        Payment->>Kafka: Consume `account.credit-completed`
        Payment->>Payment: Update Payment (status: COMPLETED)
        Payment->>Kafka: Publish `payment.transfer-completed`
        Tx->>Kafka: Log TRANSFER_COMPLETED
        Notif->>Kafka: Consume `payment.transfer-completed` -> Send Email Alert
    else Credit Failed (Compensating Transaction)
        Account->>Kafka: Publish `account.credit-failed`
        Payment->>Kafka: Consume `account.credit-failed`
        Payment->>Kafka: Publish `account.refund-requested`
        Account->>Kafka: Debit refund to fromAccount -> Publish `account.refund-completed`
        Payment->>Payment: Update Payment (status: FAILED)
        Payment->>Kafka: Publish `payment.transfer-failed`
        Notif->>Kafka: Send Failure Email Alert
    end
```

---

## 🛠️ Microservices & Tech Stack Overview

| Service | Port | Database | Primary Responsibility |
|---|---|---|---|
| **`eureka-server`** | `8761` | — | Dynamic Service Registry & Discovery |
| **`config-server`** | `8888` | — | Centralized Configuration Management (Native repository) |
| **`api-gateway`** | `8080` | — | Reactive Routing, JWT Authentication Filter, Resilience4j Circuit Breaker |
| **`auth-service`** | `8081` | PostgreSQL (`auth_db`) | User Registration, BCrypt password hashing, JWT Access/Refresh tokens |
| **`account-service`** | `8082` | PostgreSQL (`account_db`) | Bank accounts, `@Version` optimistic locking debit/credit operations |
| **`payment-service`** | `8083` | PostgreSQL (`payment_db`) | Saga Orchestration, payment lifecycle, compensating transactions |
| **`transaction-service`** | `8084` | PostgreSQL (`transaction_db`) | Immutable append-only audit ledger for transactions |
| **`notification-service`** | `8085` | PostgreSQL (`notification_db`) | Idempotent email notifications via MailHog / JavaMail |
| **`zipkin`** | `9411` | In-memory / Storage | Micrometer Brave distributed trace UI & span visualizer |

---

## 🛡️ Cross-Cutting Concerns & Production-Ready Patterns

### 1. Centralized Exception Handling
- Unified JSON error schema across all services:
  ```json
  {
    "timestamp": "2026-09-28T07:15:00.123Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "fields": { "amount": "must be greater than 0" }
  }
  ```
- Handled at both **API Gateway** (`GatewayErrorWebExceptionHandler` on WebFlux) and downstream services (`@RestControllerAdvice`).

### 2. Distributed Tracing (Micrometer Tracing + Zipkin)
- Request traces (`traceId`, `spanId`) are automatically propagated across synchronous HTTP calls and asynchronous Kafka topics via `spring.kafka.template.observation-enabled: true`.
- View distributed span visualizer at [http://localhost:9411](http://localhost:9411).

### 3. Fault Tolerance & Resilience (Resilience4j + Kafka DLT)
- **API Gateway Circuit Breaker**: Routes are wrapped in Resilience4j reactive circuit breakers. In case of downstream service degradation, requests fallback to `/fallback/{service}` with `503 Service Unavailable`.
- **Kafka Consumer Retry & Dead Letter Topics**: `DefaultErrorHandler` configured with `FixedBackOff(1000L, 3)` and `DeadLetterPublishingRecoverer` to automatically publish unrecoverable messages to `<topic>.DLT`.

### 4. Consumer Idempotency
- All Kafka consumers check the `ProcessedEvent` repository (`existsById(eventId)`) to avoid double debits/credits or duplicate emails if messages are redelivered.

---

## 🚀 Quickstart & Local Execution

### Prerequisites
- **Java 25 JDK** (`JAVA_HOME` pointing to JDK 25)
- **Docker & Docker Compose**

### Step 1: Start Infrastructure Containers
```bash
docker compose up -d
```
*Spins up PostgreSQL (with 5 databases), Kafka KRaft, Kafka UI, MailHog, and Zipkin.*

### Step 2: Start Microservices (in separate terminals or IDE)
```bash
# 1. Config & Registry
./gradlew :config-server:bootRun
./gradlew :eureka-server:bootRun

# 2. Business Services
./gradlew :auth-service:bootRun
./gradlew :account-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :transaction-service:bootRun
./gradlew :notification-service:bootRun

# 3. Gateway
./gradlew :api-gateway:bootRun
```

---

## 🧪 Testing & Postman Collection

### Automated Tests
```bash
# Run all unit and integration tests across all microservices
./gradlew test
```

### Postman Collection
Import the pre-configured Postman Collection:
📁 [`docs/postman_collection.json`](docs/postman_collection.json)

The collection includes automated tests that capture and propagate `accessToken`, `userId`, `senderAccountId`, `receiverAccountId`, and `paymentId` seamlessly across test steps.

### OpenAPI / Swagger UI Endpoints
- **Auth Service**: `http://localhost:8081/swagger-ui.html`
- **Account Service**: `http://localhost:8082/swagger-ui.html`
- **Payment Service**: `http://localhost:8083/swagger-ui.html`
- **Transaction Service**: `http://localhost:8084/swagger-ui.html`
- **Notification Service**: `http://localhost:8085/swagger-ui.html`

---

## 💡 Key Architectural Decisions & Interview Q&A

<details>
<summary><b>1. Why Saga Choreography/Orchestration instead of Two-Phase Commit (2PC)?</b></summary>
2PC requires distributed locks across heterogeneous databases and participants. In high-volume banking systems, this creates latency bottlenecks and single points of failure. The Saga pattern splits transactions into local acid transactions coordinated asynchronously via Kafka events with compensating transactions for rollbacks.
</details>

<details>
<summary><b>2. How is concurrency handled during simultaneous balance updates?</b></summary>
Account Service utilizes <b>Optimistic Concurrency Control</b> with JPA <code>@Version</code>. When concurrent transfers attempt to modify the same balance, Hibernate checks the version column, throwing an <code>OptimisticLockException</code> which is safely caught and retried/failed by the Saga orchestrator.
</details>

<details>
<summary><b>3. How do we guarantee Kafka Consumer Idempotency?</b></summary>
Each Kafka message carries a unique <code>eventId</code> (UUID). Consumers first query the <code>processed_events</code> table. If the event ID exists, the message is ignored. If not, the transaction is processed and the event ID is stored atomically.
</details>

---

## 📂 Project Structure

```
banking-microservices-demo/
├── .github/workflows/ci.yml       # GitHub Actions CI pipeline
├── docs/postman_collection.json   # Ready-to-import Postman collection
├── docker-compose.yml             # Postgres, Kafka, Kafka UI, MailHog, Zipkin
├── config-server/                 # Centralized cloud configurations
├── eureka-server/                 # Service discovery registry
├── api-gateway/                   # WebFlux gateway + JWT + Circuit Breaker
├── auth-service/                  # User auth + JWT management
├── account-service/               # Account balance + Optimistic locking
├── payment-service/               # Saga orchestrator + Testcontainers
├── transaction-service/           # Immutable transaction history
└── notification-service/          # Email notifications + Deduplication
```

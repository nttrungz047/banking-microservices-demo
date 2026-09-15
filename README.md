# Banking Microservices

Demo hệ thống ngân hàng theo kiến trúc microservices: quản lý account, xử lý transfer tiền qua Saga pattern, event-driven bằng Kafka.

## Architecture

```
                          ┌─────────────┐
                          │ API Gateway │  ← JWT validation, routing
                          └──────┬──────┘
                                 │
         ┌───────────┬──────────┼──────────┬─────────────┐
         ▼           ▼          ▼          ▼             ▼
     ┌───────┐  ┌─────────┐ ┌────────┐ ┌─────────┐ ┌──────────────┐
     │ Auth  │  │ Account │ │Payment │ │Transaction│ │Notification │
     └───────┘  └────┬────┘ └───┬────┘ └────┬─────┘ └──────┬───────┘
                      │          │           │              │
                      └──────────┴─────Kafka─┴──────────────┘

   Eureka (service discovery) + Config Server (centralized config)
   chạy song song, mọi service đăng ký vào Eureka.
```

**Money transfer flow** dùng **Choreography Saga** (event-driven, không dùng distributed transaction 2PC):

```
Payment: publish DebitRequested
  → Account: debit A → publish Debited (hoặc DebitFailed)
Payment: consume Debited → publish CreditRequested
  → Account: credit B → publish Credited (hoặc CreditFailed)
    → nếu fail: publish RefundRequested (compensating transaction, hoàn tiền A)
Transaction: consume mọi event → ghi log
Notification: consume TransferCompleted → gửi email
```

## Services

| Service | Port | Responsibility | DB |
|---|---|---|---|
| eureka-server | 8761 | Service discovery | - |
| config-server | 8888 | Centralized config (native) | - |
| api-gateway | 8080 | Routing, JWT filter | - |
| dummy-service | 8090 | Phase 0 checkpoint ping | - |
| auth-service | 8081 | Register/login, JWT issue | PostgreSQL (`auth_db`) |
| account-service | 8082 | Account CRUD, balance | PostgreSQL (`account_db`) |
| payment-service | 8083 | Saga orchestrator cho transfer | PostgreSQL (`payment_db`) |
| transaction-service | 8084 | Transaction history (append-only log) | PostgreSQL (`transaction_db`) |
| notification-service | 8085 | Email/SMS notification | - |

## Tech Stack

- Java 25, Spring Boot 4.1.x, Gradle 9.x
- Spring Cloud 2025.1.x (Gateway, Eureka, Config Server)
- Spring Security + JWT (from Phase 1)
- Kafka KRaft (event bus)
- PostgreSQL (Database per Service — 1 container, 4 databases)
- Resilience4j (Circuit Breaker, Retry) — Phase 6
- Docker Compose

## Run locally

Requires **JDK 25** (`JAVA_HOME` pointing to JDK 25).

```bash
# Infra: Postgres (4 DBs), Kafka KRaft, Kafka UI
docker compose up -d

# Start order (separate terminals)
./gradlew :config-server:bootRun
./gradlew :eureka-server:bootRun
./gradlew :dummy-service:bootRun
./gradlew :api-gateway:bootRun
```

### Phase 0 checkpoint

- Eureka dashboard: http://localhost:8761 — should list `DUMMY-SERVICE` (and `API-GATEWAY`)
- Via Gateway: `GET http://localhost:8080/api/dummy/ping` → `{"status":"UP","service":"dummy-service"}`
- Kafka UI: http://localhost:8089
- Kafka bootstrap (host apps): `localhost:9094`

## Project structure

```
banking-microservices-demo/
├── docker-compose.yml
├── docker/postgres/init-databases.sql
├── eureka-server/
├── config-server/
├── api-gateway/
├── dummy-service/          # Phase 0 checkpoint only
├── auth-service/           # Phase 1+
├── account-service/
├── payment-service/
├── transaction-service/
└── notification-service/
```

## Status

**Phase 0 complete.** Tiếp theo theo `banking-microservices-plan.md`. Conventions: `AGENTS.md`.

# Banking Microservices

Demo hệ thống ngân hàng theo kiến trúc microservices: quản lý account, xử lý transfer tiền qua Saga pattern, event-driven bằng Kafka.

## Architecture

```
                          ┌─────────────┐
                          │ API Gateway │  ← JWT validation, routing
                          └──────┬──────┘
                                 │
         ┌───────────┬───────────┼───────────┬─────────────┐
         ▼           ▼           ▼           ▼             ▼
     ┌───────┐  ┌─────────┐ ┌────────┐ ┌─────────┐ ┌──────────────┐
     │ Auth  │  │ Account │ │Payment │ │Transaction│ │Notification │
     └───────┘  └────┬────┘ └────┬───┘ └────┬────┘ └──────┬───────┘
                     │           │          │             │
                     └───────────┴────Kafka─┴─────────────┘

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
./gradlew :auth-service:bootRun
./gradlew :account-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :transaction-service:bootRun
./gradlew :dummy-service:bootRun
./gradlew :api-gateway:bootRun
```

### Phase 0 checkpoint

- Eureka dashboard: http://localhost:8761 — should list `DUMMY-SERVICE` (and `API-GATEWAY`)
- Via Gateway: `GET http://localhost:8080/api/dummy/ping` → `{"status":"UP","service":"dummy-service"}`
- Kafka UI: http://localhost:8089
- Kafka bootstrap (host apps): `localhost:9094`

### Phase 1 checkpoint

```bash
# Register
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"alice\",\"email\":\"alice@example.com\",\"password\":\"password123\"}"

# Login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"alice\",\"password\":\"password123\"}"

# Without token → 401
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/dummy/ping

# With access token → 200
curl -s http://localhost:8080/api/dummy/ping \
  -H "Authorization: Bearer <accessToken>"
```

- Eureka should list `AUTH-SERVICE`
- Gateway validates JWT and forwards `X-User-Id` to downstream services

### Phase 2 checkpoint

```bash
# Create account
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"userId":"<user-id>","balance":"1000.00","currency":"USD"}'

# Get by account id
curl -s http://localhost:8080/api/accounts/<account-id> \
  -H "Authorization: Bearer <access-token>"
```

### Phase 3 checkpoint

```bash
# Initiate transfer saga
curl -s -X POST http://localhost:8080/api/payments/transfer \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":"<account-A-id>","toAccountId":"<account-B-id>","amount":"250.00"}'

# Check payment status
curl -s http://localhost:8080/api/payments/<payment-id> \
  -H "Authorization: Bearer <access-token>"
```

### Phase 4 checkpoint

```bash
# Query transaction history for an account
curl -s "http://localhost:8080/api/transactions?accountId=<account-id>" \
  -H "Authorization: Bearer <access-token>"

# Query single transaction log
curl -s "http://localhost:8080/api/transactions/<transaction-id>" \
  -H "Authorization: Bearer <access-token>"
```

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
├── account-service/        # Phase 2+
├── payment-service/        # Phase 3+
├── transaction-service/    # Phase 4+
└── notification-service/
```

## Status

**Phase 4 complete.** Transaction service đã triển khai append-only `TransactionLog`, Kafka consumer lắng nghe toàn bộ các event (`Debited`, `Credited`, `DebitFailed`, `CreditFailed`, `Refunded`, `TransferCompleted`, `TransferFailed`), và query API `GET /api/transactions?accountId=`. Tiếp theo theo `banking-microservices-plan.md` cho Phase 5. Conventions: `AGENTS.md`.
